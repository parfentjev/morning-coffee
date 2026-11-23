package ee.fakeplastictrees.morningcoffee;

import static java.util.Optional.ofNullable;

import ee.fakeplastictrees.morningcoffee.brief.BriefServiceFactory;
import ee.fakeplastictrees.morningcoffee.brief.ScheduledBriefGenerator;
import ee.fakeplastictrees.morningcoffee.common.Config;
import ee.fakeplastictrees.morningcoffee.common.ConnectionPool;
import ee.fakeplastictrees.morningcoffee.feed.FeedService;
import ee.fakeplastictrees.morningcoffee.feed.ScheduledFeedReader;
import ee.fakeplastictrees.morningcoffee.webserver.WebServer;
import ee.fakeplastictrees.morningcoffee.webserver.handler.StaticHandler;
import ee.fakeplastictrees.morningcoffee.webserver.render.PebbleTemplateService;
import java.io.Closeable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/// Starts the Morning Coffee application.
public class App implements Closeable {
  private static final Logger logger = LogManager.getLogger();

  private WebServer server;
  private ScheduledFeedReader reader;
  private ScheduledBriefGenerator briefGenerator;
  private ConnectionPool pool;

  /// Starts application.
  static void main() {
    try {
      var app = new App();
      Runtime.getRuntime().addShutdownHook(new Thread(app::close));
      app.start();

    } catch (Exception e) {
      logger.error("unhandled exception", e);
      System.exit(1);
    }
  }

  /// Starts application services.
  ///
  /// @throws Exception if an application service cannot be initialized or started
  public void start() throws Exception {
    var config = Config.init();

    pool =
        new ConnectionPool(
            config.repository().postgresUrl(),
            config.repository().postgresUser(),
            config.repository().postgresPassword());

    var feedService = new FeedService(pool);
    reader = new ScheduledFeedReader(config.reader(), feedService);
    reader.start();

    var briefService = BriefServiceFactory.get(config.brief().service(), pool);
    briefGenerator = new ScheduledBriefGenerator(config.brief().schedule(), briefService);
    briefGenerator.start();

    var templateService = new PebbleTemplateService();
    var staticHandler = new StaticHandler();

    server =
        new WebServer(
            config.webServer(), feedService, briefService, templateService, staticHandler);
    server.start();
  }

  @Override
  public void close() {
    ofNullable(server).ifPresent(WebServer::close);
    ofNullable(briefGenerator).ifPresent(ScheduledBriefGenerator::close);
    ofNullable(reader).ifPresent(ScheduledFeedReader::close);
    ofNullable(pool).ifPresent(ConnectionPool::close);
  }
}
