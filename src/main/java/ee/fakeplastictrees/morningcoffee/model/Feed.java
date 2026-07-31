package ee.fakeplastictrees.morningcoffee.model;

import java.util.UUID;

/// Identifies a configured feed.
///
/// @param id feed identifier
/// @param url feed URL
public record Feed(UUID id, String url) {}
