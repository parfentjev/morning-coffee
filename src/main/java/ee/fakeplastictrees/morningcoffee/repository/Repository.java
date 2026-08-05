package ee.fakeplastictrees.morningcoffee.repository;

import ee.fakeplastictrees.morningcoffee.Config;
import ee.fakeplastictrees.morningcoffee.model.Feed;
import ee.fakeplastictrees.morningcoffee.model.FeedEntry;
import ee.fakeplastictrees.morningcoffee.model.FeedEntryDto;
import java.io.Closeable;
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
public class Repository implements Closeable {
  private static final Logger logger = LogManager.getLogger();

  private final ConnectionPool connectionPool;

  /// Creates a repository from database configuration.
  ///
  /// @param config database configuration
  public Repository(Config.Repository config) {
    this.connectionPool =
        new ConnectionPool(config.postgresUrl(), config.postgresUser(), config.postgresPassword());
  }

  /// Returns feeds enabled for polling.
  ///
  /// @return enabled feeds
  /// @throws RepositoryException if feeds cannot be retrieved
  public List<Feed> getFeeds() throws RepositoryException {
    var feeds = new ArrayList<Feed>();
    var sql =
        """
        select id, url from feeds where enabled = true
        """;
    try (var connection = connectionPool.getConnection();
        var statement = connection.prepareStatement(sql);
        var result = statement.executeQuery()) {
      while (result.next()) {
        var id = UUID.fromString(result.getString("id"));
        var url = result.getString("url");

        feeds.add(new Feed(id, url));
      }
    } catch (SQLException e) {
      throw new RepositoryException("failed to select feeds", e);
    }

    return feeds;
  }

  /// Saves a list of feed entries. This operation is idempotent and entries with the same feed and
  /// external ID won't be inserted twice.
  ///
  /// @param entries feed entry list to save
  /// @throws RepositoryException if feed entries cannot be saved
  public void saveFeedEntries(List<FeedEntry> entries) throws RepositoryException {
    var sql =
        """
        insert into entries(external_id, published_at, feed_id, title, link)
        values(?, ?, ?, ?, ?)
        on conflict(external_id, feed_id) do nothing
        """;
    try (var connection = connectionPool.getConnection();
        var statement = connection.prepareStatement(sql)) {
      try {
        connection.setAutoCommit(false);
        for (var entry : entries) {
          statement.setString(1, entry.getExternalId());
          statement.setObject(2, entry.getPublishedAt());
          statement.setObject(3, entry.getFeedId());
          statement.setString(4, entry.getTitle());
          statement.setString(5, entry.getLink());
          statement.addBatch();
        }

        statement.executeBatch();
        connection.commit();
      } catch (SQLException e) {
        try {
          connection.rollback();
        } catch (SQLException rollbackException) {
          e.addSuppressed(rollbackException);
        }

        throw e;
      }
    } catch (SQLException e) {
      throw new RepositoryException("failed to insert feed entries", e);
    }
  }

  /// Returns the latest feed entries in descending entry ID order.
  ///
  /// @param n maximum number of entries to return
  /// @return up to `n` latest feed entries
  /// @throws RepositoryException if feed entries cannot be retrieved
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
    try (var connection = connectionPool.getConnection();
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
      throw new RepositoryException("failed to select feed entries", e);
    }

    return entries;
  }

  @Override
  public void close() {
    logger.info("shutting down");
    connectionPool.close();
  }
}
