package schultedev.conversationalai4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Text-to-Speech service for privacy-first local processing. Simple, clean interface that uses
 * external speech tools.
 */
public class TextToSpeech {

  private static final Logger log = LoggerFactory.getLogger(TextToSpeech.class);
  private final SpeechService speechService;

  /**
   * Creates a new TextToSpeech instance.
   */
  public TextToSpeech() {
    this.speechService = new SpeechService();
    log.info("TextToSpeech initialized");
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
   * Synthesizes speech with speed and pitch parameters (for API compatibility). Currently speed and
   * pitch parameters are ignored, but method is kept for backward compatibility.
   *
   * @param text the text to synthesize
   * @param speed the speed multiplier (currently ignored)
   * @param pitch the pitch adjustment (currently ignored)
   * @return the synthesized audio as WAV bytes
   */
  @SuppressWarnings("unused")
  public byte[] synthesize(String text, double speed, double pitch) {
    return synthesize(text);
  }

  /** Checks if the text-to-speech service is ready. */
  public boolean isReady() {
    return speechService.isAvailable();
  }

  /** No cleanup needed in this simple implementation. */
  public void close() {
    log.debug("TextToSpeech closed");
  }
}
