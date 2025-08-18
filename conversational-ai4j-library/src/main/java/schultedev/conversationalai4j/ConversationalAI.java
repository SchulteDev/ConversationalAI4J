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
   * Mixed modality: Process text input and return audio response.
   *
   * @param textInput Text message to process
   * @return Audio response in WAV format, or empty array if processing failed
   * @throws UnsupportedOperationException if text-to-speech is not configured
   * @throws IllegalArgumentException if text input is null or empty
   */
  public byte[] chatWithVoiceResponse(String textInput) {
    if (textToSpeech == null) {
      throw new UnsupportedOperationException(
          "Text-to-speech service is not configured. Use withSpeech() in builder.");
    }

    if (textInput == null || textInput.trim().isEmpty()) {
      throw new IllegalArgumentException("Text input cannot be null or empty");
    }

    log.debug("Processing text input with voice response: '{}'", textInput);

    try {
      // Get AI response as text
      var aiResponse = chat(textInput);

      // Convert to speech
      var audioResponse = textToSpeech.synthesize(aiResponse);
      log.debug("Generated {} bytes of audio response for text input", audioResponse.length);

      return audioResponse;

    } catch (Exception e) {
      log.error("Error generating voice response for text: {}", e.getMessage(), e);
      return new byte[0];
    }
  }

  /**
   * Mixed modality: Process audio input and return text response.
   *
   * @param audioInput Raw audio data in WAV format (16kHz, 16-bit, mono)
   * @return Text response from AI, or empty string if processing failed
   * @throws UnsupportedOperationException if speech-to-text is not configured
   * @throws IllegalArgumentException if audio input is null or empty
   */
  /**
   * Convert speech to text only (no chat processing).
   *
   * @param audioInput Raw audio data in WAV format (16kHz, 16-bit, mono)
   * @throws UnsupportedOperationException if speech-to-text is not configured
   * @throws IllegalArgumentException if audio input is null or empty
   */
  public void speechToText(byte[] audioInput) {
    if (speechToText == null) {
      throw new UnsupportedOperationException(
          "Speech-to-text service is not configured. Use withSpeech() in builder.");
    }

    if (audioInput == null || audioInput.length == 0) {
      throw new IllegalArgumentException("Audio input cannot be null or empty");
    }

    log.info("Processing speech-to-text: {} bytes", audioInput.length);

    try {
      var text = speechToText.transcribe(audioInput);
      log.info("Speech-to-text result: '{}'", text);
    } catch (Exception e) {
      log.error("Speech-to-text failed: {}", e.getMessage(), e);
    }
  }

  /**
   * Convert speech to text with explicit format specification.
   *
   * @param audioInput Raw audio data
   * @param format Audio format specification
   * @return Transcribed text, or error message if transcription failed
   * @throws UnsupportedOperationException if speech-to-text is not configured
   * @throws IllegalArgumentException if audio input is null or empty
   */
  public String speechToText(byte[] audioInput, AudioFormat format) {
    if (speechToText == null) {
      throw new UnsupportedOperationException(
          "Speech-to-text service is not configured. Use withSpeech() in builder.");
    }

    if (audioInput == null || audioInput.length == 0) {
      throw new IllegalArgumentException("Audio input cannot be null or empty");
    }

    log.info("Processing speech-to-text: {} bytes with format {}", audioInput.length, format);

    try {
      // Use SpeechService's enhanced method
      var speechService = new SpeechService();
      var text = speechService.speechToText(audioInput, format);
      log.info("Speech-to-text result: '{}'", text);
      return text;
    } catch (Exception e) {
      log.error("Speech-to-text failed: {}", e.getMessage(), e);
      return "Speech recognition error: " + e.getMessage();
    }
  }

  public String chatWithTextResponse(byte[] audioInput) {
    if (speechToText == null) {
      throw new UnsupportedOperationException(
          "Speech-to-text service is not configured. Use withSpeech() in builder.");
    }

    if (audioInput == null || audioInput.length == 0) {
      throw new IllegalArgumentException("Audio input cannot be null or empty");
    }

    log.debug("Processing audio input with text response: {} bytes", audioInput.length);

    try {
      // Convert speech to text
      var text = speechToText.transcribe(audioInput);
      log.debug("Transcribed text: '{}'", text);

      if (text.trim().isEmpty()) {
        log.warn("No text transcribed from audio input");
        return "";
      }

      // Get AI response
      var aiResponse = chat(text);
      log.debug("Generated text response for audio input: '{}'", aiResponse);

      return aiResponse;

    } catch (Exception e) {
      log.error("Error generating text response for audio: {}", e.getMessage(), e);
      return "";
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
   * Check if text-to-speech service is available.
   *
   * @return true if text-to-speech is configured and ready
   */
  public boolean isTextToSpeechEnabled() {
    return textToSpeech != null && textToSpeech.isReady();
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
   * Check if speech-to-text service is available.
   *
   * @return true if speech-to-text is configured and ready
   */
  public boolean isSpeechToTextEnabled() {
    return speechToText != null && speechToText.isReady();
  }

  /**
   * Clean up resources used by the conversational AI system. This includes speech services and any
   * allocated native resources. Should be called when the ConversationalAI instance is no longer
   * needed.
   */
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
      if (model == null) {
        throw new IllegalStateException(
            "Model must be configured. Use withOllamaModel() or withModel()");
      }
      return new ConversationalAI(this);
    }
  }
}
