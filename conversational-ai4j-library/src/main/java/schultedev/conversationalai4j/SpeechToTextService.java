package schultedev.conversationalai4j;

import io.github.givimad.whisperjni.WhisperContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Speech-to-Text service for privacy-first local processing using Whisper.cpp. Provides simple,
 * clean interface for audio transcription with input validation.
 */
class SpeechToTextService {

  private static final Logger log = LoggerFactory.getLogger(SpeechToTextService.class);
  private final boolean enabled;
  private final WhisperContext whisperContext;

  SpeechToTextService() {
    this.enabled = SpeechServiceUtils.isSpeechEnabled();
    log.info("SpeechToTextService initialized - enabled: {}", enabled);

    WhisperContext tempContext = null;

    if (enabled) {
      try {
        var whisperModelPath = SpeechServiceUtils.getWhisperModelPath();
        tempContext = WhisperNative.createContext(whisperModelPath);
        log.info("Whisper STT service initialized successfully");
      } catch (Exception e) {
        log.warn("Whisper model not available ({}), STT will use mock responses", e.getMessage());
        tempContext = null;
      }
    } else {
      log.info("Speech-to-Text service disabled");
    }

    this.whisperContext = tempContext;
  }

  /**
   * Transcribes audio data to text.
   *
   * @param audioData Raw audio data (WAV format preferred)
   * @return Transcribed text, or error message if transcription failed
   * @throws IllegalArgumentException if audioData is null or empty
   */
  String transcribe(byte[] audioData) {
    if (audioData == null || audioData.length == 0) {
      throw new IllegalArgumentException("Audio data cannot be null or empty");
    }

    if (!enabled || whisperContext == null) {
      return SpeechServiceUtils.generateMockTranscription();
    }

    log.debug("Processing speech-to-text: {} bytes", audioData.length);

    try {
      var format = AudioFormat.detect(audioData);
      log.debug("Detected audio format: {}", format);

      var audioSamples = AudioProcessor.convertToFloatSamples(audioData, format);
      if (audioSamples.length == 0) {
        return "";
      }

      return WhisperNative.transcribe(whisperContext, audioSamples);

    } catch (Exception e) {
      log.error("Error in speech-to-text processing: {}", e.getMessage(), e);
      return SpeechServiceUtils.generateMockTranscription();
    }
  }

  /**
   * Enhanced transcription with explicit audio format specification.
   *
   * @param audioData Raw audio data
   * @param format Audio format specification
   * @return Transcribed text, or error message if transcription failed
   * @throws IllegalArgumentException if audioData is null or empty
   */
  String transcribe(byte[] audioData, AudioFormat format) {
    if (audioData == null || audioData.length == 0) {
      throw new IllegalArgumentException("Audio data cannot be null or empty");
    }

    if (!enabled || whisperContext == null) {
      return SpeechServiceUtils.generateMockTranscription();
    }

    log.debug("Processing speech-to-text: {} bytes with format {}", audioData.length, format);

    try {
      var audioSamples = AudioProcessor.convertToFloatSamples(audioData, format);
      if (audioSamples.length == 0) {
        return "";
      }

      return WhisperNative.transcribe(whisperContext, audioSamples);

    } catch (Exception e) {
      log.error("Error in speech-to-text processing: {}", e.getMessage(), e);
      return SpeechServiceUtils.generateMockTranscription();
    }
  }

  String transcribeFromChunks(java.util.List<byte[]> audioChunks) {
    if (audioChunks == null || audioChunks.isEmpty()) {
      return "";
    }

    log.debug("Processing {} audio chunks for transcription", audioChunks.size());

    var combinedAudio = AudioProcessor.combineAudioChunks(audioChunks);
    return transcribe(combinedAudio);
  }

  /** Checks if the speech-to-text service is ready. */
  boolean isReady() {
    return enabled && whisperContext != null;
  }

  boolean isAvailable() {
    return enabled && whisperContext != null;
  }

  void close() {
    if (whisperContext != null) {
      WhisperNative.closeContext(whisperContext);
    }
    log.debug("SpeechToTextService resources released");
  }
}
