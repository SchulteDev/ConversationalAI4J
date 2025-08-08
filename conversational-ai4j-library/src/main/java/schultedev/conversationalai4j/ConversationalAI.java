package schultedev.conversationalai4j;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.UserMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry point for conversational AI functionality. Provides fluent builder API for easy
 * configuration and usage.
 */
public class ConversationalAI {

  private static final Logger log = LoggerFactory.getLogger(ConversationalAI.class);

  private final ChatModel model;
  private final ConversationService service;

  private ConversationalAI(Builder builder) {
    log.debug(
        "Creating ConversationalAI instance with model: {}",
        builder.model.getClass().getSimpleName());

    this.model = builder.model;
    var aiServiceBuilder =
        AiServices.builder(ConversationService.class).chatModel(model).chatMemory(builder.memory);

    if (builder.systemPrompt != null && !builder.systemPrompt.trim().isEmpty()) {
      log.trace("Setting system prompt: {}", builder.systemPrompt);
      aiServiceBuilder.systemMessageProvider(chatMemoryId -> builder.systemPrompt);
    }

    this.service = aiServiceBuilder.build();
    log.debug("ConversationalAI instance created successfully");
  }

  /** Create a new builder for ConversationalAI */
  public static Builder builder() {
    return new Builder();
  }

  /** Send a message and get AI response */
  public String chat(String message) {
    log.trace("Processing chat message: {}", message);
    var response = service.chat(message);
    log.trace("Generated response of {} characters", response != null ? response.length() : 0);
    return response;
  }

  /** Get the underlying chat model */
  public ChatModel getModel() {
    return model;
  }

  /** Internal service interface for AI interactions */
  private interface ConversationService {
    String chat(@UserMessage String message);
  }

  /** Builder for ConversationalAI configuration */
  public static class Builder {
    private ChatModel model;
    private MessageWindowChatMemory memory = MessageWindowChatMemory.withMaxMessages(10);
    private String systemPrompt;
    private double temperature = 0.7;

    private Builder() {}

    /** Configure Ollama model */
    public Builder withOllamaModel(String modelName) {
      return withOllamaModel(modelName, "http://localhost:11434");
    }

    /** Configure Ollama model with custom base URL */
    public Builder withOllamaModel(String modelName, String baseUrl) {
      log.debug("Configuring Ollama model '{}' with base URL: {}", modelName, baseUrl);
      this.model =
          OllamaChatModel.builder()
              .baseUrl(baseUrl)
              .modelName(modelName)
              .temperature(temperature)
              .build();
      return this;
    }

    /** Configure conversation memory with default window size (10 messages) */
    public Builder withMemory() {
      this.memory = ConversationMemory.defaultMemory();
      return this;
    }

    /** Configure conversation memory with custom window size */
    public Builder withMemory(int maxMessages) {
      this.memory = ConversationMemory.sliding(maxMessages);
      return this;
    }

    /** Configure conversation memory */
    public Builder withMemory(MessageWindowChatMemory memory) {
      this.memory = memory;
      return this;
    }

    /** Set system prompt */
    public Builder withSystemPrompt(String systemPrompt) {
      this.systemPrompt = systemPrompt;
      return this;
    }

    /** Set temperature for model responses (0.0 to 1.0) */
    public Builder withTemperature(double temperature) {
      if (temperature < 0.0 || temperature > 1.0) {
        throw new IllegalArgumentException("Temperature must be between 0.0 and 1.0");
      }
      this.temperature = temperature;
      return this;
    }

    /** Build the ConversationalAI instance */
    public ConversationalAI build() {
      if (model == null) {
        throw new IllegalStateException(
            "Model must be configured. Use withOllamaModel() or withModel()");
      }
      return new ConversationalAI(this);
    }
  }
}
