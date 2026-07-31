package ee.fakeplastictrees.morningcoffee.webserver.render;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toMap;

import org.owasp.encoder.Encode;

abstract class AbstractTemplate {
  private static final String PLACEHOLDER_START = "{{";
  private static final String PLACEHOLDER_END = "}}";

  private final String template;

  protected AbstractTemplate(String template) {
    this.template = template;
  }

  public final String toHtml(TemplateData... values) throws TemplateException {
    var keyValueMap = stream(values).collect(toMap(TemplateData::key, TemplateData::value));

    var output = new StringBuilder();
    var cursor = 0;
    while (cursor < template.length()) {
      var placeholderOpening = template.indexOf(PLACEHOLDER_START, cursor);
      if (indexOfNotFound(placeholderOpening)) {
        output.append(template, cursor, template.length());
        break;
      }

      output.append(template, cursor, placeholderOpening);

      var keyStart = placeholderOpening + PLACEHOLDER_START.length();
      var placeholderEnding = template.indexOf(PLACEHOLDER_END, keyStart);
      if (indexOfNotFound(placeholderEnding)) {
        var message = "placeholder at %d isn't closed".formatted(placeholderOpening);
        throw new TemplateException(message);
      }

      var placeholderValue = template.substring(keyStart, placeholderEnding).trim();
      var placeholder = parsePlaceholderValue(placeholderValue);
      var value = keyValueMap.getOrDefault(placeholder.key(), "");

      output.append(encodeValue(value, placeholder.modifier()));
      cursor = placeholderEnding + PLACEHOLDER_END.length();
    }

    return output.toString();
  }

  /// A helper method that determines whether `String.indexOf(str, fromIndex)` found a matching
  /// value. It is not strictly necessary, but it makes the intention of the `i == -1` check
  /// clearer.
  ///
  /// @param i value returned by `String.indexOf(str, fromIndex)`
  /// @return `true` if a matching value was not found; `false` otherwise
  private boolean indexOfNotFound(int i) {
    return i == -1;
  }

  /// Encodes `value` for use in HTML templates.
  ///
  /// @param value untrusted raw data
  /// @param modifier modifier from the template that indicates how this value must be encoded
  /// @return encoded value
  /// @throws TemplateException if an unexpected modifier is passed
  private String encodeValue(String value, String modifier) throws TemplateException {
    return switch (modifier) {
      case "attribute" -> Encode.forHtmlAttribute(value);
      case "content" -> Encode.forHtmlContent(value);
      case "trusted" -> value;
      default ->
          throw new TemplateException("unexpected template modifier: %s".formatted(modifier));
    };
  }

  /// Parses a raw `placeholderValue` from a template and returns its attributes.
  ///
  /// @param placeholderValue value between opening and closing tags, e.g. `modifier|variable.name`
  /// @return [Placeholder] with extracted attributes
  /// @throws TemplateException if `placeholderValue` doesn't consist of two segments divided by
  // a single vertical bar
  private Placeholder parsePlaceholderValue(String placeholderValue) throws TemplateException {
    var placeholderParts = placeholderValue.split("\\|");
    if (placeholderParts.length != 2) {
      var message =
          "placeholder %s consists of %d parts, expected 2"
              .formatted(placeholderValue, placeholderParts.length);
      throw new TemplateException(message);
    }

    var modifier = placeholderParts[0];
    var key = placeholderParts[1];

    return new Placeholder(modifier, key);
  }

  private record Placeholder(String modifier, String key) {}
}
