package ee.fakeplastictrees.morningcoffee.model;

import java.time.Instant;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/// An entry published in an RSS or Atom feed. All properties are guaranteed to be non-null.
/// Values displayed publicly must be treated as untrusted and encoded for their output context.
///
/// @param id unique identificator
/// @param publishedAt publication date as reported by the publisher
/// @param externalId unique identifier given by the publisher; if missing, `link` is used as the
/// fallback value
/// @param feedId unique identificator of a feed that this entry was published on
/// @param title entry title
/// @param link web link to this entry; guaranteed to start with either `http://` or `https://`
public record FeedEntry(
    UUID id, Instant publishedAt, String externalId, UUID feedId, String title, String link) {
  /// @throws NullPointerException if any of the values is `null`
  public FeedEntry {
    // todo: need to rethink this
    // reader creates this record, but it cannot know its ID until
    // repository writes to a database
    requireNonNull(id);
    requireNonNull(publishedAt);
    requireNonNull(externalId);
    requireNonNull(feedId);
    requireNonNull(title);
    requireNonNull(link);

    if (linkInvalid(link)) {
      var message = "link must start with either http or https: %s".formatted(link);
      throw new IllegalArgumentException(message);
    }
  }

  private boolean linkInvalid(String link) {
    // Reverse boolean checks aren't nice, but God wouldn't send them upon us if we couldn't deal
    // with them. Amen.
    link = link.toLowerCase();

    return !(link.startsWith("http://") || link.startsWith("https://"));
  }
}
