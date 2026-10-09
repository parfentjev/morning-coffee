package ee.fakeplastictrees.morningcoffee.reader;

import inet.ipaddr.HostName;
import inet.ipaddr.HostNameException;
import inet.ipaddr.IPAddress;
import java.io.Closeable;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
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

  // 5MB, should be more than enough even for an irresponsible feed
  private static final int MAX_RESPONSE_BODY_BYTES = 5 * 1024 * 1024;

  private final HttpClient httpClient;
  private final ConcurrentHashMap<String, ThrottlingManager<HttpResponse<byte[]>>>
      throttlingManagers;
  private final Duration throttlingDelay;
  private final List<IPAddress> blockedNetworks;

  /// Creates a feed client with per-host throttling and blocked-network checks.
  ///
  /// @param throttlingDelay delay between completion of an HTTP attempt and the next attempt to the
  ///   same host
  /// @param blockedNetworks networks that feed hosts must not resolve to
  public FeedClient(Duration throttlingDelay, List<IPAddress> blockedNetworks) {
    this.httpClient = HttpClient.newBuilder().build();
    this.throttlingManagers = new ConcurrentHashMap<>();
    this.throttlingDelay = throttlingDelay;
    this.blockedNetworks = blockedNetworks;
  }

  /// Fetches the prepared feed URI and returns its response if the server responds with 200 OK.
  ///
  /// @param uri prepared URI of an RSS/Atom feed
  /// @param timeout timeout for the HTTP request
  /// @return HTTP response containing the raw response body as bytes
  /// @throws FeedClientException if host resolution fails, a target address is blocked, or HTTP I/O
  ///   fails
  /// @throws FeedClientStatusCodeException if the server responds with a status other than 200 OK
  /// @throws InterruptedException if the thread is interrupted
  public HttpResponse<byte[]> fetchFeed(URI uri, Duration timeout)
      throws FeedClientException, FeedClientStatusCodeException, InterruptedException {
    try {
      logger.debug("fetching feed: {}", uri);

      var request = request(uri, timeout);
      // TODO: the whole thread is blocked when a request is throttled, need to fix that
      // perhaps by coordinating with ThrottlingManager in ScheduledFeedReader
      // so that throttled feeds don't even reach this point
      var response = throttlingManager(uri).execute(() -> httpClient.send(request, bodyHandler()));
      if (response.statusCode() != HttpURLConnection.HTTP_OK) {
        throw new FeedClientStatusCodeException(response.statusCode());
      }

      return response;
    } catch (IOException e) {
      var message = "failed to execute http request: %s".formatted(e.getMessage());
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
