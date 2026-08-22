package ee.fakeplastictrees.morningcoffee.reader;

import inet.ipaddr.HostName;
import inet.ipaddr.HostNameException;
import inet.ipaddr.IPAddress;
import java.io.Closeable;
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

class FeedClient implements Closeable {
  private static final Logger logger = LogManager.getLogger();

  // 1MB, should be more than enough for a responsible feed
  private static final int MAX_RESPONSE_BODY_BYTES = 1 * 1024 * 1024;

  private final HttpClient httpClient;
  private final ConcurrentHashMap<String, ThrottlingManager<HttpResponse<byte[]>>>
      throttlingManagers;
  private final Duration throttlingDelay;
  private final List<IPAddress> blockedNetworks;

  /// Creates a new instance of FeedClient.
  ///
  /// @param throttlingDelay delay in seconds between requests to the same host
  public FeedClient(long throttlingDelay, List<IPAddress> blockedNetworks) {
    this.httpClient = HttpClient.newBuilder().build();
    this.throttlingManagers = new ConcurrentHashMap<>();
    this.throttlingDelay = Duration.ofSeconds(throttlingDelay);
    this.blockedNetworks = blockedNetworks;
  }

  /// Requests the given `url` and returns the raw response body if the server responds with 200 OK.
  ///
  /// @param url address of an RSS/Atom feed
  /// @param timeout timeout for the HTTP request
  /// @throws FeedClientException if the URL is malformed or the request fails
  /// @throws FeedClientStatusCodeException if the server returns an unexpected status code
  /// @throws InterruptedException if the thread is interrupted
  public HttpResponse<byte[]> fetchFeed(String url, Duration timeout)
      throws FeedClientException, FeedClientStatusCodeException, InterruptedException {
    try {
      var uri = new URI(url);
      logger.debug("fetching feed: {}", uri);

      var request = request(uri, timeout);
      var response = throttlingManager(uri).execute(() -> httpClient.send(request, bodyHandler()));
      if (response.statusCode() != HttpURLConnection.HTTP_OK) {
        throw new FeedClientStatusCodeException(response.statusCode());
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

  private HttpRequest request(URI uri, Duration timeout) throws FeedClientException {
    try {
      var targetAddresses = new HostName(uri.getHost()).toAllAddresses();
      var overlap = findOverlappingNetwork(targetAddresses);
      if (overlap.isPresent()) {
        var message =
            "%s (%s) belongs to a blocked network: %s"
                .formatted(uri, Arrays.toString(targetAddresses), overlap.get());
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
        .timeout(timeout)
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

  @Override
  public void close() {
    httpClient.close();
  }
}
