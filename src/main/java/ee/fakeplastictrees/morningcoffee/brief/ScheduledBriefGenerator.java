package ee.fakeplastictrees.morningcoffee.brief;

import ee.fakeplastictrees.morningcoffee.common.Config;
import ee.fakeplastictrees.morningcoffee.feed.FeedService;
import java.io.Closeable;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ScheduledBriefGenerator implements Closeable {
  private static final Logger logger = LogManager.getLogger();
  private static final int RETRY_LIMIT = 3;

  private final BriefService briefService;
  private final FeedService feedService;
  private final ZoneId timeZone;
  private final int scheduledHour;
  private final int scheduledMinute;
  private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

  public ScheduledBriefGenerator(
      Config.Brief.Schedule config, BriefService briefService, FeedService feedService) {
    this.briefService = briefService;
    this.feedService = feedService;

    this.timeZone = config.timeZone();
    this.scheduledHour = config.hour();
    this.scheduledMinute = config.minute();
  }

  public void start() {
    scheduleNext(0);
  }

  @Override
  public synchronized void close() {
    executor.shutdownNow();
  }

  private synchronized void scheduleNext(final int retryCount) {
    if (executor.isShutdown()) {
      return;
    }

    var timeUntil = timeUntilScheduledHour();
    if (retryCount > 0) {
      if (retryCount <= RETRY_LIMIT) {
        timeUntil = Duration.ofMinutes(1);
      } else {
        logger.info("reached RETRY_LIMIT");
        scheduleNext(0);
        return;
      }
    }

    logger.info("scheduling the next brief to be generated in {}", timeUntil);
    executor.schedule(
        () -> {
          try {
            logger.debug("generating a new brief");

            // TODO: map entries to text, call the generate method
            feedService.getEntries(0);

            scheduleNext(0);
          } catch (Throwable cause) {
            logger.error("brief generator exception", cause);
            scheduleNext(retryCount + 1);
          }
        },
        timeUntil.toNanos(),
        TimeUnit.NANOSECONDS);
  }

  private Duration timeUntilScheduledHour() {
    var now = ZonedDateTime.now(timeZone);
    var scheduledTime = now.toLocalDate().atTime(scheduledHour, scheduledMinute).atZone(timeZone);
    if (now.isAfter(scheduledTime)) {
      scheduledTime = scheduledTime.plusDays(1);
    }

    return Duration.between(now, scheduledTime);
  }
}
