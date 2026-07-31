package ee.fakeplastictrees.morningcoffee.model;

import java.time.OffsetDateTime;

/// Represents a feed entry prepared for display.
///
/// @param feedName feed display name
/// @param title entry title
/// @param link entry URL
/// @param publishedAt entry publication time
public record FeedEntryDto(
    String feedName, String title, String link, OffsetDateTime publishedAt) {}
