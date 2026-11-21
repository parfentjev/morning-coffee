package ee.fakeplastictrees.morningcoffee.webserver.render;

import java.net.URI;
import java.net.URISyntaxException;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.commonmark.renderer.html.UrlSanitizer;

/// Renders Markdown as an HTML fragment for insertion into page content.
public final class MarkdownRenderer {
  private final Parser parser = Parser.builder().build();

  private final HtmlRenderer renderer =
      HtmlRenderer.builder()
          .escapeHtml(true)
          .sanitizeUrls(true)
          .urlSanitizer(new WebUrlSanitizer())
          .attributeProviderFactory(
              context ->
                  (node, tagName, attributes) -> {
                    attributes.remove("href", "");
                    attributes.remove("src", "");
                    if ("a".equals(tagName)) {
                      attributes.put("target", "_blank");
                      attributes.put("rel", "noopener");
                    }
                  })
          .build();

  /// Converts Markdown to HTML.
  public String render(String markdown) {
    return renderer.render(parser.parse(markdown));
  }

  private static final class WebUrlSanitizer implements UrlSanitizer {
    @Override
    public String sanitizeLinkUrl(String url) {
      return sanitize(url);
    }

    @Override
    public String sanitizeImageUrl(String url) {
      return sanitize(url);
    }

    private static String sanitize(String url) {
      try {
        var uri = new URI(url);
        var scheme = uri.getScheme();
        if (("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme))
            && uri.getHost() != null) {
          return url;
        }
      } catch (URISyntaxException ignored) {
        // Malformed destinations are omitted along with disallowed schemes.
      }
      return "";
    }
  }
}
