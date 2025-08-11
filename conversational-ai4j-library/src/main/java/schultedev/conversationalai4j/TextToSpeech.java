package schultedev.conversationalai4j;

import java.nio.file.Path;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Text-to-Speech service for privacy-first local processing.
 * Simple, clean interface that uses external speech tools.
 */
public class TextToSpeech {

  private static final Logger log = LoggerFactory.getLogger(TextToSpeech.class);
  private final SpeechService speechService;
  private final String modelPath;
  private final String language;
  private final String voice;

  /**
   * Creates a new TextToSpeech instance.
   *
   * @param modelPath Path to the TTS model (for logging/info purposes)
   * @param language Language code for the model (e.g., "en-US")
   * @param voice Voice identifier (e.g., "female", "male")
   */
  public TextToSpeech(Path modelPath, String language, String voice) {
    this.speechService = new SpeechService();
    this.modelPath = Objects.requireNonNull(modelPath, "Model path cannot be null").toString();
    this.language = Objects.requireNonNull(language, "Language cannot be null");
    this.voice = Objects.requireNonNull(voice, "Voice cannot be null");

    log.info("TextToSpeech initialized for model: {} ({}, {})", modelPath, language, voice);
  }

  /**
   * Synthesizes speech from text input.
   *
   * @param text Text to convert to speech
   * @return Audio data in WAV format
   * @throws IllegalArgumentException if text is null or empty
   */
  public byte[] synthesize(String text) {
    if (text == null || text.trim().isEmpty()) {
      throw new IllegalArgumentException("Text cannot be null or empty");
    }

    log.debug("Synthesizing speech for text: '{}'", text);
    return speechService.textToSpeech(text);
  }

  /**
   * Synthesizes speech with speed parameter (for API compatibility).
   * Currently speed parameter is ignored, but method is kept for backward compatibility.
   */
  public byte[] synthesize(String text, double speed, double pitch) {
    return synthesize(text);
  }

  /**
   * Checks if the text-to-speech service is ready.
   */
  public boolean isReady() {
    return speechService.isAvailable();
  }

  /**
   * Gets the language code.
   */
  public String getLanguage() {
    return language;
  }

  /**
   * Gets the voice identifier.
   */
  public String getVoice() {
    return voice;
  }

  /**
   * Gets the model path.
   */
  public Path getModelPath() {
    return Path.of(modelPath);
  }

  /**
   * No cleanup needed in this simple implementation.
   */
  public void close() {
    log.debug("TextToSpeech closed");
  }
}