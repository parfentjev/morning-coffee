package ee.fakeplastictrees.morningcoffee.reader;

import com.rometools.rome.feed.synd.SyndEntry;
import ee.fakeplastictrees.morningcoffee.model.FeedEntry;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.UUID;

class FeedEntryMapper {
  @SuppressWarnings("HttpUrlsUsage")
  public static FeedEntry syndEntryToFeedEntry(UUID feedId, SyndEntry input)
      throws IllegalArgumentException {
    // The UI layer (webserver package) is supposed to validate stored data before rendering it,
    // but filtering out blatantly invalid feed entries here does no harm because there is no need
    // to store them in the database.
    var link =
        switch (input.getLink()) {
          case null -> throw new IllegalArgumentException("link is null");
          case String v when v.isBlank() -> throw new IllegalArgumentException("link is blank");
          case String v when v.startsWith("http://") -> v;
          case String v when v.startsWith("https://") -> v;
          default -> throw new IllegalArgumentException("link must start with 'http(s)://'");
        };

    var title =
        switch (input.getTitle()) {
          case null -> link;
          case String v when v.isBlank() -> link;
          case String v -> v;
        };

    var externalId =
        switch (input.getUri()) {
          case null -> link;
          case String v when v.isBlank() -> link;
          case String v -> v;
        };

    var publishedAt =
        switch (input.getPublishedDate()) {
          case null -> {
            var updatedDate = input.getUpdatedDate();
            if (updatedDate == null) {
              yield OffsetDateTime.now();
            }

            yield updatedDate.toInstant().atOffset(ZoneOffset.UTC);
          }
          case Date v -> v.toInstant().atOffset(ZoneOffset.UTC);
        };

    if (publishedAt.isAfter(OffsetDateTime.now())) {
      publishedAt = OffsetDateTime.now();
    }

    return new FeedEntry(externalId, publishedAt, feedId, title, link);
  }
}
