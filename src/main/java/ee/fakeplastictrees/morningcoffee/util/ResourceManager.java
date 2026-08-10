package ee.fakeplastictrees.morningcoffee.util;

import java.io.IOException;
import java.net.URLConnection;

/// Loads resources from the application classpath.
public class ResourceManager {
  /// Loads a resource and determines its media type from its filename.
  ///
  /// @param filename classpath-relative resource filename
  /// @return loaded resource
  /// @throws ResourceManagerException if the resource cannot be loaded or its media type cannot be
  ///   determined
  public static Resource loadResource(String filename) throws ResourceManagerException {
    try (var stream = ResourceManager.class.getClassLoader().getResourceAsStream(filename)) {
      if (stream == null) {
        var message = "failed to load resource: %s".formatted(filename);
        throw new ResourceManagerException(message);
      }

      var contentType = URLConnection.guessContentTypeFromName(filename);
      if (contentType == null) {
        var message = "failed to guess content-type: %s".formatted(filename);
        throw new ResourceManagerException(message);
      }

      var bytes = stream.readAllBytes();

      return new Resource(contentType, bytes);
    } catch (IOException e) {
      var message = "failed to read resource file";
      throw new ResourceManagerException(message, e);
    }
  }
}
