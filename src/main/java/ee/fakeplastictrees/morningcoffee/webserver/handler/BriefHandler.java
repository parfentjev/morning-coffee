package ee.fakeplastictrees.morningcoffee.webserver.handler;

import com.sun.net.httpserver.HttpExchange;
import ee.fakeplastictrees.morningcoffee.webserver.render.TemplateService;
import java.net.HttpURLConnection;
import java.util.Map;

/// Serves the page containing recent feed entries.
class BriefHandler extends AbstractHttpHandler {
  private final TemplateService templates;

  /// Creates an index handler.
  ///
  /// @param config web server configuration
  /// @param repository feed repository
  /// @param templates service that manages HTML templates
  public BriefHandler(TemplateService templates) {
    this.templates = templates;
  }

  @Override
  protected Response response(HttpExchange exchange) throws Exception {
    var content = Map.<String, Object>of("brief", "brief placeholder");
    var body = templates.render("brief.html", content);

    return Response.of(CONTENT_TYPE_TEXT_HTML, body, HttpURLConnection.HTTP_OK);
  }

  @Override
  protected String requestMethod() {
    return "GET";
  }
}
