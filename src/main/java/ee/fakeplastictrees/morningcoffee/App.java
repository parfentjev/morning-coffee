package ee.fakeplastictrees.morningcoffee;

import static java.util.Optional.ofNullable;

import ee.fakeplastictrees.morningcoffee.brief.BriefServiceFactory;
import ee.fakeplastictrees.morningcoffee.brief.ScheduledBriefGenerator;
import ee.fakeplastictrees.morningcoffee.common.repository.ConnectionPool;
import ee.fakeplastictrees.morningcoffee.reader.ScheduledFeedReader;
import ee.fakeplastictrees.morningcoffee.repository.Repository;
import ee.fakeplastictrees.morningcoffee.webserver.WebServer;
import ee.fakeplastictrees.morningcoffee.webserver.render.PebbleTemplateService;
import ee.fakeplastictrees.morningcoffee.webserver.render.StaticResourceService;
import java.io.Closeable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/// Starts the Morning Coffee application.
public class App implements Closeable {
  private static final Logger logger = LogManager.getLogger();

  private WebServer server;
  private ScheduledFeedReader reader;
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

    // TODO: move this repo into the reader package
    var repository = new Repository(pool);
    reader = new ScheduledFeedReader(config.reader(), repository);
    reader.start();

    // TODO: probably the service itself doesn't need to be initialised here, it can have
    // class-level visibility
    var briefService = BriefServiceFactory.get(config.brief(), pool);
    var briefGenerator = new ScheduledBriefGenerator();
    var templateService = new PebbleTemplateService();
    var resourceService = StaticResourceService.init();

    server =
        new WebServer(
            config.webServer(), repository, briefService, templateService, resourceService);
    server.start();
  }

  @Override
  public void close() {
    ofNullable(server).ifPresent(WebServer::close);
    ofNullable(reader).ifPresent(ScheduledFeedReader::close);
    ofNullable(pool).ifPresent(ConnectionPool::close);
  }
}
