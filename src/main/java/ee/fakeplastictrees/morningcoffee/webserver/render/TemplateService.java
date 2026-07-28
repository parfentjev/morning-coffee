package ee.fakeplastictrees.morningcoffee.webserver.render;

public class TemplateService {
  private final IndexTemplate indexTemplate;
  private final FeedEntryTemplate feedEntryTemplate;

  private TemplateService(IndexTemplate indexTemplate, FeedEntryTemplate feedEntryTemplate) {
    this.indexTemplate = indexTemplate;
    this.feedEntryTemplate = feedEntryTemplate;
  }

  public static TemplateService init() throws TemplateException {
    var indexTemplate = new IndexTemplate();
    var feedEntryTemplate = new FeedEntryTemplate();

    return new TemplateService(indexTemplate, feedEntryTemplate);
  }

  public IndexTemplate getIndexTemplate() {
    return indexTemplate;
  }

  public FeedEntryTemplate getFeedEntryTemplate() {
    return feedEntryTemplate;
  }
}
