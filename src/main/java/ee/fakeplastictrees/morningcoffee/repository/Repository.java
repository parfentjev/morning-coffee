package ee.fakeplastictrees.morningcoffee.repository;

import ee.fakeplastictrees.morningcoffee.Config;
import ee.fakeplastictrees.morningcoffee.model.Feed;
import ee.fakeplastictrees.morningcoffee.model.FeedEntry;
import java.util.ArrayList;
import java.util.List;

// todo: add a PostgreSQL client and use it to run queries
public class Repository {
  private final Config.Repository config;
  private final List<FeedEntry> database = new ArrayList<>();

  public Repository(Config.Repository config) {
    this.config = config;
  }

  /// @return list of feeds that the service is expected to fetch
  public List<Feed> getFeeds() {
    return List.of(new Feed("https://github.com/dani-garcia/vaultwarden/releases.atom"));
  }

  /// Save a feed entry. This operation is idempotent.
  ///
  /// @param entry [FeedEntry] to save
  public synchronized void saveFeedEntry(FeedEntry entry) {
    database.add(entry);
  }

  /// Get the latest feed entries.
  ///
  /// @param n number of feed entries to select
  public synchronized List<FeedEntry> getEntries(int n) {
    if (database.size() == 0) {
      return List.of();
    }

    var toIndex = database.size();
    var fromIndex = Math.max(0, toIndex - n);

    return List.copyOf(database.subList(fromIndex, toIndex)).reversed();
  }
}
