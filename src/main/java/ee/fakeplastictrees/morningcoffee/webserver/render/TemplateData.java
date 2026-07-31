package ee.fakeplastictrees.morningcoffee.webserver.render;

/// Maps a template key to its replacement value.
///
/// @param key template key
/// @param value replacement value
public record TemplateData(String key, String value) {}
