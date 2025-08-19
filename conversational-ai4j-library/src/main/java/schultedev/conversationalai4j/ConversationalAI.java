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
 *
 * <h2>Basic Usage:</h2>
 *
 * <pre>{@code
 * try (ConversationalAI ai = ConversationalAI.builder()
 *     .withOllamaModel("llama3.2:3b")
 *     .build()) {
 *
 *     String response = ai.chat("Hello, how are you?");
 *     System.out.println(response);
 * }
 * }</pre>
 *
 * <h2>Voice-Enabled Usage:</h2>
 *
 * <pre>{@code
 * try (ConversationalAI ai = ConversationalAI.builder()
 *     .withOllamaModel("llama3.2:3b")
 *     .withSpeech()
 *     .build()) {
 *
 *     byte[] audioResponse = ai.voiceChat(audioBytes);
 *     byte[] speechAudio = ai.textToSpeech("Hello world");
 * }
 * }</pre>
 *
 * For advanced audio processing and mixed-modality conversations, see: {@link
 * schultedev.conversationalai4j.utils.AudioUtils} and {@link
 * schultedev.conversationalai4j.utils.ConversationUtils}
 */
public class ConversationalAI implements AutoCloseable {

  private static final Logger log = LoggerFactory.getLogger(ConversationalAI.class);

  private final ChatModel model;
  private final ConversationService service;
  private final SpeechToText speechToText;
  private final TextToSpeech textToSpeech;

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

    // Initialize speech services if configured
    if (builder.speechConfig != null && builder.speechConfig.isEnabled()) {
      log.debug("Initializing speech services with config: {}", builder.speechConfig);
      this.speechToText =
          new SpeechToText(
              builder.speechConfig.getSttModelPath(), builder.speechConfig.getLanguage());
      this.textToSpeech =
          new TextToSpeech(
              builder.speechConfig.getTtsModelPath(),
              builder.speechConfig.getLanguage(),
              builder.speechConfig.getVoice());
      log.info("Speech services initialized successfully");
    } else {
      log.debug("Speech services disabled - text-only mode");
      this.speechToText = null;
      this.textToSpeech = null;
    }

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

  /**
   * Voice-to-voice conversation: Process audio input and return audio response. Combines
   * speech-to-text, AI processing, and text-to-speech in one call.
   *
   * @param audioInput Raw audio data in WAV format (16kHz, 16-bit, mono)
   * @return Audio response in WAV format, or empty array if processing failed
   * @throws UnsupportedOperationException if speech services are not configured
   * @throws IllegalArgumentException if audio input is null or empty
   */
  public byte[] voiceChat(byte[] audioInput) {
    if (speechToText == null || textToSpeech == null) {
      throw new UnsupportedOperationException(
          "Speech services are not configured. Use withSpeech() in builder.");
    }

    if (audioInput == null || audioInput.length == 0) {
      throw new IllegalArgumentException("Audio input cannot be null or empty");
    }

    log.debug("Processing voice chat with {} bytes of audio input", audioInput.length);

    try {
      // Step 1: Convert speech to text
      var text = speechToText.transcribe(audioInput);
      log.debug("Transcribed text: '{}'", text);

      if (text.trim().isEmpty()) {
        log.warn("No text transcribed from audio input");
        return new byte[0];
      }

      // Step 2: Get AI response
      var aiResponse = chat(text);
      log.debug("AI response: '{}'", aiResponse);

      if (aiResponse.trim().isEmpty()) {
        log.warn("No AI response generated");
        return new byte[0];
      }

      // Step 3: Convert response to speech
      var audioResponse = textToSpeech.synthesize(aiResponse);
      log.debug("Generated {} bytes of audio response", audioResponse.length);

      return audioResponse;

    } catch (Exception e) {
      log.error("Error in voice chat processing: {}", e.getMessage(), e);
      return new byte[0];
    }
  }

  /**
   * Check if speech services are available.
   *
   * @return true if both speech-to-text and text-to-speech are configured and ready
   */
  public boolean isSpeechEnabled() {
    return speechToText != null
        && textToSpeech != null
        && speechToText.isReady()
        && textToSpeech.isReady();
  }

  /**
   * Convert text directly to speech without LLM processing.
   *
   * @param text The text to convert to speech
   * @return Audio data in WAV format, or empty array if processing failed
   * @throws UnsupportedOperationException if text-to-speech is not configured
   * @throws IllegalArgumentException if text input is null or empty
   */
  public byte[] textToSpeech(String text) {
    if (textToSpeech == null) {
      throw new UnsupportedOperationException(
          "Text-to-speech service is not configured. Use withSpeech() in builder.");
    }

    if (text == null || text.trim().isEmpty()) {
      throw new IllegalArgumentException("Text input cannot be null or empty");
    }

    log.debug("Converting text to speech: '{}'", text);

    try {
      var audioResponse = textToSpeech.synthesize(text);
      log.debug("Generated {} bytes of audio for text conversion", audioResponse.length);
      return audioResponse;
    } catch (Exception e) {
      log.error("Error converting text to speech: {}", e.getMessage(), e);
      return new byte[0];
    }
  }

  /**
   * Clean up resources used by the conversational AI system. This includes speech services and any
   * allocated native resources. Called automatically when used with try-with-resources.
   */
  @Override
  public void close() {
    log.debug("Cleaning up ConversationalAI resources");

    if (speechToText != null) {
      speechToText.close();
    }

    if (textToSpeech != null) {
      textToSpeech.close();
    }

    log.debug("ConversationalAI resources cleaned up");
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
    private SpeechConfig speechConfig;

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
              .timeout(java.time.Duration.ofSeconds(5)) // Short timeout for fast failure
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

    /** Enable speech capabilities with default English configuration */
    public Builder withSpeech() {
      this.speechConfig = SpeechConfig.defaults();
      return this;
    }

    /** Build the ConversationalAI instance */
    public ConversationalAI build() {
      // Validate model configuration
      if (model == null) {
        throw new IllegalStateException("Model must be configured using withOllamaModel()");
      }

      // Validate speech configuration
      if (speechConfig != null && speechConfig.isEnabled()) {
        validateSpeechConfiguration(speechConfig);
      }

      // Validate temperature range (already checked in setter, but double-check)
      if (temperature < 0.0 || temperature > 1.0) {
        throw new IllegalArgumentException("Temperature must be between 0.0 and 1.0");
      }

      return new ConversationalAI(this);
    }

    private void validateSpeechConfiguration(SpeechConfig config) {
      if (config.getSttModelPath() == null
          || config.getSttModelPath().toString().trim().isEmpty()) {
        throw new IllegalStateException(
            "Speech-to-text model path required when speech is enabled. "
                + "Check that models are properly configured in speech configuration.");
      }

      if (config.getTtsModelPath() == null
          || config.getTtsModelPath().toString().trim().isEmpty()) {
        throw new IllegalStateException(
            "Text-to-speech model path required when speech is enabled. "
                + "Check that models are properly configured in speech configuration.");
      }
    }
  }
}
