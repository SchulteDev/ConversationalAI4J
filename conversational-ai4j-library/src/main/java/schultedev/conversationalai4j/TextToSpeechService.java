package schultedev.conversationalai4j;

import io.github.givimad.piperjni.PiperVoice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Text-to-Speech service for privacy-first local processing using Piper. Provides simple, clean
 * interface for speech synthesis with input validation.
 */
class TextToSpeechService {

  private static final Logger log = LoggerFactory.getLogger(TextToSpeechService.class);
  private final boolean enabled;
  private final PiperVoice piperVoice;

  TextToSpeechService(SpeechConfig speechConfig) {
    this.enabled = speechConfig != null && speechConfig.isEnabled();
    log.info("TextToSpeechService initialized - enabled: {} (with config)", enabled);

    PiperVoice tempVoice = null;

    if (enabled && speechConfig != null) {
      try {
        var piperModelPath =
            speechConfig.getTtsModelPath() != null
                ? speechConfig.getTtsModelPath().toString()
                : SpeechServiceUtils.getPiperModelPath();
        var piperConfigPath = SpeechServiceUtils.getPiperConfigPath();
        tempVoice = PiperNative.createVoice(piperModelPath, piperConfigPath);
        log.info("Piper TTS service initialized successfully with model: {}", piperModelPath);
      } catch (Exception e) {
        log.warn("Piper model not available ({}), TTS will use mock audio", e.getMessage());
        tempVoice = null;
      }
    } else {
      log.info("Text-to-Speech service disabled");
    }

    this.piperVoice = tempVoice;
  }

  /**
   * Synthesizes speech from text input.
   *
   * @param text Text to convert to speech
   * @return Audio data in WAV format
   * @throws IllegalArgumentException if text is null or empty
   */
  byte[] synthesize(String text) {
    if (text == null || text.trim().isEmpty()) {
      throw new IllegalArgumentException("Text cannot be null or empty");
    }

    if (!enabled || piperVoice == null) {
      return SpeechServiceUtils.generateMockAudio(text);
    }

    log.debug("Synthesizing text-to-speech: '{}'", text);

    try {
      return PiperNative.synthesize(piperVoice, text);
    } catch (Exception e) {
      log.error("Error in text-to-speech synthesis: {}", e.getMessage(), e);
      return SpeechServiceUtils.generateMockAudio(text);
    }
  }

  /** Checks if the text-to-speech service is ready. */
  boolean isReady() {
    return enabled && piperVoice != null;
  }

  void close() {
    if (piperVoice != null) {
      PiperNative.closeVoice(piperVoice);
    }
    log.debug("TextToSpeechService resources released");
  }
}
