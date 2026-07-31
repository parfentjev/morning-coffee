package ee.fakeplastictrees.morningcoffee.webserver.render;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

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
  /// @throws TemplateException if a template cannot be loaded
  public static TemplateService init() throws TemplateException {
    var indexTemplate = new IndexTemplate(loadFromResources("index.html"));
    var feedEntryTemplate = new FeedEntryTemplate(loadFromResources("feed_entry.html"));

    return new TemplateService(indexTemplate, feedEntryTemplate);
  }

  private static String loadFromResources(String templateFile) throws TemplateException {
    var resource = "templates/%s".formatted(templateFile);
    try (var stream = TemplateService.class.getClassLoader().getResourceAsStream(resource)) {
      if (stream == null) {
        var message = "failed to load template resource: %s".formatted(resource);
        throw new TemplateException(message);
      }

      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      var message = "failed to read template resource";
      throw new TemplateException(message, e);
    }
  }

  public IndexTemplate getIndexTemplate() {
    return indexTemplate;
  }

  public FeedEntryTemplate getFeedEntryTemplate() {
    return feedEntryTemplate;
  }
}
