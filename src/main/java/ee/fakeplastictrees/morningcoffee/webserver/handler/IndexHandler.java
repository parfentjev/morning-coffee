package ee.fakeplastictrees.morningcoffee.webserver.handler;

import ee.fakeplastictrees.morningcoffee.Config;
import ee.fakeplastictrees.morningcoffee.model.FeedEntry;
import ee.fakeplastictrees.morningcoffee.repository.Repository;
import ee.fakeplastictrees.morningcoffee.webserver.render.TemplateData;
import ee.fakeplastictrees.morningcoffee.webserver.render.TemplateException;
import ee.fakeplastictrees.morningcoffee.webserver.render.TemplateService;
import java.net.HttpURLConnection;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

class IndexHandler extends AbstractHttpHandler {
  private static final ZoneOffset TIME_OFFSET = ZoneOffset.UTC;
  private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ISO_INSTANT;

  private final Config.WebServer config;
  private final TemplateService templateService;
  private final Repository repository;

  public IndexHandler(
      Config.WebServer config, TemplateService templateService, Repository repository) {
    this.config = config;
    this.templateService = templateService;
    this.repository = repository;
  }

  @Override
  protected Response response() throws Exception {
    var entries = new TemplateData("entries", buildEntries());
    var body = templateService.getIndexTemplate().toHtml(entries);

    return Response.of(body, HttpURLConnection.HTTP_OK);
  }

  @Override
  protected String requestMethod() {
    return "GET";
  }

  @Override
  protected String requestPath() {
    return "/";
  }

  private String buildEntries() throws TemplateException {
    var output = new StringBuilder();
    var entriesNumber = config.entriesPerPage();
    for (var entry : repository.getEntries(entriesNumber)) {
      buildEntry(output, entry);
    }

    return output.toString();
  }

  private void buildEntry(StringBuilder output, FeedEntry entry) throws TemplateException {
    var template = templateService.getFeedEntryTemplate();
    var publishedAt = entry.publishedAt().atOffset(TIME_OFFSET).format(TIME_FORMATTER);

    var entryDate = new TemplateData("entry.date", publishedAt);
    var feedName = new TemplateData("feed.name", entry.feedId().toString());
    var entryLink = new TemplateData("entry.link", entry.link());
    var entryTitle = new TemplateData("entry.title", entry.title());

    var entryHtml = template.toHtml(entryDate, feedName, entryLink, entryTitle);
    output.append(entryHtml);
  }
}
