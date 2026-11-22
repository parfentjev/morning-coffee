package ee.fakeplastictrees.morningcoffee.brief;

import ee.fakeplastictrees.morningcoffee.common.repository.ConnectionPool;
import ee.fakeplastictrees.morningcoffee.common.repository.RepositoryException;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

class PostgresBriefRepository implements BriefRepository {
  private final ConnectionPool pool;

  /// Creates a repository for briefs stored in PostgreSQL.
  ///
  /// @param pool connection pool used for reads and writes
  PostgresBriefRepository(ConnectionPool pool) {
    this.pool = pool;
  }

  @Override
  public void save(Brief brief) throws RepositoryException {
    var sql =
        """
        insert into briefs(published_at, body) values(?, ?);
        """;

    try (var conn = pool.getConnection();
        var statement = conn.prepareStatement(sql)) {
      statement.setObject(1, brief.publishedAt());
      statement.setString(2, brief.body());
      statement.execute();
    } catch (SQLException e) {
      throw new RepositoryException("failed to save a brief", e);
    }
  }

  @Override
  public List<Brief> latest(int n) throws RepositoryException {
    var briefs = new ArrayList<Brief>();

    var sql =
        """
        select
            published_at,
            body
        from
            briefs
        order by
            published_at desc
        limit ?
        """;

    try (var conn = pool.getConnection();
        var statement = conn.prepareStatement(sql)) {
      statement.setInt(1, n);

      try (var result = statement.executeQuery()) {
        while (result.next()) {
          var publishedAt = result.getObject("published_at", OffsetDateTime.class);
          var body = result.getString("body");

          var brief = new Brief(publishedAt, body);
          briefs.add(brief);
        }
      }
    } catch (SQLException e) {
      throw new RepositoryException("failed to select latest briefs", e);
    }

    return briefs;
  }
}
