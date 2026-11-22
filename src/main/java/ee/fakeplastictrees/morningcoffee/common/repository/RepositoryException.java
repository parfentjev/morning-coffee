package ee.fakeplastictrees.morningcoffee.common.repository;

import java.io.Serial;

/// Signals a repository operation failure.
public class RepositoryException extends Exception {
  @Serial private static final long serialVersionUID = 1L;

  /// Creates a repository failure with its original cause.
  ///
  /// @param message description of the failed operation
  /// @param parent exception that caused the operation to fail
  public RepositoryException(String message, Exception parent) {
    super(message, parent);
  }
}
