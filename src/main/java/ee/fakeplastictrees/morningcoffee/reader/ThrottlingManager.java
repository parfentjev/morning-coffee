package ee.fakeplastictrees.morningcoffee.reader;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.Semaphore;

class ThrottlingManager<T> {
  private final Duration delay;
  private final Semaphore semaphore;
  private long nextRequestNanoTime;

  /// Creates a new instance of ThrottlingManager.
  ///
  /// @param delay delay between requests executed by the manager
  ThrottlingManager(Duration delay) {
    this.delay = delay;
    this.semaphore = new Semaphore(1);
    this.nextRequestNanoTime = System.nanoTime();
  }

  /// Executes a request after the configured delay has elapsed since the previous request
  /// completed. This reduces the likelihood of server-side rate limiting.
  ///
  /// @throws InterruptedException if the thread is interrupted
  /// @throws IOException if an I/O error occurs
  T execute(ThrottableRequest<T> request) throws InterruptedException, IOException {
    semaphore.acquire();

    // Always release the semaphore, but reset the deadline only after a request attempt.
    try {
      awaitDelay();
      try {
        return request.run();
      } finally {
        nextRequestNanoTime = System.nanoTime() + delay.toNanos();
      }
    } finally {
      semaphore.release();
    }
  }

  /// Blocks the current thread until the next request is allowed.
  ///
  /// @throws InterruptedException if the thread is interrupted
  private void awaitDelay() throws InterruptedException {
    var remainingTime = nextRequestNanoTime - System.nanoTime();
    if (remainingTime > 0) {
      Thread.sleep(Duration.ofNanos(remainingTime));
    }
  }
}
