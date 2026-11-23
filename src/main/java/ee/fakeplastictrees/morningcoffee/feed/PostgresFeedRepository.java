package ee.fakeplastictrees.morningcoffee.feed;

import ee.fakeplastictrees.morningcoffee.common.ConnectionPool;
import ee.fakeplastictrees.morningcoffee.common.RepositoryException;
import java.sql.SQLException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/// Provides PostgreSQL persistence for feeds and feed entries.
///
/// Each operation opens and closes its own database connection and JDBC resources.
final class PostgresFeedRepository {
  private final ConnectionPool connectionPool;

  /// Creates a feed repository backed by the supplied connection pool.
  ///
  /// @param connectionPool pool used for feed reads and writes
  PostgresFeedRepository(ConnectionPool connectionPool) {
    this.connectionPool = connectionPool;
  }

  /// Returns feeds enabled for polling.
  ///
  /// @return enabled feeds
  /// @throws RepositoryException if feeds cannot be retrieved
  List<Feed> getFeeds() throws RepositoryException {
    var feeds = new ArrayList<Feed>();
    var sql =
        """
        select id, url, request_timeout_seconds from feeds where enabled = true
        """;
    try (var connection = connectionPool.getConnection();
        var statement = connection.prepareStatement(sql);
        var result = statement.executeQuery()) {
      while (result.next()) {
        var id = UUID.fromString(result.getString("id"));
        var url = result.getString("url");
        var timeout = Duration.ofSeconds(result.getInt("request_timeout_seconds"));

        feeds.add(new Feed(id, url, timeout));
      }
    } catch (SQLException e) {
      throw new RepositoryException("failed to select feeds", e);
    }

    return feeds;
  }

  /// Saves a list of feed entries. This operation is idempotent and entries with the same feed and
  /// external ID won't be inserted twice. Missing optional content is filled atomically; existing
  /// content and metadata are preserved.
  ///
  /// @param entries feed entry list to save
  /// @throws RepositoryException if feed entries cannot be saved
  void saveFeedEntries(List<FeedEntry> entries) throws RepositoryException {
    var entrySql =
        """
        insert into entries(external_id, published_at, feed_id, title, link)
        values(?, ?, ?, ?, ?)
        on conflict(external_id, feed_id) do nothing
        """;

    var contentSql =
        """
        insert into public.entry_contents(entry_id, body, body_content_type)
        select id, ?, ? from public.entries where external_id = ? and feed_id = ?
        on conflict(entry_id) do nothing
        """;

    try (var connection = connectionPool.getConnection();
        var statement = connection.prepareStatement(entrySql);
        var contentStatement = connection.prepareStatement(contentSql)) {
      try {
        connection.setAutoCommit(false);
        for (var entry : entries) {
          statement.setString(1, entry.externalId());
          statement.setObject(2, entry.publishedAt());
          statement.setObject(3, entry.feedId());
          statement.setString(4, entry.title());
          statement.setString(5, entry.link());
          statement.addBatch();
        }

        statement.executeBatch();
        for (var entry : entries) {
          if (entry.body() != null) {
            contentStatement.setString(1, entry.body());
            contentStatement.setString(2, entry.bodyContentType());
            contentStatement.setString(3, entry.externalId());
            contentStatement.setObject(4, entry.feedId());
            contentStatement.addBatch();
          }
        }

        contentStatement.executeBatch();
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
      throw new RepositoryException("failed to save feed entries and optional content", e);
    }
  }

  /// Returns the latest feed entries in descending entry ID order.
  ///
  /// @param n maximum number of entries to return
  /// @return up to `n` latest feed entries
  /// @throws RepositoryException if feed entries cannot be retrieved
  List<FeedEntryDto> getEntries(int n) throws RepositoryException {
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
}
