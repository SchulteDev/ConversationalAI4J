package schultedev.conversationalai4j.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import schultedev.conversationalai4j.ConversationalAI;

/**
 * Spring MVC Controller for handling conversation interactions in the demo application. Provides a
 * simple interface for users to send messages and receive responses.
 */
@Controller
public class ConversationController {

  private static final Logger log = LoggerFactory.getLogger(ConversationController.class);
  private final ConversationalAI conversationalAI;

  @Value("${ollama.base-url:http://localhost:11434}")
  private String ollamaBaseUrl;

  @Value("${ollama.model-name:llama3.2:3b}")
  private String ollamaModelName;

  /**
   * Constructor that initializes the ConversationalAI instance with configurable Ollama settings.
   * Supports both local development and Docker containerized environments.
   */
  public ConversationController() {
    ConversationalAI tempAI;
    try {
      // Use Spring-injected values after construction
      var modelName =
          System.getProperty(
              "ollama.model-name",
              System.getenv().getOrDefault("OLLAMA_MODEL_NAME", "llama3.2:3b"));
      var baseUrl =
          System.getProperty(
              "ollama.base-url",
              System.getenv().getOrDefault("OLLAMA_BASE_URL", "http://localhost:11434"));

      log.info("Initializing ConversationalAI with Ollama model '{}' at '{}'", modelName, baseUrl);

      tempAI =
          ConversationalAI.builder()
              .withOllamaModel(modelName, baseUrl)
              .withMemory() // Use default memory
              .withSystemPrompt(
                  "You are a helpful AI assistant in a demo application. "
                      + "Keep responses concise and friendly.")
              .withTemperature(0.7)
              .withSpeech() // Enable speech capabilities with defaults
              .build();

      log.info("ConversationalAI successfully initialized with Ollama model");
    } catch (Exception e) {
      log.warn(
          "Failed to initialize ConversationalAI with Ollama: {}. Falling back to echo mode",
          e.getMessage());
      if (log.isDebugEnabled()) {
        log.debug("ConversationalAI initialization error details", e);
      }
      tempAI = null;
    }
    this.conversationalAI = tempAI;
  }

  /**
   * Displays the main conversation page.
   *
   * @param model the model to add attributes to
   * @return the template name
   */
  @GetMapping("/")
  public String index(Model model) {
    model.addAttribute("welcomeText", "ConversationalAI4J Demo");
    return "conversation";
  }

  /**
   * Processes the user's message and generates a response. Uses the ConversationalAI library if
   * available, otherwise falls back to echo mode.
   *
   * @param message the user's input message
   * @param model the model to add attributes to
   * @return the template name
   */
  @PostMapping("/send")
  public String sendMessage(@RequestParam("message") String message, Model model) {
    model.addAttribute("welcomeText", "ConversationalAI4J Demo");
    model.addAttribute("message", message);

    String response;
    if (message == null || message.trim().isEmpty()) {
      log.info("Received empty message from user");
      response = "Please enter a message.";
    } else {
      log.info("USER INPUT: '{}'", message);

      try {
        if (conversationalAI != null) {
          log.info("Processing message with AI...");
          response = conversationalAI.chat(message);
          log.info("AI RESPONSE: '{}'", response);
        } else {
          log.warn("ConversationalAI unavailable, using echo mode");
          response = "Echo (AI unavailable): " + message;
        }
      } catch (Exception e) {
        log.error("Error processing message '{}': {}", message, e.getMessage());
        if (log.isDebugEnabled()) {
          log.debug("Message processing error details", e);
        }
        response =
            "Sorry, I'm having trouble processing your request. "
                + "Error: "
                + e.getMessage()
                + "\nFallback echo: "
                + message;
      }
    }

    model.addAttribute("response", response);
    return "conversation";
  }

  /**
   * Voice-to-voice conversation endpoint: Process audio input and return audio response. Expects
   * WAV audio data and returns WAV audio response.
   *
   * @param audioData Raw audio data in WAV format (16kHz, 16-bit, mono)
   * @return Audio response in WAV format, or error response
   */
  @PostMapping(
      value = "/voice-chat",
      consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE,
      produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  public ResponseEntity<byte[]> voiceChat(@RequestBody byte[] audioData) {
    if (conversationalAI == null) {
      log.warn("ConversationalAI not available for voice chat");
      return ResponseEntity.status(503)
          .body("ConversationalAI service is not available".getBytes());
    }

    if (!conversationalAI.isSpeechEnabled()) {
      log.warn("Speech services not available for voice chat");
      return ResponseEntity.badRequest().body("Speech services are not configured".getBytes());
    }

    if (audioData == null || audioData.length == 0) {
      log.debug("Received empty audio data");
      return ResponseEntity.badRequest().body("Audio data is required".getBytes());
    }

    log.info("Processing voice chat with {} bytes of audio input", audioData.length);

    try {
      var audioResponse = conversationalAI.voiceChat(audioData);

      if (audioResponse.length == 0) {
        log.warn("No audio response generated");
        return ResponseEntity.internalServerError()
            .body("Failed to generate audio response".getBytes());
      }

      log.info("Successfully generated {} bytes of audio response", audioResponse.length);
      return ResponseEntity.ok()
          .contentType(MediaType.parseMediaType("audio/wav"))
          .body(audioResponse);

    } catch (Exception e) {
      log.error("Error processing voice chat: {}", e.getMessage(), e);
      return ResponseEntity.internalServerError()
          .body(("Voice chat error: " + e.getMessage()).getBytes());
    }
  }

  /**
   * Text input with voice response: Process text and return audio response.
   *
   * @param message Text message to process
   * @return Audio response in WAV format, or error response
   */
  @PostMapping(
      value = "/text-to-voice",
      consumes = MediaType.TEXT_PLAIN_VALUE,
      produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  public ResponseEntity<byte[]> textToVoice(@RequestBody String message) {
    if (conversationalAI == null) {
      log.warn("ConversationalAI not available for text-to-voice");
      return ResponseEntity.status(503)
          .body("ConversationalAI service is not available".getBytes());
    }

    if (!conversationalAI.isTextToSpeechEnabled()) {
      log.warn("Text-to-speech not available");
      return ResponseEntity.badRequest()
          .body("Text-to-speech service is not configured".getBytes());
    }

    if (message == null || message.trim().isEmpty()) {
      log.debug("Received empty text message");
      return ResponseEntity.badRequest().body("Text message is required".getBytes());
    }

    log.info("Processing text-to-voice for message: '{}'", message);

    try {
      var audioResponse = conversationalAI.chatWithVoiceResponse(message);

      if (audioResponse.length == 0) {
        log.warn("No audio response generated for text input");
        return ResponseEntity.internalServerError()
            .body("Failed to generate audio response".getBytes());
      }

      log.info("Successfully generated {} bytes of audio response for text", audioResponse.length);
      return ResponseEntity.ok()
          .contentType(MediaType.parseMediaType("audio/wav"))
          .body(audioResponse);

    } catch (Exception e) {
      log.error("Error processing text-to-voice: {}", e.getMessage(), e);
      return ResponseEntity.internalServerError()
          .body(("Text-to-voice error: " + e.getMessage()).getBytes());
    }
  }

  /**
   * Voice input with text response: Process audio and return text response.
   *
   * @param audioData Raw audio data in WAV format (16kHz, 16-bit, mono)
   * @return Text response from AI, or error message
   */
  @PostMapping(
      value = "/voice-to-text",
      consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE,
      produces = MediaType.TEXT_PLAIN_VALUE)
  public ResponseEntity<String> voiceToText(@RequestBody byte[] audioData) {
    if (conversationalAI == null) {
      log.warn("ConversationalAI not available for voice-to-text");
      return ResponseEntity.status(503).body("ConversationalAI service is not available");
    }

    if (!conversationalAI.isSpeechToTextEnabled()) {
      log.warn("Speech-to-text not available");
      return ResponseEntity.badRequest().body("Speech-to-text service is not configured");
    }

    if (audioData == null || audioData.length == 0) {
      log.debug("Received empty audio data");
      return ResponseEntity.badRequest().body("Audio data is required");
    }

    log.info("Processing voice-to-text with {} bytes of audio input", audioData.length);

    try {
      var textResponse = conversationalAI.chatWithTextResponse(audioData);

      if (textResponse.trim().isEmpty()) {
        log.warn("No text response generated for audio input");
        return ResponseEntity.internalServerError().body("Failed to generate text response");
      }

      log.info("Successfully generated text response: '{}'", textResponse);
      return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(textResponse);

    } catch (Exception e) {
      log.error("Error processing voice-to-text: {}", e.getMessage(), e);
      return ResponseEntity.internalServerError().body("Voice-to-text error: " + e.getMessage());
    }
  }

  /**
   * Check speech service status endpoint.
   *
   * @return Status information about available speech services
   */
  @GetMapping(value = "/speech-status", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> speechStatus() {
    if (conversationalAI == null) {
      return ResponseEntity.ok(
          "{\"available\": false, \"reason\": \"ConversationalAI service not available\"}");
    }

    var speechEnabled = conversationalAI.isSpeechEnabled();
    var sttEnabled = conversationalAI.isSpeechToTextEnabled();
    var ttsEnabled = conversationalAI.isTextToSpeechEnabled();

    var status =
        String.format(
            "{\"available\": %s, \"speechToText\": %s, \"textToSpeech\": %s, \"fullSpeech\": %s}",
            speechEnabled, sttEnabled, ttsEnabled, speechEnabled);

    log.debug("Speech status: {}", status);
    return ResponseEntity.ok(status);
  }
}
