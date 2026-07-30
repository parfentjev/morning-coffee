package ee.fakeplastictrees.morningcoffee.repository;

import ee.fakeplastictrees.morningcoffee.Config;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

class PostgresClient {
  private final Logger logger = LogManager.getLogger();

  private final Connection connection;

  private PostgresClient(Connection connection) {
    this.connection = connection;
    logger.debug("postgres client is ready");
  }

  static PostgresClient connect(Config.Repository config) throws RepositoryException {
    try {
      var connection =
          DriverManager.getConnection(
              config.postgresUrl(), config.postgresUser(), config.postgresPassword());

      return new PostgresClient(connection);
    } catch (SQLException e) {
      throw new RepositoryException("failed to create postgres connection", e);
    }
  }

  PreparedStatement statement(String sql) throws SQLException {
    return connection.prepareStatement(sql);
  }
}
