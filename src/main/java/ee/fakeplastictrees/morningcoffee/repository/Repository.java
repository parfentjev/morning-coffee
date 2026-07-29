package ee.fakeplastictrees.morningcoffee.repository;

import ee.fakeplastictrees.morningcoffee.Config;
import ee.fakeplastictrees.morningcoffee.model.Feed;
import ee.fakeplastictrees.morningcoffee.model.FeedEntry;
import java.sql.SQLException;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
    var sql =
        """
        select id, url from feeds where enabled = true
        """;
    try (var result = client.query(sql)) {
      while (result.next()) {
        var id = UUID.fromString(result.getString("id"));
        var url = result.getString("url");

        feeds.add(new Feed(id, url));
      }
    } catch (SQLException e) {
      logger.error("failed to select feeds", e);
      return List.of();
    }

    return feeds;
  }

  /// Save a feed entry. This operation is idempotent.
  ///
  /// @param entry [FeedEntry] to save
  public synchronized void saveFeedEntry(FeedEntry entry) {
    var sql =
        """
        insert into entries(external_id, published_at, feed_id, title, link)
        values(?, ?, ?, ?, ?)
        on conflict(external_id, feed_id) do nothing
        """;
    try (var statement = client.statement(sql)) {
      statement.setString(1, entry.getExtrnalId());
      statement.setObject(2, entry.getPublishedAt().atOffset(ZoneOffset.UTC));
      statement.setObject(3, entry.getFeedId());
      statement.setString(4, entry.getTitle());
      statement.setString(5, entry.getLink());
      statement.execute();
    } catch (SQLException e) {
      logger.error("failed to insert feed entry", e);
    }
  }

  /// Get the latest feed entries.
  ///
  /// @param n number of feed entries to select
  public synchronized List<FeedEntry> getEntries(int n) {
    // todo: return actual entries from the db
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
