package ee.fakeplastictrees.morningcoffee.repository;

import ee.fakeplastictrees.morningcoffee.Config;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

class PostgresClient {
  private static final int LOGIN_TIMEOUT_SECONDS = 5;

  private final Config.Repository config;
  private final Properties connectionProperties;

  PostgresClient(Config.Repository config) {
    DriverManager.setLoginTimeout(LOGIN_TIMEOUT_SECONDS);

    this.config = config;
    this.connectionProperties = new Properties();
    connectionProperties.setProperty("user", config.postgresUser());
    connectionProperties.setProperty("password", config.postgresPassword());
    connectionProperties.setProperty("loginTimeout", "5");
    connectionProperties.setProperty("socketTimeout", "5");
  }

  public Connection connect() throws SQLException {
    // I suppose normally a connection pool would be used,
    // But I think DriverManager offers no such functionality.
    //
    // For a single-user program running behind a VPN connection, this should be enough.
    // But it can be a good exercise to implement own connection pool in the future:
    // https://www.baeldung.com/java-connection-pooling
    return DriverManager.getConnection(config.postgresUrl(), connectionProperties);
  }
}
