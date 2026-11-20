package ee.fakeplastictrees.morningcoffee.webserver.render;

import java.io.Serial;

/// Signals a failure in a template service operation.
public class TemplateServiceException extends Exception {
  @Serial private static final long serialVersionUID = 1L;

  public TemplateServiceException(String message, Exception cause) {
    super(message, cause);
  }
}
