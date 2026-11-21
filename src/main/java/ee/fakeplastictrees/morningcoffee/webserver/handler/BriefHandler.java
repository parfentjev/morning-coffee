package ee.fakeplastictrees.morningcoffee.webserver.handler;

import com.sun.net.httpserver.HttpExchange;
import ee.fakeplastictrees.morningcoffee.brief.BriefService;
import ee.fakeplastictrees.morningcoffee.util.DateFormat;
import ee.fakeplastictrees.morningcoffee.webserver.render.MarkdownRenderer;
import ee.fakeplastictrees.morningcoffee.webserver.render.TemplateService;
import java.net.HttpURLConnection;
import java.util.Map;

/// Serves the latest brief.
class BriefHandler extends AbstractHttpHandler {
  private final MarkdownRenderer renderer = new MarkdownRenderer();
  private final BriefService service;
  private final TemplateService template;

  /// Creates a brief handler.
  ///
  /// @param service service providing the latest brief
  /// @param template service that manages HTML templates
  public BriefHandler(BriefService service, TemplateService template) {
    this.service = service;
    this.template = template;
  }

  @Override
  protected Response response(HttpExchange exchange) throws Exception {
    // TODO: handle the empty case
    var brief = service.latest().get();
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
