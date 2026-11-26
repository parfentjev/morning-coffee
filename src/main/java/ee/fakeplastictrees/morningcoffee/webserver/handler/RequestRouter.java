package ee.fakeplastictrees.morningcoffee.webserver.handler;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import ee.fakeplastictrees.morningcoffee.brief.BriefService;
import ee.fakeplastictrees.morningcoffee.common.Config;
import ee.fakeplastictrees.morningcoffee.feed.FeedService;
import ee.fakeplastictrees.morningcoffee.webserver.render.TemplateService;

/// Registers application HTTP handlers.
public class RequestRouter {
  /// Registers HTTP handlers with the given [HttpServer].
  ///
  /// @param config web server configuration
  /// @param feedService service providing feed entries
  /// @param server server that processes user requests
  /// @param briefService service used by the brief handler to retrieve published briefs
  /// @param templateService service that manages HTML templates
  /// @param staticHandler handler that serves static resources
  public static void registerHandlers(
      Config.WebServer config,
      FeedService feedService,
      HttpServer server,
      BriefService briefService,
      TemplateService templateService,
      HttpHandler staticHandler) {
    server.createContext("/", new IndexHandler(config, feedService, templateService));
    // server.createContext("/brief", new BriefHandler(config, briefService, templateService));
    server.createContext("/static", staticHandler);
  }
}
