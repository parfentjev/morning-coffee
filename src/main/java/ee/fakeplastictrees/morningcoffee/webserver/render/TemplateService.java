package ee.fakeplastictrees.morningcoffee.webserver.render;

import java.util.Map;

/// Renders HTML templates for user-facing responses.
public interface TemplateService {
  /// Renders the specified template with the supplied content.
  ///
  /// @param template path to the HTML template, relative to the template storage root
  /// @param content key-value pairs passed to the renderer
  /// @return rendered HTML that is safe to send to the user
  /// @throws TemplateServiceException if the service cannot produce the HTML for any reason
  String render(String template, Map<String, Object> content) throws TemplateServiceException;
}
