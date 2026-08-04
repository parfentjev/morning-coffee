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

/// Provides PostgreSQL persistence for feeds and feed entries.
///
/// Each operation opens and closes its own database connection and JDBC resources.
public class Repository {
  private static final Logger logger = LogManager.getLogger();

  private final PostgresClient client;

  /// Creates a repository from database configuration.
  ///
  /// @param config database configuration
  public Repository(Config.Repository config) {
    this.client = new PostgresClient(config);
  }

  /// Returns feeds enabled for polling.
  ///
  /// @return enabled feeds, or an empty list if the query fails
  public List<Feed> getFeeds() {
    var feeds = new ArrayList<Feed>();
    var sql =
        """
        select id, url from feeds where enabled = true
        """;
    try (var connection = client.connect();
        var statement = connection.prepareStatement(sql);
        var result = statement.executeQuery()) {
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

  /// Saves a feed entry unless the same feed and external ID already exist.
  ///
  /// @param entry feed entry to save
  public void saveFeedEntry(FeedEntry entry) {
    var sql =
        """
        insert into entries(external_id, published_at, feed_id, title, link)
        values(?, ?, ?, ?, ?)
        on conflict(external_id, feed_id) do nothing
        """;
    try (var connection = client.connect();
        var statement = connection.prepareStatement(sql)) {
      statement.setString(1, entry.getExternalId());
      statement.setObject(2, entry.getPublishedAt());
      statement.setObject(3, entry.getFeedId());
      statement.setString(4, entry.getTitle());
      statement.setString(5, entry.getLink());
      statement.executeUpdate();
    } catch (SQLException e) {
      logger.error("failed to insert feed entry", e);
    }
  }

  /// Returns the latest feed entries in descending entry ID order.
  ///
  /// @param n maximum number of entries to return
  /// @return up to `n` latest feed entries; may be empty if the query fails
  public List<FeedEntryDto> getEntries(int n) throws RepositoryException {
    var entries = new ArrayList<FeedEntryDto>();
    var sql =
        """
        select
        f.name as "feed_name",
        e.title as "entry_title", e.link as "entry_link", e.published_at as "entry_published_at"
        from entries e
        join feeds f on f.id = e.feed_id
        where f.enabled = true
        order by e.id desc limit ?;
        """;
    try (var connection = client.connect();
        var statement = connection.prepareStatement(sql)) {
      statement.setInt(1, n);
      try (var result = statement.executeQuery()) {
        while (result.next()) {
          var feedName = result.getString("feed_name");
          var title = result.getString("entry_title");
          var link = result.getString("entry_link");
          var publishedAt = result.getObject("entry_published_at", OffsetDateTime.class);

          var entry = new FeedEntryDto(feedName, title, link, publishedAt);
          entries.add(entry);
        }
      }
    } catch (SQLException e) {
      throw new RepositoryException(e);
    }

    return entries;
  }
}
