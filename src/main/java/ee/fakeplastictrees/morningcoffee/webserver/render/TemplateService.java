package ee.fakeplastictrees.morningcoffee.webserver.render;

import ee.fakeplastictrees.morningcoffee.util.ResourceManager;
import ee.fakeplastictrees.morningcoffee.util.ResourceManagerException;

/// Loads and provides HTML templates.
public class TemplateService {
  private final IndexTemplate indexTemplate;
  private final FeedEntryTemplate feedEntryTemplate;

  private TemplateService(IndexTemplate indexTemplate, FeedEntryTemplate feedEntryTemplate) {
    this.indexTemplate = indexTemplate;
    this.feedEntryTemplate = feedEntryTemplate;
  }

  /// Loads application templates from resources.
  ///
  /// @return initialized template service
  /// @throws ResourceManagerException if a template resource cannot be loaded
  public static TemplateService init() throws ResourceManagerException {
    var indexTemplate = new IndexTemplate(loadFromResources("index.html"));
    var feedEntryTemplate = new FeedEntryTemplate(loadFromResources("feed_entry.html"));

    return new TemplateService(indexTemplate, feedEntryTemplate);
  }

  private static String loadFromResources(String templateFile) throws ResourceManagerException {
    var resource = "templates/%s".formatted(templateFile);

    return ResourceManager.loadResource(resource).toString();
  }

  /// Returns template for the index page.
  ///
  /// @return index-page template
  public IndexTemplate getIndexTemplate() {
    return indexTemplate;
  }

  /// Returns template for a feed entry.
  ///
  /// @return feed-entry template
  public FeedEntryTemplate getFeedEntryTemplate() {
    return feedEntryTemplate;
  }
}
