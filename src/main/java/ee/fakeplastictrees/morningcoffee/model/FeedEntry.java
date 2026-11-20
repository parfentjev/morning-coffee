package ee.fakeplastictrees.morningcoffee.model;

import static java.util.Objects.requireNonNull;

import java.time.OffsetDateTime;
import java.util.UUID;

/// Represents a feed entry fetched for persistence. Body preserves untrusted publisher markup; both
/// body and reported content type may be null.
public record FeedEntry(
    String externalId,
    OffsetDateTime publishedAt,
    UUID feedId,
    String title,
    String link,
    String body,
    String bodyContentType) {
  public FeedEntry {
    requireNonNull(externalId);
    requireNonNull(publishedAt);
    requireNonNull(feedId);
    requireNonNull(title);
    requireNonNull(link);

    if (body == null && bodyContentType != null) {
      throw new IllegalArgumentException("body content type requires a body");
    }
  }
}
