package ee.fakeplastictrees.morningcoffee.model;

import java.time.OffsetDateTime;

public record FeedEntryDto(
    String feedName, String title, String link, OffsetDateTime publishedAt) {}
