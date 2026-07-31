package ee.fakeplastictrees.morningcoffee.reader;

import com.rometools.rome.feed.synd.SyndEntry;
import ee.fakeplastictrees.morningcoffee.model.FeedEntry;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

class FeedEntryMapper {
  private static final ZoneOffset OFFSET = ZoneOffset.UTC;

  @SuppressWarnings("HttpUrlsUsage")
  public static FeedEntry syndEntryToFeedEntry(SyndEntry input) throws IllegalArgumentException {
    // The UI layer (webserver package) is supposed to validate stored data before rendering it,
    // but filtering out blatantly invalid feed entries here does no harm because there is no need
    // to store them in the database.
    var entry = new FeedEntry();

    var link = input.getLink();
    if (link == null || link.isBlank()) {
      throw new IllegalArgumentException("link can't be empty");
    } else if (!(link.startsWith("http://") || link.startsWith("https://"))) {
      throw new IllegalArgumentException("link must start with http/https: %s".formatted(link));
    } else {
      entry.setLink(link);
    }

    var title = input.getTitle();
    if (title != null && !title.isBlank()) {
      entry.setTitle(title);
    } else {
      entry.setTitle(link);
    }

    var externalId = input.getUri();
    if (externalId != null && !externalId.isBlank()) {
      entry.setExternalId(externalId);
    } else {
      entry.setExternalId(link);
    }

    if (input.getPublishedDate() != null) {
      var publishedAt = input.getPublishedDate().toInstant().atOffset(OFFSET);
      entry.setPublishedAt(publishedAt);
    } else if (input.getUpdatedDate() != null) {
      var publishedAt = input.getUpdatedDate().toInstant().atOffset(OFFSET);
      entry.setPublishedAt(publishedAt);
    } else {
      var publishedAt = OffsetDateTime.now();
      entry.setPublishedAt(publishedAt);
    }

    if (entry.getPublishedAt().isAfter(OffsetDateTime.now())) {
      entry.setPublishedAt(OffsetDateTime.now());
    }

    return entry;
  }
}
