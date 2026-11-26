package ee.fakeplastictrees.morningcoffee.brief;

import java.io.Serial;

class BriefServiceException extends Exception {
  @Serial private static final long serialVersionUID = 1L;

  BriefServiceException(String message) {
    super(message);
  }

  BriefServiceException(String message, Exception cause) {
    super(message, cause);
  }
}
