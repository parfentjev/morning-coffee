package ee.fakeplastictrees.morningcoffee;

import ee.fakeplastictrees.morningcoffee.reader.ScheduledFeedReader;
import ee.fakeplastictrees.morningcoffee.repository.Repository;
import ee.fakeplastictrees.morningcoffee.webserver.WebServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class App {
  private static final Logger logger = LogManager.getLogger();

  public static void main(String[] args) {
    try {
      new App().start();
    } catch (Exception e) {
      logger.error("unhandled exception", e);
      System.exit(1);
    }
  }

  private void start() throws Exception {
    var config = new Config();
    var repository = new Repository(config.repository());

    var reader = new ScheduledFeedReader(config.reader(), repository);
    reader.start();

    var server = new WebServer(config.webServer(), repository);
    server.start();
  }
}
