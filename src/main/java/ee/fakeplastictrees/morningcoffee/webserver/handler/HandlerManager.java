package ee.fakeplastictrees.morningcoffee.webserver.handler;

import com.sun.net.httpserver.HttpServer;
import ee.fakeplastictrees.morningcoffee.Config;
import ee.fakeplastictrees.morningcoffee.repository.Repository;
import ee.fakeplastictrees.morningcoffee.webserver.render.StaticResourceService;
import ee.fakeplastictrees.morningcoffee.webserver.render.TemplateService;

/// Registers application HTTP handlers.
public class HandlerManager {
  /// Registers HTTP handlers with the given [HttpServer].
  ///
  /// @param config web server configuration
  /// @param repository repository used by handlers to retrieve data necessary for page rendering
  /// @param server server that processes user requests
  /// @param templateService service that manages HTML templates
  /// @param staticResourceService service that manages static resources
  public static void registerHandlers(
      Config.WebServer config,
      Repository repository,
      HttpServer server,
      TemplateService templateService,
      StaticResourceService staticResourceService) {
    server.createContext("/", new IndexHandler(config, repository, templateService));
    server.createContext("/static", new StaticHandler(staticResourceService));
  }
}
