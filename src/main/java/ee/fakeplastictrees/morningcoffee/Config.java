package ee.fakeplastictrees.morningcoffee;

import java.util.function.Function;

public class Config {
  private final Repository repository;
  private final Reader reader;

  Config() {
    this.repository = new Repository();
    this.reader = new Reader();
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

  public Repository repository() {
    return repository;
  }

  public Reader reader() {
    return reader;
  }

  public static class Repository {
    private final String postgresUrl;

    Repository() {
      this.postgresUrl = getenv("REPOSITORY_POSTGRES_URL");
    }

    public String getPostgresUrl() {
      return postgresUrl;
    }
  }

  public static class Reader {
    private final Long pollIntervalSeconds;

    Reader() {
      this.pollIntervalSeconds = getenv("READER_POLL_INTERVAL_SECONDS", Long::valueOf);
    }

    public Long getPollIntervalSeconds() {
      return pollIntervalSeconds;
    }
  }
}
