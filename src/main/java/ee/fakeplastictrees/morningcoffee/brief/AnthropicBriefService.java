package ee.fakeplastictrees.morningcoffee.brief;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.errors.AnthropicException;
import com.anthropic.errors.AnthropicInvalidDataException;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.anthropic.models.messages.StopReason;
import ee.fakeplastictrees.morningcoffee.Config;
import ee.fakeplastictrees.morningcoffee.common.repository.RepositoryException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

class AnthropicBriefService implements BriefService {
  private final Logger logger = LogManager.getLogger();
  private final Config.Brief.Anthropic config;
  private final BriefRepository repository;
  private final AnthropicClient client;

  /// Creates a brief service using the configured Anthropic account.
  ///
  /// @param config provider credentials and generation settings
  /// @param repository storage for published briefs
  /// @throws IllegalArgumentException if the provider configuration is invalid
  AnthropicBriefService(Config.Brief.Anthropic config, BriefRepository repository) {
    assertConfigValid(config);

    this.config = config;
    this.repository = repository;
    this.client = AnthropicOkHttpClient.builder().apiKey(config.apiKey()).build();
  }

  @Override
  public void generate(String idempotencyKey, String sourceMaterial) {
    // TODO: handle idempotency key
    var prompt = "basePrompt + sourceMaterial";
    var response = callProvider(prompt);
    if (response.failure().isPresent()) {
      logger.error("call to provider failed: {}", response.failure().get());
      return;
    }

    var stopReason = response.stopReason();
    if (stopReason.isEmpty() || stopReason.get().equals(StopReason.END_TURN) == false) {
      var asString = response.stopReason().map(StopReason::asString).orElse("empty");
      logger.warn("unexpected stop reason: {}", asString);

      return;
    }

    if (response.brief().isEmpty()) {
      logger.warn("brief is empty");

      return;
    }

    var body = response.brief().get();
    if (body.isBlank()) {
      logger.error("brief is blank");

      return;
    }

    try {
      var brief = new Brief(OffsetDateTime.now(), body);
      repository.save(brief);
    } catch (RepositoryException e) {
      logger.warn("failed to save brief", e);
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

  private void assertConfigValid(Config.Brief.Anthropic config) {
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

  private ProviderResponse callProvider(String prompt) {
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
      return new ProviderResponse(Optional.empty(), Optional.empty(), Optional.of(e));
    }
  }

  private record ProviderResponse(
      Optional<String> brief, Optional<StopReason> stopReason, Optional<Throwable> failure) {}
}
