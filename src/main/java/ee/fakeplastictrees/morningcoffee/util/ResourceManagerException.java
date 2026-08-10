package ee.fakeplastictrees.morningcoffee.util;

import java.io.Serial;

/// Signals a failure to load a classpath resource.
public class ResourceManagerException extends Exception {
  @Serial private static final long serialVersionUID = 1L;

  /// Creates an exception with an error message.
  ///
  /// @param message error message
  public ResourceManagerException(String message) {
    super(message);
  }

  /// Creates an exception with an error message and cause.
  ///
  /// @param message error message
  /// @param e underlying cause
  public ResourceManagerException(String message, Exception e) {
    super(message, e);
  }
}
