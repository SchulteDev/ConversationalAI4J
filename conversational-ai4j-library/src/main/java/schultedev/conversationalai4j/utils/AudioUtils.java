package schultedev.conversationalai4j.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import schultedev.conversationalai4j.AudioFormat;
import schultedev.conversationalai4j.ConversationalAI;
import schultedev.conversationalai4j.SpeechService;

/**
 * Utility methods for advanced audio processing operations.
 *
 * <p>This class provides specialized audio processing functionality that goes beyond the core
 * ConversationalAI API. Use these methods when you need fine-grained control over audio formats and
 * processing.
 */
public class AudioUtils {

  private static final Logger log = LoggerFactory.getLogger(AudioUtils.class);

  private AudioUtils() {
    // Utility class - prevent instantiation
  }

  /**
   * Convert speech to text with explicit format specification.
   *
   * <p>This method provides more control over audio format handling compared to the standard
   * ConversationalAI speech processing.
   *
   * @param ai ConversationalAI instance with speech configured
   * @param audioInput Raw audio data
   * @param format Audio format specification
   * @return Transcribed text, or error message if transcription failed
   * @throws UnsupportedOperationException if speech is not configured
   * @throws IllegalArgumentException if audio input is null or empty
   */
  public static String speechToText(ConversationalAI ai, byte[] audioInput, AudioFormat format) {
    if (!ai.isSpeechEnabled()) {
      throw new UnsupportedOperationException(
          "Speech services are not configured. Use withSpeech() in builder.");
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

  /**
   * Convert speech to text only (no chat processing) - for debugging/testing.
   *
   * <p>This method performs speech-to-text conversion without any AI chat processing. Useful for
   * testing speech recognition capabilities or debugging audio issues.
   *
   * @param ai ConversationalAI instance with speech configured
   * @param audioInput Raw audio data in WAV format (16kHz, 16-bit, mono)
   * @throws UnsupportedOperationException if speech is not configured
   * @throws IllegalArgumentException if audio input is null or empty
   */
  public static void speechToTextDebug(ConversationalAI ai, byte[] audioInput) {
    if (!ai.isSpeechEnabled()) {
      throw new UnsupportedOperationException(
          "Speech services are not configured. Use withSpeech() in builder.");
    }

    if (audioInput == null || audioInput.length == 0) {
      throw new IllegalArgumentException("Audio input cannot be null or empty");
    }

    log.info("Processing speech-to-text (debug mode): {} bytes", audioInput.length);

    // This would need access to internal speechToText field
    // For now, we'll use the public API as a workaround
    try {
      String result = speechToText(ai, audioInput, AudioFormat.wav16kMono());
      log.info("Speech-to-text debug result: '{}'", result);
    } catch (Exception e) {
      log.error("Speech-to-text debug failed: {}", e.getMessage(), e);
    }
  }
}
