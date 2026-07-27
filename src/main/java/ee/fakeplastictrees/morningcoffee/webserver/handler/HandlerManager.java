package ee.fakeplastictrees.morningcoffee.webserver.handler;

import com.sun.net.httpserver.HttpServer;
import ee.fakeplastictrees.morningcoffee.repository.Repository;
import ee.fakeplastictrees.morningcoffee.webserver.render.TemplateException;

public class HandlerManager {
  /// Registers HTTP handlers with the given [HttpServer].
  ///
  /// @param server server that processes user requests
  /// @param repository repository used by handlers to retrieve data necessary for page rendering
  /// @throws TemplateException if a handler template cannot be loaded
  public static void registerHandlers(HttpServer server, Repository repository)
      throws TemplateException {
    // todo: this actually matches any path (and method), do I need some router that decides which
    // handler should handle a given request? probably
    server.createContext("/", IndexHandler.load(repository));
  }
}
