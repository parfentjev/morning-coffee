package ee.fakeplastictrees.morningcoffee.webserver.handler;

import com.sun.net.httpserver.HttpExchange;
import ee.fakeplastictrees.morningcoffee.common.Config;
import ee.fakeplastictrees.morningcoffee.common.RepositoryException;
import ee.fakeplastictrees.morningcoffee.feed.FeedEntryDto;
import ee.fakeplastictrees.morningcoffee.feed.FeedService;
import ee.fakeplastictrees.morningcoffee.webserver.render.TemplateService;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// Serves the page containing recent feed entries.
class IndexHandler extends AbstractHttpHandler {
  private final Config.WebServer config;
  private final TemplateService templates;
  private final FeedService feedService;

  /// Creates an index handler.
  ///
  /// @param config web server configuration
  /// @param feedService service providing feed entries
  /// @param templates service that manages HTML templates
  public IndexHandler(Config.WebServer config, FeedService feedService, TemplateService templates) {
    this.config = config;
    this.feedService = feedService;
    this.templates = templates;
  }

  @Override
  protected Response response(HttpExchange exchange) throws Exception {
    var entries = Map.<String, Object>of("entries", buildEntries());
    var body = templates.render("index.html", entries);

    return Response.of(CONTENT_TYPE_TEXT_HTML, body, HttpURLConnection.HTTP_OK);
  }

  @Override
  protected String requestMethod() {
    return "GET";
  }

  private List<Map<String, Object>> buildEntries() throws RepositoryException {
    var entries = new ArrayList<Map<String, Object>>();
    for (var entry : feedService.getEntries(config.entriesPerPage())) {
      entries.add(buildEntry(entry));
    }

    return entries;
  }

  private Map<String, Object> buildEntry(FeedEntryDto entry) {
    return Map.of(
        "dateFull", entry.publishedAt().format(DateFormat.FULL),
        "dateShort", entry.publishedAt().format(DateFormat.SHORT),
        "feedName", entry.feedName(),
        "link", entry.link(),
        "title", entry.title());
  }
}
