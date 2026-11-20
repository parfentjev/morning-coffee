package ee.fakeplastictrees.morningcoffee.webserver.render;

import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.loader.ClasspathLoader;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

public class PebbleTemplateService implements TemplateService {
  private final PebbleEngine engine;

  public PebbleTemplateService() {
    var loader = new ClasspathLoader();
    loader.setPrefix("templates/");

    this.engine = new PebbleEngine.Builder().loader(loader).build();
  }

  @Override
  public String render(String template, Map<String, Object> content)
      throws TemplateServiceException {
    try {
      var writer = new StringWriter();
      engine.getTemplate(template).evaluate(writer, content);

      return writer.toString();
    } catch (IOException e) {
      var message = "IO exception during template evaluation";

      throw new TemplateServiceException(message, e);
    } catch (RuntimeException e) {
      var message = "unexpected runtime exception for: %s".formatted(template);

      throw new TemplateServiceException(message, e);
    }
  }
}
