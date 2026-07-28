package ee.fakeplastictrees.morningcoffee;

import java.util.function.Function;

public record Config(Repository repository, Reader reader, WebServer webServer) {
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

  public record Repository(String postgresUrl, String postgresUser, String postgresPassword) {
    public Repository() {
      var postgresUrl = getEnv("REPOSITORY_POSTGRES_URL");
      var postgresUser = getEnv("REPOSITORY_POSTGRES_USER");
      var postgresPassword = getEnv("REPOSITORY_POSTGRES_PASSWORD");

      this(postgresUrl, postgresUser, postgresPassword);
    }
  }

  public record Reader(Long pollIntervalSeconds) {
    public Reader() {
      var pollIntervalSeconds = getEnv("READER_POLL_INTERVAL_SECONDS", Long::valueOf);

      this(pollIntervalSeconds);
    }
  }

  public record WebServer(int port, int entriesPerPage) {
    public WebServer() {
      var port = getEnv("WEB_SEVER_PORT", Integer::valueOf);
      var entriesPerPage = getEnv("WEB_SERVER_ENTRIES_PER_PAGE", Integer::valueOf);

      this(port, entriesPerPage);
    }
  }
}
