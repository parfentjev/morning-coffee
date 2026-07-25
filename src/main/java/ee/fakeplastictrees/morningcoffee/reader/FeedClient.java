package ee.fakeplastictrees.morningcoffee.reader;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

class FeedClient {
  private static final int HTTP_OK = 200;

  private final Logger logger = LogManager.getLogger();
  private final HttpClient httpClient = HttpClient.newHttpClient();

  public byte[] fetchFeed(String url) throws FeedClientException, InterruptedException {
    try {
      var uri = new URI(url);
      var request = HttpRequest.newBuilder().uri(uri).GET().build();

      logger.debug("fetching feed: {}", uri);
      // todo: investigate response handler, should it be limited to `n` bytes?
      // otherwise, the server might return the odyssey for all I know
      var response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
      if (response.statusCode() != HTTP_OK) {
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
