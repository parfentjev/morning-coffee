package ee.fakeplastictrees.morningcoffee.webserver.render;

import ee.fakeplastictrees.morningcoffee.util.Resource;
import ee.fakeplastictrees.morningcoffee.util.ResourceManager;
import ee.fakeplastictrees.morningcoffee.util.ResourceManagerException;
import java.util.Map;
import java.util.Optional;

/// Loads and provides the application's static resources.
public class StaticResourceService {
  private final Map<String, Resource> resources;

  private StaticResourceService(Map<String, Resource> resources) {
    this.resources = resources;
  }

  /// Loads all static resources into memory.
  ///
  /// @return initialized static resource service
  /// @throws ResourceManagerException if a static resource cannot be loaded
  public static StaticResourceService init() throws ResourceManagerException {
    return new StaticResourceService(
        Map.of(
            "main.css",
            ResourceManager.loadResource("static/%s".formatted("main.css")),
            "main.js",
            ResourceManager.loadResource("static/%s".formatted("main.js")),
            "favicon.png",
            ResourceManager.loadResource("static/%s".formatted("favicon.png"))));
  }

  /// Finds a static resource by its path relative to the static-resource directory.
  ///
  /// @param relativePath relative resource path
  /// @return matching resource, or an empty optional if the path is not configured
  public Optional<Resource> getResource(String relativePath) {
    return Optional.ofNullable(resources.get(relativePath));
  }
}
