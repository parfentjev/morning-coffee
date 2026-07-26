package ee.fakeplastictrees.morningcoffee.webserver.render;

import java.util.function.Function;

public class TemplateValue {
  private final String key;
  private final String value;

  public TemplateValue(String key, String value, Function<String, String> encoder) {
    this.key = key;
    this.value = encoder.apply(value);
  }

  public String getKey() {
    return key;
  }

  public String getValue() {
    return value;
  }
}
