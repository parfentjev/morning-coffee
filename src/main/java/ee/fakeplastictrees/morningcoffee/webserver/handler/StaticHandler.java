package ee.fakeplastictrees.morningcoffee.webserver.handler;

import com.sun.net.httpserver.HttpExchange;
import ee.fakeplastictrees.morningcoffee.webserver.render.StaticResourceService;

/// Serves static resources under the `/static/` request path.
public class StaticHandler extends AbstractHttpHandler {
  private static final String REQUEST_PATH_PREFIX = "/static/";
  private final StaticResourceService staticResourceService;

  /// Creates a static resource handler.
  ///
  /// @param staticResourceService service that manages static resources
  public StaticHandler(StaticResourceService staticResourceService) {
    this.staticResourceService = staticResourceService;
  }

  @Override
  protected String requestMethod() {
    return "GET";
  }

  @Override
  protected String requestPath() {
    return "%s.+".formatted(REQUEST_PATH_PREFIX);
  }

  @Override
  protected Response response(HttpExchange exchange) throws Exception {
    var relativePath = exchange.getRequestURI().getPath().substring(REQUEST_PATH_PREFIX.length());

    var resource = staticResourceService.getResource(relativePath);
    if (resource.isEmpty()) {
      return Response.notFound();
    }

    var contentType = resource.get().contentType();
    var body = resource.get().contents();

    return Response.of(contentType, body, 200);
  }
}
