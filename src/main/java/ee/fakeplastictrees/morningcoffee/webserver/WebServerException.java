package ee.fakeplastictrees.morningcoffee.webserver;

import java.io.Serial;

/// Signals a failure to create or start the web server.
public class WebServerException extends Exception {
  @Serial private static final long serialVersionUID = 1L;

  WebServerException(String message, Exception parent) {
    super(message, parent);
  }
}
