package ee.fakeplastictrees.morningcoffee.reader;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

public class ThrottlingManagerTest {
  @Test
  public void shouldDelayRequestExecution() throws Exception {
    var expectedDelay = Duration.ofSeconds(2);
    var manager = new ThrottlingManager<Long>(expectedDelay);

    Duration time1;
    Duration time2;
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var request1 = executor.submit(() -> manager.execute(System::nanoTime));
      var request2 = executor.submit(() -> manager.execute(System::nanoTime));

      time1 = Duration.ofNanos(request1.get());
      time2 = Duration.ofNanos(request2.get());
    }

    var actualDelay = time1.minus(time2).abs();
    assertThat(actualDelay).isGreaterThanOrEqualTo(expectedDelay);
  }

  @Test
  public void managersShouldBeIndependent() throws Exception {
    var delay = Duration.ofSeconds(2);
    var manager1 = new ThrottlingManager<Long>(delay);
    var manager2 = new ThrottlingManager<Long>(delay);

    Duration time1;
    Duration time2;
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var request1 = executor.submit(() -> manager1.execute(System::nanoTime));
      var request2 = executor.submit(() -> manager2.execute(System::nanoTime));

      time1 = Duration.ofNanos(request1.get());
      time2 = Duration.ofNanos(request2.get());
    }

    var actualDelay = time1.minus(time2).abs();
    assertThat(actualDelay).isLessThan(delay);
  }
}
