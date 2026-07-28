package ee.fakeplastictrees.morningcoffee.repository;

import ee.fakeplastictrees.morningcoffee.Config;
import ee.fakeplastictrees.morningcoffee.model.Feed;
import ee.fakeplastictrees.morningcoffee.model.FeedEntry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// todo: add a PostgreSQL client and use it to run queries
public class Repository {
  private final Logger logger = LogManager.getLogger();

  private final PostgresClient client;

  private Repository(PostgresClient client) {
    this.client = client;
  }

  public static Repository init(Config.Repository config) throws RepositoryException {
    var client = PostgresClient.connect(config);

    return new Repository(client);
  }

  /// @return list of feeds that the service is expected to fetch
  public List<Feed> getFeeds() {
    var feeds = new ArrayList<Feed>();
    var sql = "select name, url from feeds where enabled = true";
    try (var result = client.query(sql)) {
      while (result.next()) {
        var name = result.getString("name");
        var url = result.getString("url");

        feeds.add(new Feed(name, url));
      }
    } catch (SQLException e) {
      logger.error("postgres client error", e);
      return List.of();
    }

    return feeds;
  }

  /// Save a feed entry. This operation is idempotent.
  ///
  /// @param entry [FeedEntry] to save
  public synchronized void saveFeedEntry(FeedEntry entry) {
    // database.add(entry);
  }

  /// Get the latest feed entries.
  ///
  /// @param n number of feed entries to select
  public synchronized List<FeedEntry> getEntries(int n) {
    //    if (database.isEmpty()) {
    //      return List.of();
    //    }
    //
    //    var toIndex = database.size();
    //    var fromIndex = Math.max(0, toIndex - n);
    //
    //    return List.copyOf(database.subList(fromIndex, toIndex)).reversed();
    return List.of();
  }
}
