package ee.fakeplastictrees.morningcoffee.feed;

import ee.fakeplastictrees.morningcoffee.common.Config;
import ee.fakeplastictrees.morningcoffee.common.RepositoryException;
import java.io.Closeable;
import java.net.URISyntaxException;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/// Polls configured feeds and persists new entries on a fixed schedule.
public final class ScheduledFeedReader implements Closeable {
  private static final Logger logger = LogManager.getLogger();

  private final Config.Reader config;
  private final ScheduledThreadPoolExecutor scheduledExecutor;
  private final ExecutorService fetchFeedExecutor;
  private final FeedService feedService;
  private final FeedClient feedClient;
  private final FeedParser feedParser;

  private final FeedCoordinator coordinator;

  /// Creates a scheduled feed reader.
  ///
  /// @param config feed reader configuration
  /// @param feedService service providing feed entries
  public ScheduledFeedReader(Config.Reader config, FeedService feedService)
      throws IllegalArgumentException {
    this(config, feedService, null);
  }

  /// Creates a scheduled reader with a configurable executor for feed fetches.
  ///
  /// @param config feed polling and retrieval settings
  /// @param feedService service providing feed entries
  /// @param workers executor that runs submissions asynchronously, or null to use virtual threads
  /// @throws IllegalArgumentException if the reader configuration is invalid
  ScheduledFeedReader(Config.Reader config, FeedService feedService, ExecutorService workers) {
    assertConfigValid(config);

    this.config = config;
    this.feedService = feedService;

    this.scheduledExecutor = new ScheduledThreadPoolExecutor(1);
    this.scheduledExecutor.setRemoveOnCancelPolicy(true);
    this.scheduledExecutor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
    this.scheduledExecutor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);

    this.fetchFeedExecutor =
        workers == null ? Executors.newVirtualThreadPerTaskExecutor() : workers;

    this.feedClient = new FeedClient(config.blockedNetworks());
    this.feedParser = new FeedParser();

    this.coordinator =
        new FeedCoordinator(
            config.pollIntervalSeconds(),
            Duration.ofSeconds(config.requestThrottlingDelaySeconds()).toNanos(),
            config.maxParallelFetches(),
            scheduledExecutor,
            fetchFeedExecutor,
            this::loadSnapshot,
            this::processFeed,
            System::nanoTime);
  }

  /// Starts scheduled feed polling.
  public void start() {
    coordinator.start();
  }

  private void assertConfigValid(Config.Reader config) throws IllegalArgumentException {
    if (config.pollIntervalSeconds() < 1) {
      throw new IllegalArgumentException("polling interval must be >= 1");
    }

    try {
      Duration.ofSeconds(config.pollIntervalSeconds()).toNanos();
    } catch (ArithmeticException e) {
      throw new IllegalArgumentException("poll interval overflows long", e);
    }

    if (config.requestThrottlingDelaySeconds() < 0) {
      throw new IllegalArgumentException("throttling delay must be >= 0");
    }

    try {
      Duration.ofSeconds(config.requestThrottlingDelaySeconds()).toNanos();
    } catch (ArithmeticException e) {
      throw new IllegalArgumentException("throttling delay overflows long", e);
    }

    // wouldn't it be nice?
    // https://openjdk.org/jeps/8303099
    if (config.blockedNetworks() == null) {
      throw new IllegalArgumentException("blocked networks list cannot be null");
    }

    if (config.maxParallelFetches() < 1) {
      throw new IllegalArgumentException("max parallel fetches must be >= 1");
    }

    if (config.maxEntriesPerFetch() < 1) {
      throw new IllegalArgumentException("max entries per fetch must be >= 1");
    }
  }

  private Optional<List<FeedCoordinator.ProcessableFeed>> loadSnapshot() {
    List<Feed> feeds;
    try {
      feeds = feedService.getFeeds();
    } catch (RepositoryException e) {
      logger.warn("failed to get feeds", e);
      return Optional.empty();
    }

    var snapshot = new LinkedHashMap<UUID, FeedCoordinator.ProcessableFeed>();
    for (var feed : feeds) {
      try {
        snapshot.put(feed.id(), new FeedCoordinator.ProcessableFeed(feed));
      } catch (URISyntaxException e) {
        logger.error("failed to parse feed url: {}", feed.url());
      }
    }

    return Optional.of(List.copyOf(snapshot.values()));
  }

  private void processFeed(FeedCoordinator.Admission task) {
    var feed = task.processableFeed();
    // fetch
    HttpResponse<byte[]> response;
    try {
      response =
          feedClient.fetchFeed(
              feed.uri(), feed.entity().requestTimeout(), task::httpAttemptFinished);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();

      return;
    } catch (FeedClientStatusCodeException e) {
      logger.debug("fetch feed unexpected status code: {} {}", feed.uri(), e.statusCode());

      return;
    } catch (FeedClientException e) {
      logger.warn("fetch feed request failed: {}", feed.uri(), e);

      return;
    }

    // process
    try {
      var entries =
          feedParser.parseResponse(feed.entity().id(), response.body()).stream()
              .sorted(this::sortByPublishedAtDesc)
              .toList();

      if (config.maxEntriesPerFetch() >= entries.size()) {
        feedService.saveFeedEntries(entries);
      } else {
        logger.debug(
            "{} returned {} entries, saving only the latest {}",
            feed.uri(),
            entries.size(),
            config.maxEntriesPerFetch());

        feedService.saveFeedEntries(entries.subList(0, config.maxEntriesPerFetch()));
      }
    } catch (FeedParserException e) {
      if (logger.isDebugEnabled()) {
        var responseData = extractResponseData(response);
        logger.debug("response data for {}: {}", feed.uri(), responseData);
      }

      logger.warn("failed to parse feed: {}", feed.uri(), e);
    } catch (RepositoryException e) {
      logger.warn("failed to save feed entries: {}", feed.uri(), e);
    }
  }

  private int sortByPublishedAtDesc(FeedEntry a, FeedEntry b) {
    return b.publishedAt().compareTo(a.publishedAt());
  }

  private Map<String, Object> extractResponseData(HttpResponse<byte[]> response) {
    var body = response.body();
    var truncatedBodyLength = Math.min(body.length, 512);

    return Map.of(
        "Status",
        response.statusCode(),
        "URI",
        response.uri(),
        "Content-Type",
        response.headers().firstValue("Content-Type").orElse("absent"),
        "Content-Length",
        response.headers().firstValue("Content-Length").orElse("absent"),
        "Server",
        response.headers().firstValue("Server").orElse("absent"),
        "Body-Length",
        body.length,
        "Body-Truncated",
        new String(body, 0, truncatedBodyLength, StandardCharsets.UTF_8));
  }

  @Override
  public void close() {
    if (!coordinator.beginStop()) {
      return;
    }

    try {
      logger.info("shutting down");
      scheduledExecutor.shutdownNow();
      fetchFeedExecutor.shutdownNow();

      if (scheduledExecutor.awaitTermination(5, TimeUnit.SECONDS) == false) {
        logger.warn("failed to stop scheduledExecutor in time");
      }

      if (fetchFeedExecutor.awaitTermination(5, TimeUnit.SECONDS) == false) {
        logger.warn("failed to stop fetchFeedExecutor in time");
      }
    } catch (InterruptedException e) {
      logger.warn("interrupted while awaiting termination", e);
      Thread.currentThread().interrupt();
    } finally {
      if (fetchFeedExecutor.isTerminated()) {
        coordinator.workersTerminated();
      }

      feedClient.close();
    }
  }
}
