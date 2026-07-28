package ee.fakeplastictrees.morningcoffee.reader;

import java.io.Serial;

class FeedParserException extends Exception {
  @Serial private static final long serialVersionUID = 1L;

  FeedParserException(String message, Exception parent) {
    super(message, parent);
  }
}
