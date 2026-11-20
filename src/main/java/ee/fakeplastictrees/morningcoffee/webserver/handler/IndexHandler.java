package ee.fakeplastictrees.morningcoffee.webserver.handler;

import com.sun.net.httpserver.HttpExchange;
import ee.fakeplastictrees.morningcoffee.Config;
import ee.fakeplastictrees.morningcoffee.model.FeedEntryDto;
import ee.fakeplastictrees.morningcoffee.repository.Repository;
import ee.fakeplastictrees.morningcoffee.repository.RepositoryException;
import ee.fakeplastictrees.morningcoffee.webserver.render.TemplateService;
import java.net.HttpURLConnection;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// Serves the page containing recent feed entries.
class IndexHandler extends AbstractHttpHandler {
  private static final DateTimeFormatter DATE_FULL = DateTimeFormatter.ISO_INSTANT;
  private static final DateTimeFormatter DATE_SHORT =
      DateTimeFormatter.ofPattern("MMM dd HH:mm").withZone(ZoneOffset.UTC);

  private final Config.WebServer config;
  private final TemplateService templates;
  private final Repository repository;

  /// Creates an index handler.
  ///
  /// @param config web server configuration
  /// @param repository feed repository
  /// @param templates service that manages HTML templates
  public IndexHandler(Config.WebServer config, Repository repository, TemplateService templates) {
    this.config = config;
    this.repository = repository;
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

  @Override
  protected String requestPath() {
    return "/";
  }

  private List<Map<String, Object>> buildEntries() throws RepositoryException {
    var entries = new ArrayList<Map<String, Object>>();
    for (var entry : repository.getEntries(config.entriesPerPage())) {
      entries.add(buildEntry(entry));
    }

    return entries;
  }

  private Map<String, Object> buildEntry(FeedEntryDto entry) {
    return Map.of(
        "dateFull", entry.publishedAt().format(DATE_FULL),
        "dateShort", entry.publishedAt().format(DATE_SHORT),
        "feedName", entry.feedName(),
        "link", entry.link(),
        "title", entry.title());
  }
}
