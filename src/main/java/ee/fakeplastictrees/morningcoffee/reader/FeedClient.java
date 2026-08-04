package ee.fakeplastictrees.morningcoffee.reader;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

class FeedClient {
  private static final Logger logger = LogManager.getLogger();

  private static final int MAX_RESPONSE_BODY_BYTES = 5 * 1024 * 1024;
  private static final String USER_AGENT =
      "MorningCoffee/1.0 (+https://github.com/parfentjev/morning-coffee)";
  private static final Duration HTTP_CLIENT_TIMEOUT = Duration.ofSeconds(10);
  private static final Duration REQUEST_THROTTLING_DURATION = Duration.ofSeconds(5);

  private final ConcurrentHashMap<String, ThrottlingManager<HttpResponse<byte[]>>>
      throttlingManagers;
  private final HttpClient httpClient;

  public FeedClient() {
    this.httpClient =
        HttpClient.newBuilder()
            .connectTimeout(HTTP_CLIENT_TIMEOUT)
            .followRedirects(Redirect.NORMAL)
            .build();

    this.throttlingManagers = new ConcurrentHashMap<>();
  }

  /// Requests the given `url` and returns the raw response body if the server responds with 200 OK.
  ///
  /// @param url address of an RSS/Atom feed
  /// @throws FeedClientException if the URL is malformed, the request fails, or the server returns
  /// an unexpected status code
  /// @throws InterruptedException if the thread is interrupted
  public byte[] fetchFeed(String url) throws FeedClientException, InterruptedException {
    try {
      var uri = new URI(url);
      logger.debug("fetching feed: {}", uri);

      var response =
          throttlingManager(uri).execute(() -> httpClient.send(request(uri), bodyHandler()));
      if (response.statusCode() != HttpURLConnection.HTTP_OK) {
        var statusCode = response.statusCode();
        var message = "%s returned unexpected status code: %d".formatted(uri, statusCode);
        throw new FeedClientException(message);
      }

      return response.body();
    } catch (URISyntaxException e) {
      var message = "failed to parse feed url: %s".formatted(url);
      throw new FeedClientException(message, e);
    } catch (IOException e) {
      var message = "failed to execute http request: %s".formatted(url);
      throw new FeedClientException(message, e);
    }
  }

  private HttpRequest request(URI uri) {
    return HttpRequest.newBuilder()
        .header("User-Agent", USER_AGENT)
        .uri(uri)
        .timeout(HTTP_CLIENT_TIMEOUT)
        .GET()
        .build();
  }

  private BodyHandler<byte[]> bodyHandler() {
    return HttpResponse.BodyHandlers.limiting(
        HttpResponse.BodyHandlers.ofByteArray(), MAX_RESPONSE_BODY_BYTES);
  }

  private ThrottlingManager<HttpResponse<byte[]>> throttlingManager(URI uri) {
    return throttlingManagers.computeIfAbsent(
        uri.getHost().toLowerCase(Locale.ROOT),
        _ -> new ThrottlingManager<>(REQUEST_THROTTLING_DURATION));
  }
}
