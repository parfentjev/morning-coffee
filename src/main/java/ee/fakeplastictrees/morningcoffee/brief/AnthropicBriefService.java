package ee.fakeplastictrees.morningcoffee.brief;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.errors.AnthropicException;
import com.anthropic.errors.AnthropicInvalidDataException;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.anthropic.models.messages.StopReason;
import ee.fakeplastictrees.morningcoffee.common.Config;
import ee.fakeplastictrees.morningcoffee.common.RepositoryException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

class AnthropicBriefService implements BriefService {
  private final Logger logger = LogManager.getLogger();
  private final Config.Brief.Service.Anthropic config;
  private final BriefRepository repository;
  private final AnthropicClient client;

  /// Creates a brief service using the configured Anthropic account.
  ///
  /// @param config provider credentials and generation settings
  /// @param repository storage for published briefs
  /// @throws IllegalArgumentException if the provider configuration is invalid
  AnthropicBriefService(Config.Brief.Service.Anthropic config, BriefRepository repository) {
    assertConfigValid(config);

    this.config = config;
    this.repository = repository;
    this.client = AnthropicOkHttpClient.builder().apiKey(config.apiKey()).build();
  }

  @Override
  public void generate(String feedEntries) throws BriefServiceException {
    var prompt =
        "<instruction>%s</instruction><feedEntries>%s</feedEntries>"
            .formatted(config.basePrompt(), feedEntries);
    var response = callProvider(prompt);
    if (response.failure().isPresent()) {
      var message = "call to provider failed: %s".formatted(response.failure().get());
      throw new BriefServiceException(message);
    }

    var stopReason = response.stopReason();
    if (stopReason.isEmpty() || stopReason.get().equals(StopReason.END_TURN) == false) {
      var asString = response.stopReason().map(StopReason::asString).orElse("empty");
      var message = "unexpected stop reason: %s".formatted(asString);
      throw new BriefServiceException(message);
    }

    if (response.brief().isEmpty()) {
      var message = "brief is empty";
      throw new BriefServiceException(message);
    }

    var body = response.brief().get();
    if (body.isBlank()) {
      var message = "brief is blank";
      throw new BriefServiceException(message);
    }

    try {
      var brief = new Brief(OffsetDateTime.now(), body);
      repository.save(brief);
    } catch (RepositoryException e) {
      throw new BriefServiceException("failed to save brief", e);
    }
  }

  @Override
  public List<BriefDto> latest(int n) {
    try {
      return repository.latest(n).stream().map(Mapper::briefToBriefDto).toList();
    } catch (RepositoryException e) {
      logger.warn("failed to get latest friefs", e);

      return List.of();
    }
  }

  private void assertConfigValid(Config.Brief.Service.Anthropic config) {
    if (config.apiKey() == null || config.apiKey().isBlank()) {
      throw new IllegalArgumentException("api key is required");
    }

    if (config.basePrompt() == null || config.basePrompt().isBlank()) {
      throw new IllegalArgumentException("base prompt is required");
    }

    if (config.maxTokens() <= 0L) {
      throw new IllegalArgumentException(
          "max tokens limit must be a positive integer, now: %d".formatted(config.maxTokens()));
    }

    if (config.model() == null) {
      throw new IllegalArgumentException("model is required");
    }

    try {
      Model.of(config.model()).validate();
    } catch (AnthropicInvalidDataException e) {
      throw new IllegalArgumentException("invalid model: %s".formatted(config.model()), e);
    }
  }

  private ProviderResponse callProvider(String prompt) throws BriefServiceException {
    logger.debug("calling model with: {}", prompt);
    // TODO: temporary stub
    if (true) {
      return new ProviderResponse(
          Optional.of("temporary stub"), Optional.of(StopReason.END_TURN), Optional.empty());
    }

    try {
      var message =
          client
              .messages()
              .create(
                  MessageCreateParams.builder()
                      .maxTokens(config.maxTokens())
                      .addUserMessage(prompt)
                      .model(config.model())
                      .build());

      var body = new StringBuilder();
      for (var block : message.content()) {
        block.text().ifPresent(text -> body.append(text.text()));
      }

      return new ProviderResponse(
          Optional.of(body.toString()), message.stopReason(), Optional.empty());
    } catch (AnthropicException e) {
      throw new BriefServiceException("anthropic client exception", e);
    }
  }

  private record ProviderResponse(
      Optional<String> brief, Optional<StopReason> stopReason, Optional<Throwable> failure) {}
}
