package ee.fakeplastictrees.morningcoffee;

import java.util.function.Function;

/// Collects application configuration.
///
/// @param repository repository configuration
/// @param reader feed reader configuration
/// @param webServer web server configuration
public record Config(Repository repository, Reader reader, WebServer webServer) {
  /// Loads application configuration from environment variables.
  public Config() {
    var repository = new Repository();
    var reader = new Reader();
    var webServer = new WebServer();

    this(repository, reader, webServer);
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
  ///
  /// @param postgresUrl PostgreSQL JDBC URL
  /// @param postgresUser PostgreSQL user
  /// @param postgresPassword PostgreSQL password
  public record Repository(String postgresUrl, String postgresUser, String postgresPassword) {
    /// Loads repository configuration from environment variables.
    public Repository() {
      var url = getEnv("REPOSITORY_POSTGRES_URL");
      var user = getEnv("REPOSITORY_POSTGRES_USER");
      var password = getEnv("REPOSITORY_POSTGRES_PASSWORD");

      this(url, user, password);
    }
  }

  /// Configures feed polling.
  ///
  /// @param pollIntervalSeconds delay between feed polls, in seconds
  /// @param requestThrottlingDelaySeconds delay in seconds between requests to the same host
  public record Reader(Long pollIntervalSeconds, Long requestThrottlingDelaySeconds) {
    /// Loads reader configuration from environment variables.
    public Reader() {
      var pollInterval = getEnv("READER_POLL_INTERVAL_SECONDS", Long::valueOf);
      var throttlingDelay = getEnv("READER_REQUEST_THROTTLING_DELAY_SECONDS", Long::valueOf);

      this(pollInterval, throttlingDelay);
    }
  }

  /// Configures HTTP serving.
  ///
  /// @param serverPort HTTP listen serverPort
  /// @param entriesPerPage maximum entries shown per page
  public record WebServer(int serverPort, int entriesPerPage) {
    /// Loads web server configuration from environment variables.
    public WebServer() {
      var port = getEnv("WEB_SERVER_PORT", Integer::valueOf);
      var entriesPerPage = getEnv("WEB_SERVER_ENTRIES_PER_PAGE", Integer::valueOf);

      this(port, entriesPerPage);
    }
  }
}
