package ee.fakeplastictrees.morningcoffee.repository;

import java.io.Serial;

public class RepositoryException extends Exception {
  @Serial private static final long serialVersionUID = 1L;

  RepositoryException(String message) {
    super(message);
  }

  RepositoryException(String message, Exception parent) {
    super(message, parent);
  }
}
