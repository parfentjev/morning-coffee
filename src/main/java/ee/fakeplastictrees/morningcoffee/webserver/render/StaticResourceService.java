package ee.fakeplastictrees.morningcoffee.webserver.render;

import ee.fakeplastictrees.morningcoffee.util.Resource;
import ee.fakeplastictrees.morningcoffee.util.ResourceManager;
import ee.fakeplastictrees.morningcoffee.util.ResourceManagerException;
import java.util.HashMap;
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
    var resources =
        loadResources(
            "favicon.png",
            "e73de0c273d6297f0654c096066dedf3e4b1261dfcf3c6ac4c44236d932bf9c4.css",
            "741763f8277c65485b2779875913fecf00bb694935532e0f01886d4800c8e1c3.js");

    return new StaticResourceService(resources);
  }

  /// Finds a static resource by its path relative to the static-resource directory.
  ///
  /// @param relativePath relative resource path
  /// @return matching resource, or an empty optional if the path is not configured
  public Optional<Resource> getResource(String relativePath) {
    return Optional.ofNullable(resources.get(relativePath));
  }

  private static HashMap<String, Resource> loadResources(String... filenames)
      throws ResourceManagerException {
    var resources = new HashMap<String, Resource>();
    for (var filename : filenames) {
      var resource = ResourceManager.loadResource("static/%s".formatted(filename));
      resources.put(filename, resource);
    }

    return resources;
  }
}
