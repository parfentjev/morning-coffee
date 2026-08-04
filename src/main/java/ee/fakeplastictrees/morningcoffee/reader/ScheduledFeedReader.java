package ee.fakeplastictrees.morningcoffee.reader;

import ee.fakeplastictrees.morningcoffee.Config;
import ee.fakeplastictrees.morningcoffee.model.Feed;
import ee.fakeplastictrees.morningcoffee.repository.Repository;
import java.io.Closeable;
import java.util.ArrayList;
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
    this.fetchFeedExecutor = Executors.newVirtualThreadPerTaskExecutor();

    this.feedClient = new FeedClient(config.requestThrottlingDelaySeconds());
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
    for (var feed : repository.getFeeds()) {
      tasks.add(
          () -> {
            processFeed(feed);

            return null;
          });
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
    try {
      var response = feedClient.fetchFeed(feed.url());
      var entries =
          feedParser.parseResponse(response).stream()
              .peek(entry -> entry.setFeedId(feed.id()))
              .toList();
      repository.saveFeedEntries(entries);
    } catch (FeedClientException e) {
      logger.warn("failed to fetch feed: {}", feed.url(), e);
    } catch (FeedParserException e) {
      logger.warn("failed to parse feed: {}", feed.url(), e);
    }
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
    } catch (InterruptedException e) {
      logger.warn("interrupted while awaiting termination", e);
      Thread.currentThread().interrupt();
    }
  }
}
