package ee.fakeplastictrees.morningcoffee.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import inet.ipaddr.IPAddressString;
import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

// Integration tests for the HTTP boundary; scheduling rules live in FeedCoordinatorTest.
class FeedClientTest {
  @Test
  void successfulRequestReturnsBodyAndReportsOneCompletedAttempt() throws Exception {
    var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/",
        exchange -> {
          try (exchange) {
            exchange.sendResponseHeaders(200, 1);
            exchange.getResponseBody().write(42);
          }
        });
    server.start();
    var completedAttempts = new AtomicInteger();
    try (var client = new FeedClient(List.of())) {
      var uri = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/");

      var response =
          client.fetchFeed(
              uri, Duration.ofSeconds(2), _ -> completedAttempts.incrementAndGet());

      assertThat(response.body()).containsExactly((byte) 42);
      assertThat(completedAttempts.get()).isEqualTo(1);
    } finally {
      server.stop(0);
    }
  }

  @Test
  void errorResponseStillCountsAsOneCompletedAttempt() throws Exception {
    var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/",
        exchange -> {
          try (exchange) {
            exchange.sendResponseHeaders(503, -1);
          }
        });
    server.start();
    var completedAttempts = new AtomicInteger();
    try (var client = new FeedClient(List.of())) {
      var uri = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/");

      assertThatThrownBy(
              () -> client.fetchFeed(
                  uri, Duration.ofSeconds(2), _ -> completedAttempts.incrementAndGet()))
          .isInstanceOf(FeedClientStatusCodeException.class);
      assertThat(completedAttempts.get()).isEqualTo(1);
    } finally {
      server.stop(0);
    }
  }

  @Test
  void blockedNetworkDoesNotCountAsAnHttpAttempt() {
    var completedAttempts = new AtomicInteger();
    try (var client =
        new FeedClient(List.of(new IPAddressString("127.0.0.0/8").getAddress()))) {
      assertThatThrownBy(
              () ->
                  client.fetchFeed(
                      URI.create("http://127.0.0.1/"),
                      Duration.ofSeconds(2),
                      _ -> completedAttempts.incrementAndGet()))
          .isInstanceOf(FeedClientException.class);
      assertThat(completedAttempts.get()).isZero();
    }
  }
}
