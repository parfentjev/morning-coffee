package ee.fakeplastictrees.morningcoffee;

import static java.util.Optional.ofNullable;

import ee.fakeplastictrees.morningcoffee.brief.BriefServiceFactory;
import ee.fakeplastictrees.morningcoffee.brief.BriefServiceFactory.Provider;
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

  private Repository repository;
  private ScheduledFeedReader reader;
  private WebServer server;

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
    repository = new Repository(config.repository());

    reader = new ScheduledFeedReader(config.reader(), repository);
    reader.start();

    var briefService = BriefServiceFactory.get(Provider.DUMMY);
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
    ofNullable(repository).ifPresent(Repository::close);
  }
}
