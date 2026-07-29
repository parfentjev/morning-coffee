package ee.fakeplastictrees.morningcoffee.model;

import static java.util.Objects.requireNonNull;

import java.time.Instant;

public record FeedEntryDto(
    String externalId, Instant publishedAt, String title, String link, String feedName) {
  public FeedEntryDto {
    requireNonNull(externalId);
    requireNonNull(publishedAt);
    requireNonNull(title);
    requireNonNull(link);
    requireNonNull(feedName);

    if (linkInvalid(link)) {
      var message = "link must start with either http or https: %s".formatted(link);
      throw new IllegalArgumentException(message);
    }
  }

  private boolean linkInvalid(String link) {
    // This part is a bit verbose, but I find it easier to read than a one-liner due to inverse
    // checks.
    link = link.toLowerCase();
    if (link.startsWith("http://")) {
      return false;
    } else if (link.startsWith("https://")) {
      return false;
    }

    return true;
  }
}
