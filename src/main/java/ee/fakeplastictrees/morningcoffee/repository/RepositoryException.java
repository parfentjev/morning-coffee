package ee.fakeplastictrees.morningcoffee.repository;

import java.io.Serial;

/// Signals a repository operation failure.
public class RepositoryException extends Exception {
  @Serial private static final long serialVersionUID = 1L;

  RepositoryException(Exception parent) {
    super(parent);
  }
}
