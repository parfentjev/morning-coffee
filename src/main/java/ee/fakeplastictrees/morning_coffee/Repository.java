package ee.fakeplastictrees.morning_coffee;

import java.util.ArrayList;
import java.util.List;

class Repository {
  private final List<FeedEntry> database = new ArrayList<>();

  /// Get feeds. A highly valuable comment, innit?
  public List<Feed> getFeeds() {
    return List.of(
        new Feed("https://feed1.com/"),
        new Feed("https://feed2.ee/"),
        new Feed("https://feed3.fi/"));
  }

  /// Save a feed entry. This operation is idempotent.
  public synchronized void saveEntry(FeedEntry entry) {
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
