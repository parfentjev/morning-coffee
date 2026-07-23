package ee.fakeplastictrees.morning_coffee;

class Config {
  private final Repository repository;
  private final Reader reader;

  Config() {
    this.repository = new Repository();
    this.reader = new Reader();
  }

  public Repository getRepository() {
    return repository;
  }

  public Reader getReader() {
    return reader;
  }

  public static class Repository {
    private final String postgresUrl;

    Repository() {
      this.postgresUrl = System.getenv("POSTGRES_URL");
    }

    public String getPostgresUrl() {
      return postgresUrl;
    }
  }

  public static class Reader {
    private final Integer pollIntervalMinutes;

    Reader() {
      this.pollIntervalMinutes = Integer.valueOf(System.getenv("READER_POLL_INTERVAL_MINUTES"));
    }

    public Integer getPollIntervalMinutes() {
      return pollIntervalMinutes;
    }
  }
}
