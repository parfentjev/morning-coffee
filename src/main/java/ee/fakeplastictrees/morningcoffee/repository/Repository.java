package ee.fakeplastictrees.morningcoffee.repository;

import ee.fakeplastictrees.morningcoffee.Config;
import ee.fakeplastictrees.morningcoffee.model.Feed;
import ee.fakeplastictrees.morningcoffee.model.FeedEntry;
import ee.fakeplastictrees.morningcoffee.model.FeedEntryDto;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// todo: it's a mess of different closeable objects, need to rethink this
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
    try (var statement = client.statement(sql)) {
      var result = statement.executeQuery();
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
      statement.setString(1, entry.getExternalId());
      statement.setObject(2, entry.getPublishedAt());
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
  public synchronized List<FeedEntryDto> getEntries(int n) {
    var entries = new ArrayList<FeedEntryDto>();
    var sql =
        """
        select
        f.name as \"feed_name\",
        e.title as \"entry_title\", e.link as \"entry_link\", e.published_at as \"entry_published_at\"
        from entries e
        join feeds f on f.id = e.feed_id
        order by e.id desc limit ?;
        """;
    try (var statement = client.statement(sql)) {
      statement.setInt(1, n);

      var result = statement.executeQuery();
      while (result.next()) {
        var feedName = result.getString("feed_name");
        var title = result.getString("entry_title");
        var link = result.getString("entry_link");
        var publishedAt = result.getObject("entry_published_at", OffsetDateTime.class);

        var entry = new FeedEntryDto(feedName, title, link, publishedAt);
        entries.add(entry);
      }
    } catch (SQLException e) {
      logger.error("failed to insert feed entry", e);
    }

    return entries;
  }
}
