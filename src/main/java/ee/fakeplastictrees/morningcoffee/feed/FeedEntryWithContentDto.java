package ee.fakeplastictrees.morningcoffee.feed;

import java.time.OffsetDateTime;
import java.util.UUID;

/// A feed entry and its optional stored content.
public record FeedEntryWithContentDto(
    UUID id,
    String feedName,
    String title,
    String link,
    OffsetDateTime publishedAt,
    String body,
    String bodyContentType) {}
