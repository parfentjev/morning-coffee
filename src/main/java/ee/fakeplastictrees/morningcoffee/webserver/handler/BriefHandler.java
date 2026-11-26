package ee.fakeplastictrees.morningcoffee.webserver.handler;

import com.sun.net.httpserver.HttpExchange;
import ee.fakeplastictrees.morningcoffee.brief.BriefService;
import ee.fakeplastictrees.morningcoffee.common.Config;
import ee.fakeplastictrees.morningcoffee.webserver.render.MarkdownRenderer;
import ee.fakeplastictrees.morningcoffee.webserver.render.TemplateService;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Map;

/// Serves the latest briefs.
class BriefHandler extends AbstractHttpHandler {
  private final MarkdownRenderer renderer = new MarkdownRenderer();
  private final int briefsPerPage;
  private final BriefService service;
  private final TemplateService template;

  /// Creates a brief handler.
  ///
  /// @param config web server settings specifying how many briefs to request
  /// @param service service providing the latest published briefs
  /// @param template service that manages HTML templates
  public BriefHandler(Config.WebServer config, BriefService service, TemplateService template) {
    this.briefsPerPage = config.briefsPerPage();
    this.service = service;
    this.template = template;
  }

  @Override
  protected Response response(HttpExchange exchange) throws Exception {
    var briefs = new ArrayList<Map<String, String>>();
    for (var brief : service.latest(briefsPerPage)) {
      briefs.add(
          Map.of(
              "date", brief.publishedAt().format(DateFormat.SHORT),
              "bodyHtml", renderer.render(brief.body())));
    }

    Map<String, Object> content = briefs.isEmpty() ? Map.of() : Map.of("briefs", briefs);
    var response = template.render("briefs.html", content);

    return Response.of(CONTENT_TYPE_TEXT_HTML, response, HttpURLConnection.HTTP_OK);
  }

  @Override
  protected String requestMethod() {
    return "GET";
  }
}
