package ee.fakeplastictrees.morningcoffee.brief;

import ee.fakeplastictrees.morningcoffee.Config;
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

  private final ZoneId timeZone;
  private final int scheduledHour;
  private final int scheduledMinute;
  private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

  public ScheduledBriefGenerator(Config.Brief.Schedule config) {
    this.timeZone = config.timeZone();
    this.scheduledHour = config.hour();
    this.scheduledMinute = config.minute();
  }

  public void start() {
    scheduleNext();
  }

  @Override
  public synchronized void close() {
    executor.shutdownNow();
  }

  private synchronized void scheduleNext() {
    if (executor.isShutdown()) {
      return;
    }

    var timeUntil = timeUntilScheduledHour();
    logger.info("scheduling the next brief to be generated in {}", timeUntil);

    executor.schedule(
        () -> {
          try {
            logger.debug("generating a new brief");
          } catch (Throwable t) {
            logger.error("brief generator error", t);
          } finally {
            scheduleNext();
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
