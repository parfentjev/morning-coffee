package ee.fakeplastictrees.morningcoffee.webserver.handler;

import com.sun.net.httpserver.HttpExchange;
import ee.fakeplastictrees.morningcoffee.webserver.render.StaticResourceService;
import java.io.IOException;
import java.util.Map;

/// Serves static resources under the `/static/` request path.
public class StaticHandler extends AbstractHttpHandler {
  private static final String REQUEST_PATH_PREFIX = "/static/";
  private final Map<String, Response> resources;

  /// Loads the responses served by this handler.
  public StaticHandler() throws IOException {
    resources = StaticResourceService.init((type, body) -> Response.of(type, body, 200));
  }

  @Override
  protected String requestMethod() {
    return "GET";
  }

  @Override
  protected Response response(HttpExchange exchange) throws Exception {
    var relativePath = exchange.getRequestURI().getPath().substring(REQUEST_PATH_PREFIX.length());

    var response = resources.get(relativePath);
    return response == null ? Response.notFound() : response;
  }
}
