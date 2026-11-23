package ee.fakeplastictrees.morningcoffee.webserver.handler;

import com.sun.net.httpserver.HttpExchange;
import ee.fakeplastictrees.morningcoffee.brief.BriefService;
import ee.fakeplastictrees.morningcoffee.common.Config;
import ee.fakeplastictrees.morningcoffee.webserver.render.MarkdownRenderer;
import ee.fakeplastictrees.morningcoffee.webserver.render.TemplateService;
import java.net.HttpURLConnection;
import java.util.Map;

/// Serves the latest brief.
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
    var briefs = service.latest(briefsPerPage);
    if (briefs.isEmpty()) {
      // TODO: handle empty case
    }

    // TODO: so far it assumes only 1 brief is returned, handle multiple
    var brief = briefs.getFirst();
    var date = brief.publishedAt().format(DateFormat.SHORT);
    var bodyHtml = renderer.render(brief.body());
    var content = Map.<String, Object>of("date", date, "bodyHtml", bodyHtml);

    var response = template.render("brief.html", content);

    return Response.of(CONTENT_TYPE_TEXT_HTML, response, HttpURLConnection.HTTP_OK);
  }

  @Override
  protected String requestMethod() {
    return "GET";
  }
}
