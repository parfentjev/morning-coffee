package ee.fakeplastictrees.morningcoffee.reader;

import ee.fakeplastictrees.morningcoffee.Config;
import ee.fakeplastictrees.morningcoffee.repository.Repository;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ScheduledFeedReader {
  private final Logger logger = LogManager.getLogger();

  private final Config.Reader config;
  private final ScheduledExecutorService scheduledExecutor;
  private final ExecutorService fetchFeedExecutor;
  private final Repository repository;

  private final FeedClient feedClient = new FeedClient();
  private final FeedParser feedParser = new FeedParser();

  public ScheduledFeedReader(Config.Reader config, Repository repository) {
    this.config = config;
    this.repository = repository;
    this.scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
    this.fetchFeedExecutor = Executors.newVirtualThreadPerTaskExecutor();
  }

  public void start() {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> stop()));

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

  private void stop() {
    logger.info("shutting down");
    scheduledExecutor.shutdownNow();
    fetchFeedExecutor.shutdownNow();
  }

  private void fetchFeeds() throws InterruptedException {
    var tasks = new ArrayList<Callable<Void>>();
    for (var feed : repository.getFeeds()) {
      tasks.add(
          () -> {
            processFeed(feed.url());

            return null;
          });
    }

    var futures = fetchFeedExecutor.invokeAll(tasks);
    for (var future : futures) {
      try {
        future.get();
      } catch (ExecutionException e) {
        logger.error("unhandled fetch feed execution exception", e);
      }
    }
  }

  private void processFeed(String url) throws InterruptedException {
    try {
      var response = feedClient.fetchFeed(url);
      var entries = feedParser.parseResponse(response);

      entries.forEach(repository::saveFeedEntry);
    } catch (FeedClientException e) {
      logger.warn("failed to fetch feed", e);
    } catch (FeedParserException e) {
      logger.warn("failed to parse feed", e);
    }
  }
}
