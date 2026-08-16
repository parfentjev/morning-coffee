package ee.fakeplastictrees.morningcoffee.reader;

import java.io.Serial;

public class FeedClientStatusCodeException extends Exception {
  @Serial private static final long serialVersionUID = 1L;

  private final int statusCode;

  FeedClientStatusCodeException(int statusCode) {
    this.statusCode = statusCode;
  }

  int statusCode() {
    return statusCode;
  }
}
