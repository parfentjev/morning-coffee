package ee.fakeplastictrees.morningcoffee.reader;

import static ee.fakeplastictrees.morningcoffee.reader.FeedEntryMapper.syndEntryToFeedEntry;
import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndEntryImpl;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

public class FeedEntryMapperTest {
  private static final UUID FEED_ID = UUID.randomUUID();

  @ParameterizedTest
  @ValueSource(strings = {"http", "https"})
  public void inputWithAllPropsAccepted(String protocol) {
    var input = syndEntry();
    input.setLink("%s://example.com/".formatted(protocol));

    var output = syndEntryToFeedEntry(FEED_ID, input);
    assertThat(output.feedId()).isEqualTo(FEED_ID);
    assertThat(output.link()).isEqualTo(input.getLink());
    assertThat(output.title()).isEqualTo(input.getTitle());
    assertThat(output.externalId()).isEqualTo(input.getUri());
    assertThat(output.publishedAt().toInstant()).isEqualTo(input.getPublishedDate().toInstant());
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"", " ", "file://"})
  public void inputWithInvalidLinkRejected(String link) {
    var input = syndEntry();
    input.setLink(link);

    assertThrows(IllegalArgumentException.class, () -> syndEntryToFeedEntry(FEED_ID, input));
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"", " "})
  public void inputWithoutTitleUsesLink(String title) {
    var input = syndEntry();
    input.setTitle(title);

    var output = syndEntryToFeedEntry(FEED_ID, input);
    assertThat(output.title()).isEqualTo(input.getLink());
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"", " "})
  public void inputWithoutExternalIdUsesLink(String externalId) {
    var input = syndEntry();
    input.setUri(externalId);

    var output = syndEntryToFeedEntry(FEED_ID, input);
    assertThat(output.externalId()).isEqualTo(input.getLink());
  }

  @Test
  public void updatedDateUsedWhenPublishedDateMissing() {
    var input = syndEntry();
    input.setPublishedDate(null);
    input.setUpdatedDate(date(now().minusDays(1)));

    var output = syndEntryToFeedEntry(FEED_ID, input);
    assertThat(output.publishedAt().toInstant()).isEqualTo(input.getUpdatedDate().toInstant());
  }

  @Test
  public void currentDateUsedWhenPublishedAndUpdatedDatesMissing() {
    var input = syndEntry();
    input.setPublishedDate(null);
    input.setUpdatedDate(null);

    var before = now();
    var output = syndEntryToFeedEntry(FEED_ID, input);
    var after = now();

    assertThat(output.publishedAt().toInstant()).isBetween(before.toInstant(), after.toInstant());
  }

  @Test
  public void publishedDatePreferredOverUpdatedDate() {
    var input = syndEntry();
    input.setPublishedDate(date(now().minusDays(2)));
    input.setUpdatedDate(date(now().minusDays(1)));

    var output = syndEntryToFeedEntry(FEED_ID, input);
    assertThat(output.publishedAt().toInstant()).isEqualTo(input.getPublishedDate().toInstant());
  }

  @Test
  public void futurePublishedDateReplacedWithCurrentDate() {
    var input = syndEntry();
    input.setPublishedDate(date(now().plusDays(1)));

    var before = now();
    var output = syndEntryToFeedEntry(FEED_ID, input);
    var after = now();

    assertThat(output.publishedAt().toInstant()).isBetween(before.toInstant(), after.toInstant());
  }

  @Test
  public void futureUpdatedDateReplacedWithCurrentDate() {
    var input = syndEntry();
    input.setPublishedDate(null);
    input.setUpdatedDate(date(now().plusDays(1)));

    var before = now();
    var output = syndEntryToFeedEntry(FEED_ID, input);
    var after = now();

    assertThat(output.publishedAt().toInstant()).isBetween(before.toInstant(), after.toInstant());
  }

  private SyndEntry syndEntry() {
    var input = new SyndEntryFixture();
    input.setLink("https://example.com/");
    input.setTitle("some title");
    input.setUri("some externalId");
    input.setPublishedDate(date(now().minusDays(1)));

    return input;
  }

  private Date date(OffsetDateTime date) {
    return Date.from(date.toInstant());
  }

  // SyndEntryImpl throws NullPointerException in setUpdatedDate if the value is null. This fixture
  // extends SyndEntryImpl and overrides the setter to test null values. Other getters and setters
  // are overridden too to ensure that the test data isn't altered in any way by an external
  // library (ROME).
  private static final class SyndEntryFixture extends SyndEntryImpl {
    private String link;
    private String title;
    private String uri;
    private Date publishedDate;
    private Date updatedDate;

    @Override
    public String getLink() {
      return link;
    }

    @Override
    public void setLink(String link) {
      this.link = link;
    }

    @Override
    public String getTitle() {
      return title;
    }

    @Override
    public void setTitle(String title) {
      this.title = title;
    }

    @Override
    public String getUri() {
      return uri;
    }

    @Override
    public void setUri(String uri) {
      this.uri = uri;
    }

    @Override
    public Date getPublishedDate() {
      return publishedDate;
    }

    @Override
    public void setPublishedDate(Date publishedDate) {
      this.publishedDate = publishedDate;
    }

    @Override
    public Date getUpdatedDate() {
      return updatedDate;
    }

    @Override
    public void setUpdatedDate(Date updatedDate) {
      this.updatedDate = updatedDate;
    }
  }
}
