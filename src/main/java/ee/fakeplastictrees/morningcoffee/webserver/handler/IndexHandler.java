package ee.fakeplastictrees.morningcoffee.webserver.handler;

import com.sun.net.httpserver.HttpExchange;
import ee.fakeplastictrees.morningcoffee.Config;
import ee.fakeplastictrees.morningcoffee.model.FeedEntryDto;
import ee.fakeplastictrees.morningcoffee.repository.Repository;
import ee.fakeplastictrees.morningcoffee.repository.RepositoryException;
import ee.fakeplastictrees.morningcoffee.webserver.render.TemplateData;
import ee.fakeplastictrees.morningcoffee.webserver.render.TemplateException;
import ee.fakeplastictrees.morningcoffee.webserver.render.TemplateService;
import java.net.HttpURLConnection;
import java.time.format.DateTimeFormatter;

/// Serves the page containing recent feed entries.
class IndexHandler extends AbstractHttpHandler {
  private static final DateTimeFormatter TIME_FORMATTER =
      DateTimeFormatter.ofPattern("MMM dd HH:mm");

  private final Config.WebServer config;
  private final TemplateService templateService;
  private final Repository repository;

  /// Creates an index handler.
  ///
  /// @param config web server configuration
  /// @param repository feed repository
  /// @param templateService service that manages HTML templates
  public IndexHandler(
      Config.WebServer config, Repository repository, TemplateService templateService) {
    this.config = config;
    this.repository = repository;
    this.templateService = templateService;
  }

  @Override
  protected Response response(HttpExchange exchange) throws Exception {
    var entries = new TemplateData("entries", buildEntries());
    var body = templateService.getIndexTemplate().toHtml(entries);

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

  private String buildEntries() throws RepositoryException, TemplateException {
    var output = new StringBuilder();
    var entriesNumber = config.entriesPerPage();
    for (var entry : repository.getEntries(entriesNumber)) {
      buildEntry(output, entry);
    }

    return output.toString();
  }

  private void buildEntry(StringBuilder output, FeedEntryDto entry) throws TemplateException {
    var template = templateService.getFeedEntryTemplate();
    var publishedAt = entry.publishedAt().format(TIME_FORMATTER);

    var entryDate = new TemplateData("entry.date", publishedAt);
    var feedName = new TemplateData("feed.name", entry.feedName());
    var entryLink = new TemplateData("entry.link", entry.link());
    var entryTitle = new TemplateData("entry.title", entry.title());

    var entryHtml = template.toHtml(entryDate, feedName, entryLink, entryTitle);
    output.append(entryHtml);
  }
}
