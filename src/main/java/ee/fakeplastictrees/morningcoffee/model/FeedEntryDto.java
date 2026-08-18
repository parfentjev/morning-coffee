package ee.fakeplastictrees.morningcoffee.model;

import static java.util.Objects.requireNonNull;

import java.time.OffsetDateTime;

/// Represents a feed entry prepared for display.
///
/// @param feedName feed display name
/// @param title entry title
/// @param link entry URL
/// @param publishedAt entry publication time
public record FeedEntryDto(String feedName, String title, String link, OffsetDateTime publishedAt) {
  public FeedEntryDto {
    requireNonNull(feedName);
    requireNonNull(title);
    requireNonNull(link);
    requireNonNull(publishedAt);
  }
}
