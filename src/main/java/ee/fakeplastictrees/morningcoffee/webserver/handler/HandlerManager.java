package ee.fakeplastictrees.morningcoffee.webserver.handler;

import com.sun.net.httpserver.HttpServer;
import ee.fakeplastictrees.morningcoffee.Config;
import ee.fakeplastictrees.morningcoffee.repository.Repository;
import ee.fakeplastictrees.morningcoffee.webserver.render.TemplateService;

public class HandlerManager {
  /// Registers HTTP handlers with the given [HttpServer].
  ///
  /// @param server server that processes user requests
  /// @param config web server configuration
  /// @param template service that manages HTML templates
  /// @param repository repository used by handlers to retrieve data necessary for page rendering
  public static void registerHandlers(
      Config.WebServer config,
      HttpServer server,
      TemplateService templateService,
      Repository repository) {
    server.createContext("/", new IndexHandler(config, templateService, repository));
  }
}
