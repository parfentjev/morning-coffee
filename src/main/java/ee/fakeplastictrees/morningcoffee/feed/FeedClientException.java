package ee.fakeplastictrees.morningcoffee.feed;

import java.io.Serial;

class FeedClientException extends Exception {
  @Serial private static final long serialVersionUID = 1L;

  FeedClientException(String message) {
    super(message);
  }

  FeedClientException(String message, Exception parent) {
    super(message, parent);
  }
}
