package ee.fakeplastictrees.morningcoffee.reader;

class FeedClientException extends Exception {
  private static final long serialVersionUID = 1L;

  FeedClientException(String message) {
    super(message);
  }

  FeedClientException(String message, Exception parent) {
    super(message, parent);
  }
}
