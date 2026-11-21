package ee.fakeplastictrees.morningcoffee.brief;

import java.time.OffsetDateTime;

public record BriefDto(OffsetDateTime publishedAt, String body) {}
