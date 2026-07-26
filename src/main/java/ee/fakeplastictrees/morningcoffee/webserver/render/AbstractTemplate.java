package ee.fakeplastictrees.morningcoffee.webserver.render;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toMap;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

abstract class AbstractTemplate implements Template {
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
  public final String renderToHtml(TemplateValue... values) {
    if (values.length == 0) {
      return template;
    }

    var keyValueMap = stream(values).collect(toMap(TemplateValue::getKey, TemplateValue::getValue));

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
        // todo: add proper exception
        var message = "placeholder at %d isn't closed".formatted(placeholderOpening);
        throw new RuntimeException(message);
      }

      var key = template.substring(keyStart, placeholderEnding).trim();
      var value = keyValueMap.get(key);
      if (value == null) {
        // todo: probably not worth throwing an exception,
        // but add some visible text for development purposes
        value = "keyValueMap: key %s not found".formatted(key);
      }

      output.append(value);
      cursor = placeholderEnding + PLACEHOLDER_END.length();
    }

    return output.toString();
  }

  /// A helper function that determines whether `String.indexOf(str, fromIndex)` found a matching
  /// value or not. It isn't strictly necessary, but it makes the intention behind the `i == -1`
  /// check clearer.
  ///
  /// @return `true` if a matching value was not found.
  /// @return `false` if a matching value was found.
  private boolean indexOfNotFound(int i) {
    return i == -1;
  }
}
