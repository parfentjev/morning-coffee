package ee.fakeplastictrees.morningcoffee.webserver;

import java.io.Serial;

public class WebServerException extends Exception {
  @Serial private static final long serialVersionUID = 1L;

  WebServerException(String message, Exception parent) {
    super(message, parent);
  }
}
