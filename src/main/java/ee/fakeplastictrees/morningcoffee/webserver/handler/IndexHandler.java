package ee.fakeplastictrees.morningcoffee.webserver.handler;

import ee.fakeplastictrees.morningcoffee.repository.Repository;
import ee.fakeplastictrees.morningcoffee.webserver.render.FeedEntryTemplate;
import ee.fakeplastictrees.morningcoffee.webserver.render.IndexTemplate;
import ee.fakeplastictrees.morningcoffee.webserver.render.TemplateData;
import ee.fakeplastictrees.morningcoffee.webserver.render.TemplateException;

class IndexHandler extends AbstractHttpHandler {
  private final Repository repository;
  private final IndexTemplate indexTemplate;
  private final FeedEntryTemplate feedEntryTemplate;

  private IndexHandler(
      Repository repository, IndexTemplate indexTemplate, FeedEntryTemplate feedEntryTemplate) {
    this.repository = repository;
    this.indexTemplate = indexTemplate;
    this.feedEntryTemplate = feedEntryTemplate;
  }

  static IndexHandler load(Repository repository) throws TemplateException {
    var indexTemplate = new IndexTemplate();
    var feedEntryTemplate = new FeedEntryTemplate();

    return new IndexHandler(repository, indexTemplate, feedEntryTemplate);
  }

  @Override
  protected Response response() throws Exception {
    var entriesHtml = new StringBuilder();
    for (var entry : repository.getEntries(10)) {
      var feedName = new TemplateData("feed.name", "Some Feed");
      var entryLink = new TemplateData("entry.link", entry.link());
      var entryTitle = new TemplateData("entry.title", entry.title());

      var entryHtml = feedEntryTemplate.renderToHtml(feedName, entryLink, entryTitle);
      entriesHtml.append(entryHtml);
    }

    var entries = new TemplateData("entries", entriesHtml.toString());
    var body = indexTemplate.renderToHtml(entries);

    return Response.of(body, 200);
  }
}
