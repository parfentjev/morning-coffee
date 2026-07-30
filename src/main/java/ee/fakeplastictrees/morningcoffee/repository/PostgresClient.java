package ee.fakeplastictrees.morningcoffee.repository;

import ee.fakeplastictrees.morningcoffee.Config;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

class PostgresClient {
  private final Config.Repository config;

  PostgresClient(Config.Repository config) {
    this.config = config;
  }

  public Connection connect() throws SQLException {
    // I suppose normally a connection pool would be used,
    // But I think DriverManager offers no such functionality.
    //
    // For a single-user program running behind a VPN connection, this should be enough.
    // But it can be a good exercise to implement own connection pool in the future:
    // https://www.baeldung.com/java-connection-pooling
    return DriverManager.getConnection(
        config.postgresUrl(), config.postgresUser(), config.postgresPassword());
  }
}
