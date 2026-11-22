package ee.fakeplastictrees.morningcoffee.brief;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ScheduledBriefGenerator {
  private static final Logger logger = LogManager.getLogger();

  // TODO: store in a config
  private static final ZoneId TIME_ZONE = ZoneId.of("Europe/Tallinn");
  // TODO: store in a config
  private static final int SCHEDULED_HOUR = 20;
  // TODO: store in a config
  private static final int SCHEDULED_MINUTE = 29;
  private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

  public ScheduledBriefGenerator() {
    scheduleNext();
  }

  private void scheduleNext() {
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
    var now = ZonedDateTime.now(TIME_ZONE);
    var scheduledTime =
        now.toLocalDate().atTime(SCHEDULED_HOUR, SCHEDULED_MINUTE).atZone(TIME_ZONE);
    if (now.isAfter(scheduledTime)) {
      scheduledTime = scheduledTime.plusDays(1);
    }

    return Duration.between(now, scheduledTime);
  }
}
