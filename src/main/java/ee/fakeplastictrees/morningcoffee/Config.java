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

  private static String getEnv(String key) {
    return getEnv(key, v -> v);
  }

  private static <T> T getEnv(String key, Function<String, T> parser) {
    var value = System.getenv(key);
    if (value == null || value.isBlank()) {
      throw new RuntimeException("Env property is missing: " + key);
    }

    return parser.apply(value);
  }

  /// Configures PostgreSQL access.
  public static final class Repository {
    private final String postgresUrl;
    private final String postgresUser;
    private final String postgresPassword;

    private Repository() {
      this.postgresUrl = getEnv("REPOSITORY_POSTGRES_URL");
      this.postgresUser = getEnv("REPOSITORY_POSTGRES_USER");
      this.postgresPassword = getEnv("REPOSITORY_POSTGRES_PASSWORD");
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

    private Reader() {
      this.pollIntervalSeconds = getEnv("READER_POLL_INTERVAL_SECONDS", Long::valueOf);
      this.requestThrottlingDelaySeconds =
          getEnv("READER_REQUEST_THROTTLING_DELAY_SECONDS", Long::valueOf);
      this.blockedNetworks = getEnv("READER_BLOCKED_NETWORKS", Reader::mapStringToIpAddress);
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
      this.serverHostname = getEnv("WEB_SERVER_HOSTNAME");
      this.serverPort = getEnv("WEB_SERVER_PORT", Integer::valueOf);
      this.entriesPerPage = getEnv("WEB_SERVER_ENTRIES_PER_PAGE", Integer::valueOf);
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
}
