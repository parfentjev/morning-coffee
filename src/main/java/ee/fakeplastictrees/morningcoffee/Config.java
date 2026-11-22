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
  private final Brief brief;

  private Config(Repository repository, Reader reader, WebServer webServer, Brief brief) {
    this.repository = repository;
    this.reader = reader;
    this.webServer = webServer;
    this.brief = brief;
  }

  /// Loads application configuration from environment variables.
  ///
  /// @return initialized application configuration
  public static Config init() {
    return new Config(new Repository(), new Reader(), new WebServer(), new Brief());
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

  /// {@return the brief provider configuration}
  public Brief brief() {
    return brief;
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

    /// {@return the JDBC URL for the feed and brief database}
    public String postgresUrl() {
      return postgresUrl;
    }

    /// {@return the database user used to open connections}
    public String postgresUser() {
      return postgresUser;
    }

    /// {@return the password used to authenticate the database user}
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

    /// {@return the interval between feed polls, in seconds}
    public long pollIntervalSeconds() {
      return pollIntervalSeconds;
    }

    /// {@return the configured delay between feed requests, in seconds}
    public long requestThrottlingDelaySeconds() {
      return requestThrottlingDelaySeconds;
    }

    /// {@return the networks excluded from feed retrieval}
    @SuppressFBWarnings(
        value = "EI_EXPOSE_REP",
        justification = "List is created with Stream.toList(), which is immutable.")
    public List<IPAddress> blockedNetworks() {
      return blockedNetworks;
    }

    /// {@return the maximum number of concurrent feed fetches}
    public int maxParallelFetches() {
      return maxParallelFetches;
    }

    /// {@return the maximum number of entries processed per fetch}
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
    private final int briefsPerPage;

    private WebServer() {
      var env = new EnvironmentReader("WEB_SERVER");
      this.serverHostname = env.optional("HOSTNAME", "127.0.0.1");
      this.serverPort = env.optional("PORT", 8080, Integer::valueOf);
      this.entriesPerPage = env.optional("ENTRIES_PER_PAGE", 100, Integer::valueOf);
      this.briefsPerPage = env.optional("BRIEFS_PER_PAGE", 1, Integer::valueOf);
    }

    /// {@return the host name or address to bind the HTTP server to}
    public String serverHostname() {
      return serverHostname;
    }

    /// {@return the port on which the HTTP server listens}
    public int serverPort() {
      return serverPort;
    }

    /// {@return the maximum number of feed entries requested per page}
    public int entriesPerPage() {
      return entriesPerPage;
    }

    /// {@return the maximum number of briefs requested per page}
    public int briefsPerPage() {
      return briefsPerPage;
    }
  }

  /// Configures Brief providers.
  public static final class Brief {
    private final String provider;
    private final Anthropic anthropic;

    private Brief() {
      var env = new EnvironmentReader("BRIEF");
      this.provider = env.optional("PROVIDER", "DUMMY");
      this.anthropic = new Anthropic();
    }

    /// {@return the configured brief provider name}
    public String provider() {
      return provider;
    }

    /// {@return the configuration for the Anthropic brief provider}
    public Anthropic anthropic() {
      return anthropic;
    }

    /// Configures Anthropic Brief providers.
    public static final class Anthropic {
      private final String apiKey;
      private final String basePrompt;
      private final long maxTokens;
      private final String model;

      private Anthropic() {
        var env = new EnvironmentReader("BRIEF_ANTHROPIC");
        this.apiKey = env.optional("API_KEY", null);
        this.basePrompt = env.optional("BASE_PROMPT", null);
        this.maxTokens = env.optional("MAX_TOKENS", 2048L, Long::valueOf);
        this.model = env.optional("MODEL", null);
      }

      /// {@return the API key used to authenticate Anthropic requests}
      public String apiKey() {
        return apiKey;
      }

      /// {@return the instructions used to generate a brief from feed entries}
      public String basePrompt() {
        return basePrompt;
      }

      /// {@return the maximum number of output tokens requested per brief}
      public long maxTokens() {
        return maxTokens;
      }

      /// {@return the Anthropic model ID used for brief generation}
      public String model() {
        return model;
      }
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
