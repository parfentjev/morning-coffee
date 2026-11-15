package ee.fakeplastictrees.morningcoffee.reader;

import static org.assertj.core.api.Assertions.assertThat;

import ee.fakeplastictrees.morningcoffee.model.Feed;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Delayed;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class FeedCoordinatorTest {
  @Test
  void sameHostWaitsUntilCooldownExpires() {
    try (var f = new Fixture(10, 5)) {
      f.add("a.example");
      f.add("a.example");
      f.processing = task -> task.httpAttemptFinished(f.now);
      f.start();
      assertThat(f.workers.tasks).hasSize(1);

      f.workers.runNext();
      f.wakeAt(9);
      assertThat(f.workers.tasks).isEmpty();

      f.wakeAt(10);
      assertThat(f.workers.tasks).hasSize(1);
    }
  }

  @Test
  void coolingHostDoesNotBlockAnotherHost() {
    try (var f = new Fixture(10, 1)) {
      f.add("a.example");
      f.add("a.example");
      f.add("b.example");
      var processedHosts = new ArrayList<String>();
      f.processing = task -> {
        processedHosts.add(task.processableFeed().hostname());
        task.httpAttemptFinished(f.now);
      };
      f.start();

      f.workers.runNext();
      f.workers.runNext();

      assertThat(processedHosts).containsExactly("a.example", "b.example");
    }
  }

  @Test
  void zeroCooldownStillAllowsOnlyOneConcurrentRequestPerHost() {
    try (var f = new Fixture(0, 5)) {
      f.add("a.example");
      f.add("a.example");
      f.processing = task -> task.httpAttemptFinished(f.now);
      f.start();
      assertThat(f.workers.tasks).hasSize(1);

      f.workers.runNext();

      assertThat(f.workers.tasks).hasSize(1);
    }
  }

  @Test
  void failureBeforeHttpAttemptDoesNotStartCooldown() {
    try (var f = new Fixture(10, 5)) {
      f.add("a.example");
      f.add("a.example");
      f.processing = _ -> {
        throw new IllegalStateException("failed before HTTP attempt");
      };
      f.start();

      f.workers.runNext();

      assertThat(f.workers.tasks).hasSize(1);
    }
  }

  @Test
  void processingKeepsCapacityUntilTheTaskCompletes() {
    try (var f = new Fixture(0, 1)) {
      f.add("a.example");
      f.add("b.example");
      f.processing = task -> {
        task.httpAttemptFinished(f.now);
        assertThat(f.workers.tasks).isEmpty();
      };
      f.start();
      assertThat(f.workers.tasks).hasSize(1);

      f.workers.runNext();

      assertThat(f.workers.tasks).hasSize(1);
      f.workers.runNext();
      assertThat(f.workers.tasks).isEmpty();
    }
  }

  @Test
  void failedRefreshDoesNotStopLaterPolling() {
    try (var f = new Fixture(10, 5)) {
      f.failRefresh = true;
      f.start();
      assertThat(f.workers.tasks).isEmpty();

      f.failRefresh = false;
      f.add("a.example");
      var processedHosts = new ArrayList<String>();
      f.processing = task -> processedHosts.add(task.processableFeed().hostname());
      f.poll();
      f.workers.runNext();

      assertThat(processedHosts).containsExactly("a.example");
    }
  }

  static final class Fixture implements AutoCloseable {
    final CapturingExecutor workers = new CapturingExecutor();
    final CapturingTimers timers = new CapturingTimers();
    final List<FeedCoordinator.ProcessableFeed> feeds = new ArrayList<>();
    Consumer<FeedCoordinator.Admission> processing = task -> {};
    boolean failRefresh;
    long now;
    final FeedCoordinator coordinator;

    Fixture(long delay, int capacity) {
      coordinator =
          new FeedCoordinator(
              60,
              delay,
              capacity,
              timers,
              workers,
              () -> {
                if (failRefresh) {
                  throw new IllegalStateException("controlled refresh failure");
                }
                return Optional.of(List.copyOf(feeds));
              },
              task -> processing.accept(task),
              () -> now);
    }

    void add(String host) {
      var uri = URI.create("https://" + host + "/" + feeds.size());
      feeds.add(
          new FeedCoordinator.ProcessableFeed(
              new Feed(UUID.randomUUID(), uri.toString(), Duration.ofSeconds(1)), uri, host));
    }

    void poll() {
      timers.fireNext();
      workers.runNext();
    }

    void wakeAt(long time) {
      now = time;
      timers.actions.getLast().run();
    }

    void start() {
      coordinator.start();
      poll();
    }

    @Override
    public void close() {
      coordinator.beginStop();
      workers.shutdownNow();
      timers.shutdownNow();
      coordinator.workersTerminated();
    }
  }

  static final class CapturingExecutor extends AbstractExecutorService {
    final List<Runnable> tasks = new ArrayList<>();
    private boolean stopped;

    void runNext() {
      tasks.removeFirst().run();
    }

    @Override
    public void execute(Runnable task) {
      if (stopped) {
        throw new RejectedExecutionException("controlled rejection");
      }
      tasks.add(task);
    }

    @Override
    public void shutdown() {
      stopped = true;
    }

    @Override
    public List<Runnable> shutdownNow() {
      shutdown();
      return List.copyOf(tasks);
    }

    @Override
    public boolean isShutdown() {
      return stopped;
    }

    @Override
    public boolean isTerminated() {
      return stopped;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) {
      return stopped;
    }
  }

  static final class CapturingTimers extends ScheduledThreadPoolExecutor {
    final List<Timer> actions = new ArrayList<>();

    CapturingTimers() {
      super(1);
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable action, long delay, TimeUnit unit) {
      var timer = new Timer(action, unit.toNanos(delay));
      actions.add(timer);
      return timer;
    }

    void fireNext() {
      actions.removeFirst().run();
    }
  }

  static final class Timer extends FutureTask<Void> implements ScheduledFuture<Void> {
    private final long delay;

    Timer(Runnable action, long delay) {
      super(action, null);
      this.delay = delay;
    }

    @Override
    public long getDelay(TimeUnit unit) {
      return unit.convert(delay, TimeUnit.NANOSECONDS);
    }

    @Override
    public int compareTo(Delayed other) {
      return Long.compare(delay, other.getDelay(TimeUnit.NANOSECONDS));
    }
  }
}
