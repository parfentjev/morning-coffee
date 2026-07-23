package ee.fakeplastictrees.morning_coffee;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

class Reader {
  private static final Integer REFRESH_DELAY = 10;
  private static final TimeUnit REFRESH_UNIT = TimeUnit.SECONDS;

  private final ScheduledExecutorService executor;
  private final Repository repository;

  public Reader(ScheduledExecutorService executor, Repository repository) {
    this.executor = executor;
    this.repository = repository;
  }

  public void run() {
    executor.scheduleWithFixedDelay(this::fetchFeeds, 0, REFRESH_DELAY, REFRESH_UNIT);
  }

  private void fetchFeeds() {
    repository.getFeeds()
        .forEach(feed -> {
          var title = String.format("%s: %d", feed.url(), ThreadLocalRandom.current().nextInt(0, 100));
          var url = String.format("#%d", ThreadLocalRandom.current().nextInt(0, 100));
          var entry = new FeedEntry(title, url);
          repository.saveEntry(entry);
        });
  }
}
