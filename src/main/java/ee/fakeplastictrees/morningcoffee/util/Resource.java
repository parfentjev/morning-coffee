package ee.fakeplastictrees.morningcoffee.util;

import java.nio.charset.StandardCharsets;

/// Holds a classpath resource's content type and contents.
///
/// @param contentType resource media type
/// @param contents resource bytes
public record Resource(String contentType, byte[] contents) {
  public Resource(String contentType, byte[] contents) {
    this.contentType = contentType;
    this.contents = contents.clone();
  }

  public byte[] contents() {
    return contents.clone();
  }

  /// Returns resource contents decoded as UTF-8.
  ///
  /// @return decoded resource contents
  @Override
  public String toString() {
    return new String(contents, StandardCharsets.UTF_8);
  }
}
