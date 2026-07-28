package ee.fakeplastictrees.morningcoffee;

import java.util.function.Function;

public record Config(Repository repository, Reader reader, WebServer webServer) {
  public Config() {
    var repository = new Repository();
    var reader = new Reader();
    var webServer = new WebServer();

    this(repository, reader, webServer);
  }

  private static String getenv(String key) {
    return getenv(key, v -> v);
  }

  private static <T> T getenv(String key, Function<String, T> parser) {
    var value = System.getenv(key);
    if (value == null || value.isBlank()) {
      throw new RuntimeException("Env property is missing: " + key);
    }

    return parser.apply(value);
  }

  public record Repository(String postgresUrl) {
    public Repository() {
      var postgresUrl = getenv("REPOSITORY_POSTGRES_URL");

      this(postgresUrl);
    }
  }

  public record Reader(Long pollIntervalSeconds) {
    public Reader() {
      var pollIntervalSeconds = getenv("READER_POLL_INTERVAL_SECONDS", Long::valueOf);

      this(pollIntervalSeconds);
    }
  }

  public record WebServer(int port, int entriesPerPage) {
    public WebServer() {
      var port = getenv("WEB_SEVER_PORT", Integer::valueOf);
      var entriesPerPage = getenv("WEB_SERVER_ENTRIES_PER_PAGE", Integer::valueOf);

      this(port, entriesPerPage);
    }
  }
}
