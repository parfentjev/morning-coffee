package ee.fakeplastictrees.morningcoffee.reader;

import ee.fakeplastictrees.morningcoffee.Config;
import ee.fakeplastictrees.morningcoffee.model.Feed;
import ee.fakeplastictrees.morningcoffee.model.FeedEntry;
import ee.fakeplastictrees.morningcoffee.repository.Repository;
import ee.fakeplastictrees.morningcoffee.repository.RepositoryException;
import java.io.Closeable;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/// Polls configured feeds and persists new entries on a fixed schedule.
public class ScheduledFeedReader implements Closeable {
  private static final Logger logger = LogManager.getLogger();

  private final Config.Reader config;
  private final ScheduledExecutorService scheduledExecutor;
  private final ExecutorService fetchFeedExecutor;
  private final Repository repository;
  private final FeedClient feedClient;
  private final FeedParser feedParser;

  /// Creates a scheduled feed reader.
  ///
  /// @param config feed reader configuration
  /// @param repository feed repository
  public ScheduledFeedReader(Config.Reader config, Repository repository) {
    this.config = config;
    this.repository = repository;

    this.scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
    this.fetchFeedExecutor = Executors.newFixedThreadPool(config.maxParallelFetches());

    this.feedClient =
        new FeedClient(config.requestThrottlingDelaySeconds(), config.blockedNetworks());
    this.feedParser = new FeedParser();
  }

  /// Starts scheduled feed polling.
  public void start() {
    var interval = config.pollIntervalSeconds();
    var timeUnit = TimeUnit.SECONDS;
    scheduledExecutor.scheduleWithFixedDelay(
        () -> {
          try {
            fetchFeeds();
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          } catch (RuntimeException e) {
            logger.error("unhandled reader runtime exception", e);
          }
        },
        0,
        interval,
        timeUnit);

    logger.info("scheduled feed reader to run every {} {}", interval, timeUnit.name());
  }

  private void fetchFeeds() throws InterruptedException {
    var tasks = new ArrayList<Callable<Void>>();
    try {
      for (var feed : repository.getFeeds()) {
        tasks.add(
            () -> {
              processFeed(feed);

              return null;
            });
      }
    } catch (RepositoryException e) {
      logger.warn("failed to get feeds", e);
      return;
    }

    for (var future : fetchFeedExecutor.invokeAll(tasks)) {
      try {
        future.get();
      } catch (ExecutionException e) {
        logger.error("unhandled fetch feed execution exception", e);
      }
    }
  }

  private void processFeed(Feed feed) throws InterruptedException {
    // fetch
    HttpResponse<byte[]> response;
    try {
      response = feedClient.fetchFeed(feed.url(), feed.requestTimeout());
    } catch (FeedClientStatusCodeException e) {
      logger.debug("fetch feed unexpected status code: {} {}", feed.url(), e.statusCode());
      return;
    } catch (FeedClientException e) {
      logger.warn("fetch feed request failed: {}", feed.url(), e);
      return;
    }

    // process
    try {
      var entries =
          feedParser.parseResponse(feed.id(), response.body()).stream()
              .sorted(this::sortByPublishedAtDesc)
              .toList();

      if (config.maxEntriesPerFetch() >= entries.size()) {
        repository.saveFeedEntries(entries);
      } else {
        logger.debug(
            "{} returned {} entries, saving only the latest {}",
            feed.url(),
            entries.size(),
            config.maxEntriesPerFetch());

        repository.saveFeedEntries(entries.subList(0, config.maxEntriesPerFetch()));
      }
    } catch (FeedParserException e) {
      if (logger.isDebugEnabled()) {
        var responseData = extractResponseData(response);
        logger.debug("response data for {}: {}", feed.url(), responseData);
      }

      logger.warn("failed to parse feed: {}", feed.url(), e);
    } catch (RepositoryException e) {
      logger.warn("failed to save feed entries: {}", feed.url(), e);
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

      feedClient.close();
    } catch (InterruptedException e) {
      logger.warn("interrupted while awaiting termination", e);
      Thread.currentThread().interrupt();
    }
  }
}
