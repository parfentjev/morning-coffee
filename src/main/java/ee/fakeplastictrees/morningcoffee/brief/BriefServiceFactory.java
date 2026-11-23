package ee.fakeplastictrees.morningcoffee.brief;

import ee.fakeplastictrees.morningcoffee.common.Config;
import ee.fakeplastictrees.morningcoffee.common.ConnectionPool;

public class BriefServiceFactory {
  private static BriefRepository repository;

  /// Creates a brief service for the configured provider.
  ///
  /// @param config provider selection and provider-specific settings
  /// @param pool connection pool used for brief storage
  /// @return the selected brief service
  /// @throws IllegalArgumentException if the provider is unknown or its configuration is invalid
  public static BriefService get(Config.Brief.Service config, ConnectionPool pool)
      throws IllegalArgumentException {
    if (repository == null) {
      repository = new PostgresBriefRepository(pool);
    }

    return switch (Provider.valueOf(config.provider())) {
      case DUMMY -> new DummyBriefService();
      case ANTHROPIC -> new AnthropicBriefService(config.anthropic(), repository);
    };
  }

  private enum Provider {
    DUMMY,
    ANTHROPIC
  }
}
