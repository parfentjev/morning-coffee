package ee.fakeplastictrees.morningcoffee.reader;

class FeedParserException extends Exception {
  private static final long serialVersionUID = 1L;

  FeedParserException(String message, Exception parent) {
    super(message, parent);
  }
}
