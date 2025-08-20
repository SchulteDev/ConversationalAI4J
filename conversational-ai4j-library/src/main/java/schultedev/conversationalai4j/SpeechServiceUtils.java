package schultedev.conversationalai4j;

import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared utilities for speech services including configuration, mocking, audio processing, and
 * advanced operations.
 *
 * <p>Provides both low-level utilities for service implementation and high-level methods for
 * advanced audio processing workflows.
 */
class SpeechServiceUtils {

  private static final Logger log = LoggerFactory.getLogger(SpeechServiceUtils.class);

  // Default model paths for programmatic configuration
  private static final String DEFAULT_WHISPER_MODEL_PATH = "/app/models/whisper/ggml-base.en.bin";
  private static final String DEFAULT_PIPER_MODEL_PATH = "/app/models/piper/en_US-amy-medium.onnx";
  private static final String DEFAULT_PIPER_CONFIG_PATH = "/app/models/piper/en_US-amy-medium.onnx.json";

  private SpeechServiceUtils() {
    // Utility class - prevent instantiation
  }

  /**
   * Get default Whisper model path for programmatic configuration.
   */
  static String getWhisperModelPath() {
    return DEFAULT_WHISPER_MODEL_PATH;
  }

  /**
   * Get default Piper model path for programmatic configuration.
   */
  static String getPiperModelPath() {
    return DEFAULT_PIPER_MODEL_PATH;
  }

  /**
   * Get default Piper config path for programmatic configuration.
   */
  static String getPiperConfigPath() {
    return DEFAULT_PIPER_CONFIG_PATH;
  }

  static String generateMockTranscription() {
    return "Mock transcription: Hello, this is a test.";
  }

  static byte[] generateMockAudio(String text) {
    var wordCount = text == null || text.trim().isEmpty() ? 1 : text.split("\\s+").length;
    var durationMs = Math.max(500, wordCount * 200);
    var sampleCount = (int) (16000 * durationMs / 1000.0);
    var wavData = new byte[44 + sampleCount * 2];

    System.arraycopy("RIFF".getBytes(), 0, wavData, 0, 4);
    writeInt32LE(wavData, 4, wavData.length - 8);
    System.arraycopy("WAVE".getBytes(), 0, wavData, 8, 4);
    System.arraycopy("fmt ".getBytes(), 0, wavData, 12, 4);
    writeInt32LE(wavData, 16, 16);
    writeInt16LE(wavData, 20, (short) 1);
    writeInt16LE(wavData, 22, (short) 1);
    writeInt32LE(wavData, 24, 16000);
    writeInt32LE(wavData, 28, 32000);
    writeInt16LE(wavData, 32, (short) 2);
    writeInt16LE(wavData, 34, (short) 16);
    System.arraycopy("data".getBytes(), 0, wavData, 36, 4);
    writeInt32LE(wavData, 40, 32000);

    return wavData;
  }

  private static void writeInt32LE(byte[] data, int offset, int value) {
    data[offset] = (byte) (value & 0xFF);
    data[offset + 1] = (byte) ((value >> 8) & 0xFF);
    data[offset + 2] = (byte) ((value >> 16) & 0xFF);
    data[offset + 3] = (byte) ((value >> 24) & 0xFF);
  }

  private static void writeInt16LE(byte[] data, int offset, short value) {
    data[offset] = (byte) (value & 0xFF);
    data[offset + 1] = (byte) ((value >> 8) & 0xFF);
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
      // Use the existing speech service from the ConversationalAI instance
      var speechService = ai.getSpeechToTextService();
      if (speechService == null) {
        throw new UnsupportedOperationException("Speech-to-text service not configured");
      }
      var text = speechService.transcribe(audioInput, format);
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
  static void speechToTextDebug(ConversationalAI ai, byte[] audioInput) {
    if (!ai.isSpeechEnabled()) {
      throw new UnsupportedOperationException(
          "Speech services are not configured. Use withSpeech() in builder.");
    }

    if (audioInput == null || audioInput.length == 0) {
      throw new IllegalArgumentException("Audio input cannot be null or empty");
    }

    log.info("Processing speech-to-text (debug mode): {} bytes", audioInput.length);

    try {
      var result = speechToText(ai, audioInput, AudioFormat.wav16kMono());
      log.info("Speech-to-text debug result: '{}'", result);
    } catch (Exception e) {
      log.error("Speech-to-text debug failed: {}", e.getMessage(), e);
    }
  }
}
