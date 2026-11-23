package ee.fakeplastictrees.morningcoffee.feed;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FeedParserTest {
  @Test
  void rssContentTakesPrecedenceOverDescriptionAndMedia() throws Exception {
    var entry =
        parse(
            false,
            """
            <description>teaser</description>
            <content:encoded><![CDATA[<p>Full &amp; original</p>]]></content:encoded>
            <media:content url="https://example.com/image.png" medium="image"/>
            """);
    assertThat(entry.body()).isEqualTo("<p>Full &#38; original</p>");
    assertThat(entry.bodyContentType()).isEqualTo("html");
  }

  @Test
  void rssDescriptionAndAbsentBody() throws Exception {
    assertThat(parse(false, "<description>summary</description>").body()).isEqualTo("summary");
    assertThat(parse(false, "").body()).isNull();
  }

  @Test
  void atomContentAndSummary() throws Exception {
    var entry =
        parse(
            true,
            "<content type=\"html\">&lt;p&gt;body&lt;/p&gt;</content>"
                + "<summary>teaser</summary>");
    assertThat(entry.body()).isEqualTo("<p>body</p>");
    assertThat(entry.bodyContentType()).isEqualTo("html");

    var summary = "A substantial summary. ".repeat(100);
    assertThat(parse(true, "<summary>" + summary + "</summary>").body()).isEqualTo(summary);
    assertThat(parse(true, "").body()).isNull();
  }

  @Test
  void atomExternalContentFallsBackAndXhtmlPreservesMarkup() throws Exception {
    assertThat(
            parse(
                    true,
                    "<content src=\"https://example.invalid/body\"/>"
                        + "<summary>fallback</summary>")
                .body())
        .isEqualTo("fallback");

    var entry =
        parse(
            true,
            """
            <content type="xhtml"><div xmlns="http://www.w3.org/1999/xhtml">
            <p>Original <b>markup</b></p></div></content>
            """);
    assertThat(entry.body()).contains("Original", "<b>markup</b>");
    assertThat(entry.bodyContentType()).isEqualTo("xhtml");
  }

  private FeedEntry parse(boolean atom, String fields)
      throws Exception {
    var xml =
        atom
            ? """
            <feed xmlns="http://www.w3.org/2005/Atom"><title>Feed</title>
            <id>urn:feed</id><updated>2025-01-01T00:00:00Z</updated><entry>
            <id>urn:entry</id><title>Entry</title><link href="https://example.com/"/>
            <updated>2025-01-01T00:00:00Z</updated>%s</entry></feed>
            """
                .formatted(fields)
            : """
            <rss version="2.0" xmlns:content="http://purl.org/rss/1.0/modules/content/"
            xmlns:media="http://search.yahoo.com/mrss/"><channel>
            <title>Feed</title><link>https://example.com/</link><description>Feed</description>
            <item><title>Entry</title><link>https://example.com/</link>%s</item></channel></rss>
            """
                .formatted(fields);

    var entries =
        new FeedParser().parseResponse(UUID.randomUUID(), xml.getBytes(StandardCharsets.UTF_8));
    assertThat(entries).hasSize(1);

    return entries.getFirst();
  }
}
