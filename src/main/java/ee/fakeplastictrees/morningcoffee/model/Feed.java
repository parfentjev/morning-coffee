package ee.fakeplastictrees.morningcoffee.model;

import static java.util.Objects.requireNonNull;

import java.time.Duration;
import java.util.UUID;

/// Identifies a configured feed.
///
/// @param id feed identifier
/// @param url feed URL
/// @param requestTimeout timeout for the HTTP request
public record Feed(UUID id, String url, Duration requestTimeout) {
  public Feed {
    requireNonNull(id);
    requireNonNull(url);
    requireNonNull(requestTimeout);
  }
}
