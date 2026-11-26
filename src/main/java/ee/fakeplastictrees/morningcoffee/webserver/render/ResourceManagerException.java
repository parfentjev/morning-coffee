package ee.fakeplastictrees.morningcoffee.webserver.render;

import java.io.IOException;
import java.io.Serial;

/// Signals a failure to load a classpath resource.
class ResourceManagerException extends IOException {
  @Serial private static final long serialVersionUID = 1L;

  /// Creates an exception with an error message.
  ///
  /// @param message error message
  ResourceManagerException(String message) {
    super(message);
  }

  /// Creates an exception with an error message and cause.
  ///
  /// @param message error message
  /// @param e underlying cause
  ResourceManagerException(String message, Exception e) {
    super(message, e);
  }
}
