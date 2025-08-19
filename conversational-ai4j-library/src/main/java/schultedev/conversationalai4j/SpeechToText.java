package schultedev.conversationalai4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Speech-to-Text service for privacy-first local processing. Simple, clean interface that uses
 * external speech tools.
 */
public class SpeechToText {

  private static final Logger log = LoggerFactory.getLogger(SpeechToText.class);
  private final SpeechService speechService;

  /**
   * Creates a new SpeechToText instance.
   */
  public SpeechToText() {
    this.speechService = new SpeechService();
    log.info("SpeechToText initialized");
  }

  /**
   * Transcribes audio data to text.
   *
   * @param audioData Raw audio data (WAV format preferred)
   * @return Transcribed text, or error message if transcription failed
   * @throws IllegalArgumentException if audioData is null or empty
   */
  public String transcribe(byte[] audioData) {
    if (audioData == null || audioData.length == 0) {
      throw new IllegalArgumentException("Audio data cannot be null or empty");
    }

    log.debug("Transcribing {} bytes of audio data", audioData.length);
    return speechService.speechToText(audioData);
  }

  /** Checks if the speech-to-text service is ready. */
  public boolean isReady() {
    return speechService.isAvailable();
  }

  /** No cleanup needed in this simple implementation. */
  public void close() {
    log.debug("SpeechToText closed");
  }
}
