package ee.fakeplastictrees.morningcoffee.brief;

import ee.fakeplastictrees.morningcoffee.common.Config;
import java.io.Closeable;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ScheduledBriefGenerator implements Closeable {
  private static final Logger logger = LogManager.getLogger();
  private static final int RETRY_LIMIT = 10;

  private final BriefService service;
  private final ZoneId timeZone;
  private final int scheduledHour;
  private final int scheduledMinute;
  private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

  public ScheduledBriefGenerator(Config.Brief.Schedule config, BriefService service) {
    this.service = service;
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

  private synchronized void scheduleNext(Task t) {
    if (executor.isShutdown()) {
      return;
    }

    var timeUntil = timeUntilScheduledHour();
    var idempotencyKey = String.valueOf(Instant.now().toEpochMilli());
    var retryCount = 0;

    var isRetry = t.retry().isPresent();
    if (isRetry) {
      var retry = t.retry().get();
      if (retry.count() <= RETRY_LIMIT) {
        timeUntil = Duration.ofSeconds(10);
        idempotencyKey = retry.idempotencyKey();
        retryCount = retry.count();
      } else {
        // TODO: schedule the next task you bloody idiot
        logger.info("reached RETRY_LIMIT");
        return;
      }
    }

    logger.info("scheduling the next brief to be generated in {}", timeUntil);
    executor.schedule(
        () -> {
          try {
            logger.debug("generating a new brief");
            // TODO: populate with real data
            service.generate("idempotencyKey", "sourceMaterial");
            scheduleNext(new Task(Optional.empty()));
          } catch (Throwable cause) {
            logger.error("brief generator exception", cause);

            var retry = new Retry(idempotencyKey, retryCount + 1);
            var task = new Task(Optional.of(retry));
            scheduleNext(task);
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

  private record Task(Optional<Retry> retry) {}

  private record Retry(String idempotencyKey, int count) {}
}
