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
      var postgresUrl = getEnv("REPOSITORY_POSTGRES_URL");
      var postgresUser = getEnv("REPOSITORY_POSTGRES_USER");
      var postgresPassword = getEnv("REPOSITORY_POSTGRES_PASSWORD");

      this(postgresUrl, postgresUser, postgresPassword);
    }
  }

  /// Configures feed polling.
  ///
  /// @param pollIntervalSeconds delay between feed polls, in seconds
  public record Reader(Long pollIntervalSeconds) {
    /// Loads reader configuration from environment variables.
    public Reader() {
      var pollIntervalSeconds = getEnv("READER_POLL_INTERVAL_SECONDS", Long::valueOf);

      this(pollIntervalSeconds);
    }
  }

  /// Configures HTTP serving.
  ///
  /// @param serverPort HTTP listen serverPort
  /// @param entriesPerPage maximum entries shown per page
  public record WebServer(int serverPort, int entriesPerPage) {
    /// Loads web server configuration from environment variables.
    public WebServer() {
      var serverPort = getEnv("WEB_SERVER_PORT", Integer::valueOf);
      var entriesPerPage = getEnv("WEB_SERVER_ENTRIES_PER_PAGE", Integer::valueOf);

      this(serverPort, entriesPerPage);
    }
  }
}
