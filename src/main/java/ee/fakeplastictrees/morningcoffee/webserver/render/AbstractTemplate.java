package ee.fakeplastictrees.morningcoffee.webserver.render;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toMap;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.owasp.encoder.Encode;

abstract class AbstractTemplate implements Template {
  private final Logger logger = LogManager.getLogger();

  private static final String PLACEHOLDER_START = "{{";
  private static final String PLACEHOLDER_END = "}}";

  private final String template;

  protected AbstractTemplate(String templateFile) throws TemplateException {
    var resource = "templates/%s".formatted(templateFile);
    try (var stream = AbstractTemplate.class.getClassLoader().getResourceAsStream(resource)) {
      if (stream == null) {
        var message = "failed to load template resource: %s".formatted(resource);
        throw new TemplateException(message);
      }

      this.template = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      var message = "failed to read template resource";
      throw new TemplateException(message, e);
    }
  }

  @Override
  // todo: this method is rather long, I can probably add a few helper functions
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

      // todo: perhaps this part can be extracted;
      // parsePlaceholder() thar returns modifier and key - but there are no tuples in java?
      var placeholderValue = template.substring(keyStart, placeholderEnding).trim();
      var placeholderParts = placeholderValue.split("\\|");
      if (placeholderParts.length != 2) {
        var message =
            "placeholder %s consists of %d parts, expected 2"
                .formatted(placeholderValue, placeholderParts.length);
        throw new TemplateException(message);
      }

      var modifier = placeholderParts[0];
      var key = placeholderParts[1];
      // todo: up to here

      var value = keyValueMap.get(key);
      if (value == null) {
        value = new String();
        logger.warn("keyValueMap: expected key {} doesn't exist", key);
      }

      output.append(encodeValue(value, modifier));
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

  private String encodeValue(String value, String modifier) throws TemplateException {
    return switch (modifier) {
      case "attribute" -> Encode.forHtmlAttribute(value);
      case "content" -> Encode.forHtmlContent(value);
      case "trusted" -> value;
      default ->
          throw new TemplateException("unexpected template modifier: %s".formatted(modifier));
    };
  }
}
