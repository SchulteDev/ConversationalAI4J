package schultedev.conversationalai4j;

import io.github.givimad.whisperjni.WhisperContext;
import io.github.givimad.whisperjni.WhisperFullParams;
import io.github.givimad.whisperjni.WhisperJNI;
import io.github.givimad.whisperjni.WhisperSamplingStrategy;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Clean wrapper for Whisper.cpp JNI for speech-to-text functionality. */
class WhisperNative {

  static final String DEFAULT_WHISPER_MODEL_PATH = "/app/models/whisper/ggml-base.en.bin";

  private static final Logger log = LoggerFactory.getLogger(WhisperNative.class);
  private static boolean libraryLoaded = false;
  private static WhisperJNI whisper;

  private WhisperNative() {
    // Utility class - prevent instantiation
  }

  /** Initialize Whisper library and load native components. */
  static synchronized boolean initialize() {
    if (libraryLoaded) {
      return true;
    }

    try {
      WhisperJNI.loadLibrary(log::debug);
      whisper = new WhisperJNI();
      libraryLoaded = true;
      log.info("Whisper.cpp JNI library loaded successfully");
      return true;
    } catch (Exception e) {
      log.warn("Whisper.cpp JNI library not available: {}", e.getMessage());
      return false;
    }
  }

  /** Creates a Whisper context for transcription. */
  static WhisperContext createContext(String modelPath) {
    if (!initialize()) {
      throw new RuntimeException("Whisper library not initialized");
    }

    try {
      var context = whisper.init(Path.of(modelPath));
      log.info("Whisper context created with model: {}", modelPath);
      return context;
    } catch (Exception e) {
      log.error("Failed to create Whisper context with model {}: {}", modelPath, e.getMessage(), e);
      throw new RuntimeException("Failed to create Whisper context", e);
    }
  }

  /** Transcribes audio using Whisper. */
  static String transcribe(WhisperContext context, float[] audioSamples) {
    if (!libraryLoaded || whisper == null || context == null) {
      return "";
    }

    if (audioSamples == null || audioSamples.length == 0) {
      return "";
    }

    try {
      var params = new WhisperFullParams(WhisperSamplingStrategy.GREEDY);
      params.translate = false;
      params.printProgress = false;
      params.printTimestamps = false;
      params.printSpecial = false;

      var result = whisper.full(context, params, audioSamples, audioSamples.length);
      if (result != 0) {
        log.warn("Whisper transcription returned non-zero result: {}", result);
      }

      var segmentCount = whisper.fullNSegments(context);
      if (segmentCount == 0) {
        return "";
      }

      var transcription = new StringBuilder();
      for (var i = 0; i < segmentCount; i++) {
        var text = whisper.fullGetSegmentText(context, i);
        if (text != null && !text.trim().isEmpty()) {
          transcription.append(text.trim()).append(" ");
        }
      }

      var finalText = transcription.toString().trim();
      log.debug("Whisper transcribed {} samples to: '{}'", audioSamples.length, finalText);
      return finalText;

    } catch (Exception e) {
      log.error("Error during Whisper transcription: {}", e.getMessage(), e);
      return "";
    }
  }

  /** Closes and releases a Whisper context. */
  static void closeContext(WhisperContext context) {
    if (context != null && whisper != null) {
      try {
        context.close();
        log.debug("Whisper context released");
      } catch (Exception e) {
        log.warn("Error closing Whisper context: {}", e.getMessage(), e);
      }
    }
  }

  /** Check if Whisper library is available. */
  static boolean isAvailable() {
    return initialize();
  }
}
