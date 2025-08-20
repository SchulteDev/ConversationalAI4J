package schultedev.conversationalai4j.demo;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import schultedev.conversationalai4j.ConversationUtils;
import schultedev.conversationalai4j.ConversationalAI;
import schultedev.conversationalai4j.SpeechConfig;

/**
 * Spring MVC Controller for handling conversation interactions in the demo application. Provides a
 * simple interface for users to send messages and receive responses.
 */
@Controller
public class ConversationController {

  private static final Logger log = LoggerFactory.getLogger(ConversationController.class);
  private final ConversationalAI conversationalAI;
  private final List<Message> conversationHistory = new ArrayList<>();

  private final AppConfig appConfig;

  /**
   * Constructor that initializes the ConversationalAI instance with Spring-managed configuration.
   * Uses dependency injection for clean separation of concerns.
   */
  public ConversationController(AppConfig appConfig) {
    this.appConfig = appConfig;
    ConversationalAI tempAI;
    try {
      var modelName = appConfig.getOllamaModelName();
      var baseUrl = appConfig.getOllamaBaseUrl();

      log.info(
          "AppConfig loaded - Ollama: {}, Speech enabled: {}, App prompt: '{}'",
          baseUrl,
          appConfig.isSpeechEnabled(),
          appConfig.getSystemPrompt());
      log.info("Initializing ConversationalAI with Ollama model '{}' at '{}'", modelName, baseUrl);

      // Build speech configuration programmatically if enabled
      SpeechConfig speechConfig = null;
      if (appConfig.isSpeechEnabled()) {
        var builder =
            new SpeechConfig.Builder().withLanguage("en-US").withVoice("female").withEnabled(true);

        // Configure STT model if specified
        if (appConfig.getSpeechWhisperModelPath() != null) {
          builder.withSttModel(Paths.get(appConfig.getSpeechWhisperModelPath()));
        }

        // Configure TTS model if specified
        if (appConfig.getSpeechPiperModelPath() != null) {
          builder.withTtsModel(Paths.get(appConfig.getSpeechPiperModelPath()));
        }

        speechConfig = builder.build();
      }

      var aiBuilder =
          ConversationalAI.builder()
              .withOllamaModel(modelName, baseUrl)
              .withMemory() // Use default memory
              .withSystemPrompt(appConfig.getSystemPrompt())
              .withTemperature(appConfig.getTemperature());

      if (speechConfig != null) {
        aiBuilder.withSpeech(speechConfig);
      }

      tempAI = aiBuilder.build();

      log.info("ConversationalAI successfully initialized with Ollama model");
    } catch (Exception e) {
      log.error("Failed to initialize ConversationalAI with Ollama - Full error details:", e);
      log.warn(
          "Failed to initialize ConversationalAI with Ollama: {}. Falling back to echo mode",
          e.getMessage());
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
    model.addAttribute("conversationHistory", conversationHistory);
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

    if (!conversationalAI.isSpeechEnabled()) {
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
      var audioResponse = ConversationUtils.chatWithVoiceResponse(conversationalAI, message);

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

    if (!conversationalAI.isSpeechEnabled()) {
      log.warn("Speech-to-text not available");
      return ResponseEntity.badRequest().body("Speech-to-text service is not configured");
    }

    if (audioData == null || audioData.length == 0) {
      log.debug("Received empty audio data");
      return ResponseEntity.badRequest().body("Audio data is required");
    }

    log.info("Processing voice-to-text with {} bytes of audio input", audioData.length);

    try {
      var textResponse = ConversationUtils.chatWithTextResponse(conversationalAI, audioData);

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
   * Direct text-to-speech conversion: Convert text directly to audio without LLM processing.
   *
   * @param text Text to convert to speech (already processed AI response)
   * @return Audio response in WAV format, or error response
   */
  @PostMapping(
      value = "/direct-tts",
      consumes = MediaType.TEXT_PLAIN_VALUE,
      produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  public ResponseEntity<byte[]> directTextToSpeech(@RequestBody String text) {
    if (conversationalAI == null) {
      log.warn("ConversationalAI not available for direct TTS");
      return ResponseEntity.status(503)
          .body("ConversationalAI service is not available".getBytes());
    }

    if (!conversationalAI.isSpeechEnabled()) {
      log.warn("Text-to-speech not available for direct TTS");
      return ResponseEntity.badRequest()
          .body("Text-to-speech service is not configured".getBytes());
    }

    if (text == null || text.trim().isEmpty()) {
      log.debug("Received empty text for direct TTS");
      return ResponseEntity.badRequest().body("Text is required".getBytes());
    }

    log.info("Processing direct TTS for text: '{}'", text);

    try {
      var audioResponse = conversationalAI.textToSpeech(text.trim());

      if (audioResponse.length == 0) {
        log.warn("No audio response generated for direct TTS");
        return ResponseEntity.internalServerError()
            .body("Failed to generate audio response".getBytes());
      }

      log.info(
          "Successfully generated {} bytes of audio response for direct TTS", audioResponse.length);
      return ResponseEntity.ok()
          .contentType(MediaType.parseMediaType("audio/wav"))
          .body(audioResponse);

    } catch (Exception e) {
      log.error("Error processing direct TTS: {}", e.getMessage(), e);
      return ResponseEntity.internalServerError()
          .body(("Direct TTS error: " + e.getMessage()).getBytes());
    }
  }

  /**
   * Chat API endpoint for AJAX requests - returns JSON response.
   *
   * @param message the user's input message
   * @return JSON response with AI message
   */
  @PostMapping(
      value = "/chat",
      consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> chatAPI(@RequestParam("message") String message) {
    if (message == null || message.trim().isEmpty()) {
      return ResponseEntity.badRequest().body("{\"error\": \"Message is required\"}");
    }

    log.info("API USER INPUT: '{}'", message);

    // Add user message to history
    conversationHistory.add(new Message(message, true, false));

    try {
      String response;
      if (conversationalAI != null) {
        log.info("Processing message with AI...");
        response = conversationalAI.chat(message);
        log.info("API AI RESPONSE: '{}'", response);
      } else {
        log.warn("ConversationalAI unavailable, using echo mode");
        response = "Echo (AI unavailable): " + message;
      }

      // Add AI response to history with TTS capability indication
      var hasAudio = conversationalAI != null;
      conversationHistory.add(new Message(response, false, hasAudio));

      // Return JSON response
      var jsonResponse =
          String.format(
              "{\"response\": \"%s\", \"hasAudio\": %s}",
              response.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r"), hasAudio);

      return ResponseEntity.ok(jsonResponse);

    } catch (Exception e) {
      log.error("Error processing API message '{}': {}", message, e.getMessage());
      var errorResponse =
          "Sorry, I'm having trouble processing your request. Error: " + e.getMessage();

      // Add error response to history
      conversationHistory.add(new Message(errorResponse, false, false));

      var jsonError =
          String.format(
              "{\"response\": \"%s\", \"hasAudio\": false}",
              errorResponse.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r"));

      return ResponseEntity.ok(jsonError);
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

    var status =
        String.format(
            "{\"available\": %s, \"speechToText\": %s, \"textToSpeech\": %s, \"fullSpeech\": %s}",
            speechEnabled, speechEnabled, speechEnabled, speechEnabled);

    log.debug("Speech status: {}", status);
    return ResponseEntity.ok(status);
  }

  // Simple conversation history storage (for demo purposes)
  public record Message(String text, boolean isUser, boolean hasAudio) {}
}
