package ee.fakeplastictrees.morningcoffee.repository;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.Closeable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

class ConnectionPool implements Closeable {
  private final HikariDataSource dataSource;

  /// Creates a new database connection pool.
  ///
  /// @param url jdbc url string: `jdbc:postgresql://127.0.0.1:5432/database`
  /// @param username username used to establish a connection
  /// @param password password used to establish a connection
  ConnectionPool(String url, String username, String password) {
    var config = new HikariConfig();
    config.setJdbcUrl(url);
    config.setUsername(username);
    config.setPassword(password);
    config.setConnectionTimeout(5000);
    config.setValidationTimeout(1000);
    config.setInitializationFailTimeout(10000);
    config.setMaximumPoolSize(10);
    config.setMinimumIdle(1);
    config.setAutoCommit(true);

    var properties = new Properties();
    properties.setProperty("connectTimeout", "5");
    properties.setProperty("socketTimeout", "5");
    config.setDataSourceProperties(properties);

    this.dataSource = new HikariDataSource(config);
  }

  Connection getConnection() throws SQLException {
    return dataSource.getConnection();
  }

  @Override
  public void close() {
    dataSource.close();
  }
}
