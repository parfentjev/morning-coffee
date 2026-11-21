package ee.fakeplastictrees.morningcoffee.brief;

import java.time.OffsetDateTime;

record Brief(OffsetDateTime publishedAt, String body) {}
