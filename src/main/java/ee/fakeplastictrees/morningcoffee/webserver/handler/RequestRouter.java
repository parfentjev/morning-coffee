package ee.fakeplastictrees.morningcoffee.webserver.handler;

import com.sun.net.httpserver.HttpServer;
import ee.fakeplastictrees.morningcoffee.Config;
import ee.fakeplastictrees.morningcoffee.brief.BriefService;
import ee.fakeplastictrees.morningcoffee.repository.Repository;
import ee.fakeplastictrees.morningcoffee.webserver.render.StaticResourceService;
import ee.fakeplastictrees.morningcoffee.webserver.render.TemplateService;

/// Registers application HTTP handlers.
public class RequestRouter {
  /// Registers HTTP handlers with the given [HttpServer].
  ///
  /// @param config web server configuration
  /// @param repository repository used by handlers to retrieve data necessary for page rendering
  /// @param server server that processes user requests
  /// @param briefService service used by the brief handler to retrieve published briefs
  /// @param templateService service that manages HTML templates
  /// @param staticResourceService service that manages static resources
  public static void registerHandlers(
      Config.WebServer config,
      Repository repository,
      HttpServer server,
      BriefService briefService,
      TemplateService templateService,
      StaticResourceService staticResourceService) {
    server.createContext("/", new IndexHandler(config, repository, templateService));
    server.createContext("/brief", new BriefHandler(config, briefService, templateService));
    server.createContext("/static", new StaticHandler(staticResourceService));
  }
}
