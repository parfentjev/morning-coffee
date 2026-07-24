package ee.fakeplastictrees.morningcoffee;

import ee.fakeplastictrees.morningcoffee.model.Feed;
import ee.fakeplastictrees.morningcoffee.model.FeedEntry;
import java.util.ArrayList;
import java.util.List;

public class Repository {
  private final Config.Repository config;
  private final List<FeedEntry> database = new ArrayList<>();

  Repository(Config.Repository config) {
    this.config = config;
  }

  /// Get feeds. A highly valuable comment, innit?
  public List<Feed> getFeeds() {
    return List.of(new Feed("https://github.com/dani-garcia/vaultwarden/releases.atom"));
  }

  /// Save a feed entry. This operation is idempotent.
  public synchronized void saveFeedEntry(FeedEntry entry) {
    database.add(entry);
  }

  /// Get the last `n` feed entires.
  public synchronized List<FeedEntry> getEntries(int n) {
    if (database.size() == 0) {
      return List.of();
    }

    var toIndex = database.size();
    var fromIndex = Math.max(0, toIndex - n);

    return List.copyOf(database.subList(fromIndex, toIndex)).reversed();
  }
}
