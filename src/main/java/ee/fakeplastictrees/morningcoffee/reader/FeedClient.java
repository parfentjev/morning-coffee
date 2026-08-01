package ee.fakeplastictrees.morningcoffee.reader;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

class FeedClient {
  private static final Logger logger = LogManager.getLogger();

  private static final int MAX_RESPONSE_BODY_BYTES = 5 * 1024 * 1024;
  private static final Duration HTTP_CLIENT_TIMEOUT = Duration.ofSeconds(10);

  private final HttpClient httpClient;

  public FeedClient() {
    this.httpClient =
        HttpClient.newBuilder()
            .connectTimeout(HTTP_CLIENT_TIMEOUT)
            .followRedirects(Redirect.NORMAL)
            .build();
  }

  public byte[] fetchFeed(String url) throws FeedClientException, InterruptedException {
    try {
      var uri = new URI(url);
      var request = HttpRequest.newBuilder().uri(uri).timeout(HTTP_CLIENT_TIMEOUT).GET().build();

      logger.debug("fetching feed: {}", uri);
      var bodyHandler =
          HttpResponse.BodyHandlers.limiting(
              HttpResponse.BodyHandlers.ofByteArray(), MAX_RESPONSE_BODY_BYTES);
      var response = httpClient.send(request, bodyHandler);
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
}
