package ee.fakeplastictrees.morningcoffee.reader;

import inet.ipaddr.HostName;
import inet.ipaddr.HostNameException;
import inet.ipaddr.IPAddress;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

class FeedClient {
  private static final Logger logger = LogManager.getLogger();

  private static final int MAX_RESPONSE_BODY_BYTES = 5 * 1024 * 1024;
  private static final Duration HTTP_CLIENT_TIMEOUT = Duration.ofSeconds(10);

  private final HttpClient httpClient;
  private final ConcurrentHashMap<String, ThrottlingManager<HttpResponse<byte[]>>>
      throttlingManagers;
  private final Duration throttlingDelay;
  private final List<IPAddress> blockedNetworks;

  /// Creates a new instance of FeedClient.
  ///
  /// @param throttlingDelay delay in seconds between requests to the same host
  public FeedClient(long throttlingDelay, List<IPAddress> blockedNetworks) {
    this.httpClient = HttpClient.newBuilder().connectTimeout(HTTP_CLIENT_TIMEOUT).build();
    this.throttlingManagers = new ConcurrentHashMap<>();
    this.throttlingDelay = Duration.ofSeconds(throttlingDelay);
    this.blockedNetworks = blockedNetworks;
  }

  /// Requests the given `url` and returns the raw response body if the server responds with 200 OK.
  ///
  /// @param url address of an RSS/Atom feed
  /// @throws FeedClientException if the URL is malformed, the request fails, or the server returns
  ///   an unexpected status code
  /// @throws InterruptedException if the thread is interrupted
  public HttpResponse<byte[]> fetchFeed(String url)
      throws FeedClientException, InterruptedException {
    try {
      var uri = new URI(url);
      logger.debug("fetching feed: {}", uri);

      var request = request(uri);
      var response = throttlingManager(uri).execute(() -> httpClient.send(request, bodyHandler()));
      if (response.statusCode() != HttpURLConnection.HTTP_OK) {
        var statusCode = response.statusCode();
        var message = "%s returned unexpected status code: %d".formatted(uri, statusCode);
        throw new FeedClientException(message);
      }

      return response;
    } catch (URISyntaxException e) {
      var message = "failed to parse feed url: %s".formatted(url);
      throw new FeedClientException(message, e);
    } catch (IOException e) {
      var message = "failed to execute http request: %s".formatted(url);
      throw new FeedClientException(message, e);
    }
  }

  private HttpRequest request(URI uri) throws FeedClientException {
    try {
      var targetAddresses = new HostName(uri.getHost()).toAllAddresses();
      var blocked = findOverlappingNetwork(targetAddresses);
      if (blocked.isPresent()) {
        var message =
            "%s (%s) belongs to a blocked network: %s"
                .formatted(uri, Arrays.toString(targetAddresses), blocked.get());
        throw new FeedClientException(message);
      }
    } catch (UnknownHostException e) {
      var message = "IP address of a host could not be determined: %s".formatted(uri.getHost());
      throw new FeedClientException(message, e);
    } catch (HostNameException e) {
      var message = "invalid host name or IP address: %s".formatted(uri.getHost());
      throw new FeedClientException(message, e);
    }

    return HttpRequest.newBuilder()
        .header("User-Agent", "MorningCoffee/1.0 (+https://github.com/parfentjev/morning-coffee)")
        .header(
            "Accept",
            "application/atom+xml, application/rss+xml, application/xml;q=0.9, text/xml;q=0.8,"
                + " */*;q=0.1")
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
        uri.getHost().toLowerCase(Locale.ROOT), _ -> new ThrottlingManager<>(throttlingDelay));
  }

  private Optional<IPAddress> findOverlappingNetwork(IPAddress[] targetAddresses) {
    for (var targetAddress : targetAddresses) {
      var overlap =
          blockedNetworks.stream()
              .filter(blockedNetwork -> blockedNetwork.contains(targetAddress))
              .findFirst();

      if (overlap.isPresent()) {
        return overlap;
      }
    }

    return Optional.empty();
  }
}
