package ee.fakeplastictrees.morningcoffee;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import inet.ipaddr.AddressStringException;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import inet.ipaddr.IncompatibleAddressException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/// Collects application configuration.
public final class Config {
  private final Repository repository;
  private final Reader reader;
  private final WebServer webServer;

  private Config(Repository repository, Reader reader, WebServer webServer) {
    this.repository = repository;
    this.reader = reader;
    this.webServer = webServer;
  }

  /// Loads application configuration from environment variables.
  ///
  /// @return initialized application configuration
  public static Config init() {
    return new Config(new Repository(), new Reader(), new WebServer());
  }

  /// Returns repository configuration.
  ///
  /// @return repository configuration
  public Repository repository() {
    return repository;
  }

  /// Returns feed reader configuration.
  ///
  /// @return feed reader configuration
  public Reader reader() {
    return reader;
  }

  /// Returns web server configuration.
  ///
  /// @return web server configuration
  public WebServer webServer() {
    return webServer;
  }

  /// Configures PostgreSQL access.
  public static final class Repository {
    private final String postgresUrl;
    private final String postgresUser;
    private final String postgresPassword;

    private Repository() {
      var env = new EnvironmentReader("REPOSITORY_POSTGRES");
      this.postgresUrl = env.required("URL");
      this.postgresUser = env.required("USER");
      this.postgresPassword = env.required("PASSWORD");
    }

    public String postgresUrl() {
      return postgresUrl;
    }

    public String postgresUser() {
      return postgresUser;
    }

    public String postgresPassword() {
      return postgresPassword;
    }
  }

  /// Configures feed polling.
  public static final class Reader {
    private final long pollIntervalSeconds;
    private final long requestThrottlingDelaySeconds;
    private final List<IPAddress> blockedNetworks;
    private final int maxParallelFetches;
    private final int maxEntriesPerFetch;

    private Reader() {
      var env = new EnvironmentReader("READER");
      this.pollIntervalSeconds = env.optional("POLL_INTERVAL_SECONDS", 1800L, Long::valueOf);
      this.requestThrottlingDelaySeconds =
          env.optional("REQUEST_THROTTLING_DELAY_SECONDS", 30L, Long::valueOf);
      this.blockedNetworks =
          env.optional("BLOCKED_NETWORKS", List.of(), Reader::mapStringToIpAddress);
      this.maxParallelFetches = env.optional("MAX_PARALLEL_FETCHES", 5, Integer::valueOf);
      this.maxEntriesPerFetch = env.optional("MAX_ENTRIES_PER_FETCH", 10, Integer::valueOf);
    }

    public long pollIntervalSeconds() {
      return pollIntervalSeconds;
    }

    public long requestThrottlingDelaySeconds() {
      return requestThrottlingDelaySeconds;
    }

    @SuppressFBWarnings(
        value = "EI_EXPOSE_REP",
        justification = "List is created with Stream.toList(), which is immutable.")
    public List<IPAddress> blockedNetworks() {
      return blockedNetworks;
    }

    public int maxParallelFetches() {
      return maxParallelFetches;
    }

    public int maxEntriesPerFetch() {
      return maxEntriesPerFetch;
    }

    private static List<IPAddress> mapStringToIpAddress(String input) {
      return Arrays.stream(input.split(",")).map(Reader::parseIpAddress).toList();
    }

    private static IPAddress parseIpAddress(String input) {
      try {
        var address = new IPAddressString(input).toAddress();
        if (address == null) {
          var message =
              "represents only a network prefix or the empty address string: %s".formatted(input);
          throw new RuntimeException(message);
        }

        return address;
      } catch (AddressStringException e) {
        var message = "the address format is invalid: %s".formatted(input);
        throw new RuntimeException(message, e);
      } catch (IncompatibleAddressException e) {
        var message =
            "address string representing multiple addresses cannot be represented: %s"
                .formatted(input);
        throw new RuntimeException(message, e);
      }
    }
  }

  /// Configures HTTP serving.
  public static final class WebServer {
    private final String serverHostname;
    private final int serverPort;
    private final int entriesPerPage;

    private WebServer() {
      var env = new EnvironmentReader("WEB_SERVER");
      this.serverHostname = env.optional("HOSTNAME", "127.0.0.1");
      this.serverPort = env.optional("PORT", 8080, Integer::valueOf);
      this.entriesPerPage = env.optional("ENTRIES_PER_PAGE", 100, Integer::valueOf);
    }

    public String serverHostname() {
      return serverHostname;
    }

    public int serverPort() {
      return serverPort;
    }

    public int entriesPerPage() {
      return entriesPerPage;
    }
  }

  private record EnvironmentReader(String prefix) {
    private String optional(String key, String fallback) {
      return optional(key, fallback, v -> v);
    }

    private <T> T optional(String key, T fallback, Function<String, T> parser) {
      var fullKey = "%s_%s".formatted(prefix, key);
      var value = System.getenv(fullKey);
      if (value == null || value.isBlank()) {
        return fallback;
      }

      return parser.apply(value);
    }

    private String required(String key) {
      return required(key, v -> v);
    }

    private <T> T required(String key, Function<String, T> parser) {
      var value = optional(key, null);
      if (value == null || value.isBlank()) {
        throw new RuntimeException("Env property is missing: " + key);
      }

      return parser.apply(value);
    }
  }
}
