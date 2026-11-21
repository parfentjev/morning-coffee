package ee.fakeplastictrees.morningcoffee.webserver.render;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MarkdownRendererTest {
  private final MarkdownRenderer renderer = new MarkdownRenderer();

  @Test
  void rendersMarkdownAndEscapesInlineAndBlockHtml() {
    var html = renderer.render("**Brief** with <em>markup</em>\n\n<script>example</script>");

    assertThat(html)
        .contains("<strong>Brief</strong>", "&lt;em&gt;markup&lt;/em&gt;")
        .contains("&lt;script&gt;example&lt;/script&gt;")
        .doesNotContain("<em>", "<script>");
  }

  @ParameterizedTest
  @ValueSource(strings = {"http://example.org/a", "https://example.org/b", "HTTPS://example.org"})
  void permitsAbsoluteWebLinksAndImages(String url) {
    var html = renderer.render("[link](%s) ![image](%s)".formatted(url, url));

    assertThat(html).contains("href=\"" + url + "\"", "src=\"" + url + "\"");
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "javascript:example", "data:text/plain,example", "mailto:a@example.org",
      "ftp://example.org", "//example.org", "/entry", "#section", "https:entry", "http://"
  })
  void omitsRejectedLinkAndImageDestinations(String url) {
    var html = renderer.render("[link](%s) ![image](%s)".formatted(url, url));

    assertThat(html).contains(">link</a>", "alt=\"image\"").doesNotContain("href=", "src=");
  }

  @Test
  void opensLinksInANewBrowsingContextWithoutAnOpener() {
    for (var markdown : new String[] {"[source](https://example.org)", "<https://example.org>"}) {
      assertThat(renderer.render(markdown))
          .contains("href=\"https://example.org\"", "target=\"_blank\"", "rel=\"noopener\"");
    }
  }
}
