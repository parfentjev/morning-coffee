package ee.fakeplastictrees.morningcoffee.webserver.render;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/// Loads the application's static resources at startup.
public final class StaticResourceService {
  private StaticResourceService() {}

  /// Loads each resource and maps its media type and bytes to the caller's representation.
  public static <T> Map<String, T> init(BiFunction<String, byte[], T> mapper) throws IOException {
    var resources = new HashMap<String, T>();
    for (var filename :
        List.of(
            "favicon.png",
            "e8482d5f887a2952839cd9c895286d9cca8a100235e1626b2dae6098ac2e2df9.css",
            "741763f8277c65485b2779875913fecf00bb694935532e0f01886d4800c8e1c3.js")) {
      var resource = ResourceManager.loadResource("static/%s".formatted(filename));
      resources.put(filename, mapper.apply(resource.contentType(), resource.contents()));
    }
    return Map.copyOf(resources);
  }
}
