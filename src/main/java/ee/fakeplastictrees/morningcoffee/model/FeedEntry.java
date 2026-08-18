package ee.fakeplastictrees.morningcoffee.model;

import static java.util.Objects.requireNonNull;

import java.time.OffsetDateTime;
import java.util.UUID;

/// Represents a feed entry fetched for persistence.
public record FeedEntry(
    String externalId, OffsetDateTime publishedAt, UUID feedId, String title, String link) {
  public FeedEntry {
    requireNonNull(externalId);
    requireNonNull(publishedAt);
    requireNonNull(feedId);
    requireNonNull(title);
    requireNonNull(link);
  }
}
