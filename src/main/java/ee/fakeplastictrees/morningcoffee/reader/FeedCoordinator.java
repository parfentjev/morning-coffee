package ee.fakeplastictrees.morningcoffee.reader;

import ee.fakeplastictrees.morningcoffee.model.Feed;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

final class FeedCoordinator {
  private static final Logger logger = LogManager.getLogger();

  private enum Lifecycle {
    NEW,
    RUNNING,
    STOPPING
  }

  private enum Phase {
    RESERVED,
    HTTP_FINISHED,
    FINISHED,
    REJECTED
  }

  // All scheduling state and helper state are guarded by this single monitor.
  private final Object lock = new Object();

  private Lifecycle lifecycle = Lifecycle.NEW;
  private boolean refreshing;
  private ScheduledFuture<?> pollingTask;
  private final DeadlineWakeup wakeup = new DeadlineWakeup();
  private final Map<String, HostState> hosts = new HashMap<>();
  private final LinkedHashMap<UUID, ProcessableFeed> pendingFeeds = new LinkedHashMap<>();
  private final Map<UUID, Admission> activeTasks = new HashMap<>();
  private final long pollIntervalSeconds;
  private final long delayNanos;
  private final int capacity;
  private final ScheduledExecutorService scheduledExecutor;
  private final ExecutorService fetchFeedExecutor;
  private final Supplier<Optional<List<ProcessableFeed>>> loadSnapshot;
  private final Consumer<Admission> process;
  private final LongSupplier clock;

  // Values must be validated. Both executors must submit asynchronously, never inline.
  FeedCoordinator(
      long pollIntervalSeconds,
      long delayNanos,
      int capacity,
      ScheduledExecutorService timers,
      ExecutorService workers,
      Supplier<Optional<List<ProcessableFeed>>> loadSnapshot,
      Consumer<Admission> process,
      LongSupplier clock) {
    this.pollIntervalSeconds = pollIntervalSeconds;
    this.delayNanos = delayNanos;
    this.capacity = capacity;
    this.scheduledExecutor = timers;
    this.fetchFeedExecutor = workers;
    this.loadSnapshot = loadSnapshot;
    this.process = process;
    this.clock = clock;
  }

  void start() {
    synchronized (lock) {
      if (lifecycle != Lifecycle.NEW) {
        return;
      }

      schedulePollLocked(0);

      if (pollingTask != null) {
        lifecycle = Lifecycle.RUNNING;
      }
    }
  }

  private void schedulePollLocked(long seconds) {
    if (lifecycle == Lifecycle.STOPPING) {
      return;
    }

    try {
      pollingTask = scheduledExecutor.schedule(this::fetchFeeds, seconds, TimeUnit.SECONDS);
    } catch (RejectedExecutionException e) {
      logger.warn("failed to schedule repository poll", e);
    }
  }

  private void fetchFeeds() {
    synchronized (lock) {
      pollingTask = null;
      if (lifecycle == Lifecycle.STOPPING || refreshing) {
        return;
      }

      refreshing = true;

      try {
        fetchFeedExecutor.execute(
            () -> {
              var snapshot = Optional.<List<ProcessableFeed>>empty();
              try {
                snapshot = loadSnapshot.get();
              } catch (RuntimeException e) {
                logger.error("unhandled reader runtime exception", e);
              } finally {
                synchronized (lock) {
                  try {
                    refreshFinishedLocked(snapshot);
                  } finally {
                    refreshing = false;
                    schedulePollLocked(pollIntervalSeconds);
                  }
                }
              }
            });
      } catch (RejectedExecutionException e) {
        refreshing = false;
        logger.warn("repository refresh submission rejected", e);
        schedulePollLocked(pollIntervalSeconds);
      }
    }
  }

  private void refreshFinishedLocked(Optional<List<ProcessableFeed>> result) {
    if (lifecycle != Lifecycle.RUNNING) {
      return;
    }

    if (result.isPresent()) {
      var snapshot = new LinkedHashMap<UUID, ProcessableFeed>();
      result.get().forEach(feed -> snapshot.put(feed.entity().id(), feed));
      pendingFeeds.keySet().retainAll(snapshot.keySet());
      snapshot.forEach(
          (id, feed) -> {
            if (!activeTasks.containsKey(id)) {
              pendingFeeds.put(id, feed);
            }
          });
    }

    dispatchLocked();
  }

  // Caller must hold lock throughout admission and submission.
  private void dispatchLocked() {
    if (lifecycle != Lifecycle.RUNNING) {
      return;
    }

    var now = clock.getAsLong();
    var iterator = pendingFeeds.entrySet().iterator();
    while (iterator.hasNext() && activeTasks.size() < capacity) {
      var entry = iterator.next();
      var id = entry.getKey();
      var task = new Admission(entry.getValue());
      var host = hosts.computeIfAbsent(task.processableFeed().hostname(), _ -> new HostState());

      if (!host.reserve(task, now)) {
        continue;
      }

      activeTasks.put(id, task);

      try {
        fetchFeedExecutor.execute(() -> runTask(task));
      } catch (RejectedExecutionException e) {
        releaseLocked(task, Phase.REJECTED);
        logger.warn("feed task submission rejected: {} {}", id, entry.getValue().uri(), e);

        break;
      }
      iterator.remove();
    }
    reconsiderThrottleLocked(now);
  }

  private void reconsiderThrottleLocked(long now) {
    wakeup.cancel();

    var pendingHosts = new HashSet<String>();
    pendingFeeds.values().forEach(feed -> pendingHosts.add(feed.hostname()));

    hosts
        .entrySet()
        .removeIf(
            entry ->
                !pendingHosts.contains(entry.getKey())
                    && !entry.getValue().reserved()
                    && entry.getValue().remaining(now) == 0);

    if (lifecycle != Lifecycle.RUNNING || activeTasks.size() >= capacity) {
      return;
    }

    Long delay = null;
    for (var hostname : pendingHosts) {
      var host = hosts.get(hostname);
      if (host != null && !host.reserved() && host.remaining(now) > 0) {
        var remaining = host.remaining(now);
        delay = delay == null ? remaining : Math.min(delay, remaining);
      }
    }

    if (delay == null) {
      return;
    }

    wakeup.replace(now + delay);
  }

  private void runTask(Admission task) {
    try {
      process.accept(task);
    } catch (RuntimeException e) {
      logger.error(
          "unhandled process feed exception: {} {}", task.feed.entity().id(), task.feed.uri(), e);
    } finally {
      task.close();
    }
  }

  private boolean releaseLocked(Admission task, Phase phase) {
    var id = task.feed.entity().id();
    if (activeTasks.get(id) != task) {
      return false;
    }

    var host = hosts.get(task.feed.hostname());
    if (host != null && host.owner == task) {
      host.owner = null;
    }

    activeTasks.remove(id);
    task.phase = phase;

    return true;
  }

  final class Admission implements AutoCloseable {
    private final ProcessableFeed feed;
    private Phase phase = Phase.RESERVED;

    private Admission(ProcessableFeed feed) {
      this.feed = feed;
    }

    ProcessableFeed processableFeed() {
      return feed;
    }

    void httpAttemptFinished(long completedAtNanos) {
      synchronized (lock) {
        if (phase != Phase.RESERVED || activeTasks.get(feed.entity().id()) != this) {
          return;
        }

        var host = hosts.get(feed.hostname());
        if (host != null && host.owner == this) {
          host.deadline = completedAtNanos + delayNanos;
          host.owner = null;
        }

        phase = Phase.HTTP_FINISHED;
        dispatchLocked();
      }
    }

    @Override
    public void close() {
      synchronized (lock) {
        if (releaseLocked(this, Phase.FINISHED)) {
          dispatchLocked();
        }
      }
    }
  }

  private final class HostState {
    private Admission owner;
    private Long deadline;

    boolean reserved() {
      return owner != null;
    }

    long remaining(long now) {
      return deadline == null ? 0 : Math.max(0, deadline - now);
    }

    boolean reserve(Admission task, long now) {
      if (reserved() || remaining(now) > 0) {
        return false;
      }

      owner = task;
      return true;
    }
  }

  // Called only under lock, including callback validation and dispatch.
  private final class DeadlineWakeup {
    private ScheduledFuture<?> future;
    private long generation;

    void cancel() {
      synchronized (lock) {
        ++generation;
        if (future != null) {
          future.cancel(false);
          future = null;
        }
      }
    }

    void replace(long deadline) {
      cancel();
      var expected = generation;
      try {
        future =
            scheduledExecutor.schedule(
                () -> {
                  synchronized (lock) {
                    if (lifecycle != Lifecycle.RUNNING || expected != generation) {
                      return;
                    }

                    future = null;
                    dispatchLocked();
                  }
                },
                Math.max(0, deadline - clock.getAsLong()),
                TimeUnit.NANOSECONDS);
      } catch (RejectedExecutionException e) {
        logger.warn("failed to schedule throttle wake-up", e);
      }
    }
  }

  boolean beginStop() {
    synchronized (lock) {
      if (lifecycle == Lifecycle.STOPPING) {
        return false;
      }

      lifecycle = Lifecycle.STOPPING;
      if (pollingTask != null) {
        pollingTask.cancel(false);
        pollingTask = null;
      }

      wakeup.cancel();
      pendingFeeds.clear();

      return true;
    }
  }

  void workersTerminated() {
    synchronized (lock) {
      activeTasks.clear();
      hosts.clear();
    }
  }

  record ProcessableFeed(Feed entity, URI uri, String hostname) {
    ProcessableFeed(Feed entity) throws URISyntaxException {
      var uri = new URI(entity.url());
      var hostname = uri.getHost().toLowerCase(Locale.ROOT);
      this(entity, uri, hostname);
    }
  }
}
