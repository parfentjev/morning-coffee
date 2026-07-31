package ee.fakeplastictrees.morningcoffee.webserver.render;

import java.io.Serial;

/// Signals a failure to load or render a template.
public class TemplateException extends Exception {
  @Serial private static final long serialVersionUID = 1L;

  TemplateException(String message) {
    super(message);
  }

  TemplateException(String message, Exception parent) {
    super(message, parent);
  }
}
