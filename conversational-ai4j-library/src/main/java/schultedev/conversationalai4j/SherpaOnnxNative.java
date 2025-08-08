package schultedev.conversationalai4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Native interface to sherpa-onnx libraries. Supports Linux platforms only. Falls back to mock
 * implementation on other platforms.
 */
public class SherpaOnnxNative {

  private static final Logger log = LoggerFactory.getLogger(SherpaOnnxNative.class);

  private static Boolean nativeLibraryAvailable = null;
  private static boolean initializationAttempted = false;

  /**
   * Checks if sherpa-onnx native libraries are available. Only available on Linux platforms.
   *
   * @return true if native libraries are loaded and available
   */
  public static synchronized boolean isNativeLibraryAvailable() {
    if (nativeLibraryAvailable == null) {
      nativeLibraryAvailable = tryLoadNativeLibrary();
    }
    return nativeLibraryAvailable;
  }

  /**
   * Attempts to load the sherpa-onnx native library.
   *
   * @return true if library was loaded successfully
   */
  private static boolean tryLoadNativeLibrary() {
    if (initializationAttempted) {
      return nativeLibraryAvailable != null ? nativeLibraryAvailable : false;
    }

    initializationAttempted = true;

    // Platform check - only support Linux
    String os = System.getProperty("os.name").toLowerCase();
    if (!os.contains("linux")) {
      log.info(
          "Speech functionality requires Linux platform. Current OS: {}. Using mock implementation.",
          os);
      return false;
    }

    try {
      // Check if sherpa-onnx Python package is available (Docker setup)
      if (System.getenv("SPEECH_ENABLED") != null) {
        // In Docker with sherpa-onnx Python package installed
        log.info("sherpa-onnx environment detected - speech functionality enabled");
        return true;
      }

      // Try to load sherpa-onnx library (system installation)
      System.loadLibrary("sherpa-onnx");
      log.info("Successfully loaded sherpa-onnx native library on Linux");
      return true;

    } catch (UnsatisfiedLinkError e) {
      log.warn(
          "sherpa-onnx native library not available on Linux: {}. Using mock implementation.",
          e.getMessage());
      log.info("To enable real speech: Install sherpa-onnx system package or use Docker setup");
      return false;
    } catch (Exception e) {
      log.warn(
          "Error loading sherpa-onnx native library: {}. Using mock implementation.",
          e.getMessage(),
          e);
      return false;
    }
  }

  // Native method declarations for sherpa-onnx JNI
  // These will only be available when native library is loaded

  /**
   * Creates a speech-to-text recognizer.
   *
   * @param modelPath Path to the STT model
   * @param language Language code
   * @return Native recognizer handle, or 0 if not available
   */
  public static long createSttRecognizer(String modelPath, String language) {
    if (!isNativeLibraryAvailable()) {
      log.trace("Native STT recognizer not available - using mock");
      return 1L; // Mock handle
    }

    try {
      return nativeCreateSttRecognizer(modelPath, language);
    } catch (UnsatisfiedLinkError e) {
      log.warn("Failed to create native STT recognizer: {}", e.getMessage());
      return 0L;
    }
  }

  /**
   * Transcribes audio using sherpa-onnx.
   *
   * @param recognizer Recognizer handle from createSttRecognizer
   * @param audioData Raw audio data (16kHz, 16-bit, mono)
   * @return Transcribed text
   */
  public static String transcribeAudio(long recognizer, byte[] audioData) {
    if (!isNativeLibraryAvailable() || recognizer == 1L) {
      // Mock implementation
      log.trace("Using mock STT transcription for {} bytes", audioData.length);
      return "Hello, this is a mock transcription from sherpa-onnx.";
    }

    // Check if we're using Python sherpa-onnx (Docker environment)
    if (System.getenv("SPEECH_ENABLED") != null) {
      return transcribeWithPython(audioData);
    }

    try {
      return nativeTranscribeAudio(recognizer, audioData);
    } catch (UnsatisfiedLinkError e) {
      log.warn("Failed to transcribe audio: {}", e.getMessage());
      return ""; // Fallback to empty string
    }
  }

  /**
   * Creates a text-to-speech synthesizer.
   *
   * @param modelPath Path to the TTS model
   * @param language Language code
   * @param voice Voice identifier
   * @return Native synthesizer handle, or 0 if not available
   */
  public static long createTtsSynthesizer(String modelPath, String language, String voice) {
    if (!isNativeLibraryAvailable()) {
      log.trace("Native TTS synthesizer not available - using mock");
      return 1L; // Mock handle
    }

    try {
      return nativeCreateTtsSynthesizer(modelPath, language, voice);
    } catch (UnsatisfiedLinkError e) {
      log.warn("Failed to create native TTS synthesizer: {}", e.getMessage());
      return 0L;
    }
  }

  /**
   * Synthesizes speech using sherpa-onnx.
   *
   * @param synthesizer Synthesizer handle from createTtsSynthesizer
   * @param text Text to convert to speech
   * @return Audio data in WAV format
   */
  public static byte[] synthesizeSpeech(long synthesizer, String text) {
    if (!isNativeLibraryAvailable() || synthesizer == 1L) {
      // Mock implementation - return the same mock WAV data as before
      log.trace("Using mock TTS synthesis for text: '{}'", text);
      return generateMockWavData(text);
    }

    // Check if we're using Python sherpa-onnx (Docker environment)
    if (System.getenv("SPEECH_ENABLED") != null) {
      return synthesizeWithPython(text);
    }

    try {
      return nativeSynthesizeSpeech(synthesizer, text);
    } catch (UnsatisfiedLinkError e) {
      log.warn("Failed to synthesize speech: {}", e.getMessage());
      return new byte[0];
    }
  }

  /**
   * Releases a recognizer handle.
   *
   * @param recognizer Recognizer handle to release
   */
  public static void releaseSttRecognizer(long recognizer) {
    if (isNativeLibraryAvailable() && recognizer != 1L) {
      try {
        nativeReleaseSttRecognizer(recognizer);
      } catch (UnsatisfiedLinkError e) {
        log.debug("Failed to release STT recognizer: {}", e.getMessage());
      }
    }
  }

  /**
   * Releases a synthesizer handle.
   *
   * @param synthesizer Synthesizer handle to release
   */
  public static void releaseTtsSynthesizer(long synthesizer) {
    if (isNativeLibraryAvailable() && synthesizer != 1L) {
      try {
        nativeReleaseTtsSynthesizer(synthesizer);
      } catch (UnsatisfiedLinkError e) {
        log.debug("Failed to release TTS synthesizer: {}", e.getMessage());
      }
    }
  }

  // Native method declarations - only available when JNI library is loaded
  private static native long nativeCreateSttRecognizer(String modelPath, String language);

  private static native String nativeTranscribeAudio(long recognizer, byte[] audioData);

  private static native void nativeReleaseSttRecognizer(long recognizer);

  private static native long nativeCreateTtsSynthesizer(
      String modelPath, String language, String voice);

  private static native byte[] nativeSynthesizeSpeech(long synthesizer, String text);

  private static native void nativeReleaseTtsSynthesizer(long synthesizer);

  // Mock WAV data generation (same as before)
  private static byte[] generateMockWavData(String text) {
    int sampleRate = 16000;
    int duration = Math.max(1, text.length() / 10);
    int numSamples = sampleRate * duration;

    byte[] wavData = new byte[44 + numSamples * 2];

    // WAV header
    System.arraycopy("RIFF".getBytes(), 0, wavData, 0, 4);
    writeInt32LE(wavData, 4, wavData.length - 8);
    System.arraycopy("WAVE".getBytes(), 0, wavData, 8, 4);
    System.arraycopy("fmt ".getBytes(), 0, wavData, 12, 4);
    writeInt32LE(wavData, 16, 16);
    writeInt16LE(wavData, 20, (short) 1);
    writeInt16LE(wavData, 22, (short) 1);
    writeInt32LE(wavData, 24, sampleRate);
    writeInt32LE(wavData, 28, sampleRate * 2);
    writeInt16LE(wavData, 32, (short) 2);
    writeInt16LE(wavData, 34, (short) 16);
    System.arraycopy("data".getBytes(), 0, wavData, 36, 4);
    writeInt32LE(wavData, 40, numSamples * 2);

    // Simple sine wave audio data
    int frequency = 440 + (text.hashCode() % 200);
    for (int i = 0; i < numSamples; i++) {
      double sample = Math.sin(2 * Math.PI * frequency * i / sampleRate) * 0.3;
      short sampleValue = (short) (sample * Short.MAX_VALUE);
      writeInt16LE(wavData, 44 + i * 2, sampleValue);
    }

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

  // Python-based speech processing for Docker environment

  /** Transcribes audio using Python sherpa-onnx CLI. */
  private static String transcribeWithPython(byte[] audioData) {
    try {
      // Write audio data to temporary file
      var tempFile = java.nio.file.Files.createTempFile("sherpa-audio", ".wav");
      java.nio.file.Files.write(tempFile, audioData);

      String sttModelPath = System.getenv("STT_MODEL_PATH");
      if (sttModelPath == null) {
        log.warn("STT_MODEL_PATH not set for Python transcription");
        return "STT model path not configured";
      }

      // Call Python sherpa-onnx STT
      ProcessBuilder pb =
          new ProcessBuilder(
              "python3",
              "-c",
              String.format(
                  "import sherpa_onnx; "
                      + "recognizer = sherpa_onnx.OnlineRecognizer.from_transducer('%s/tokens.txt', '%s/encoder.int8.onnx', '%s/decoder.int8.onnx', '%s/joiner.int8.onnx'); "
                      + "stream = recognizer.create_stream(); "
                      + "import soundfile; "
                      + "data, sr = soundfile.read('%s'); "
                      + "stream.accept_waveform(sr, data); "
                      + "recognizer.decode_stream(stream); "
                      + "print(stream.result.text)",
                  sttModelPath, sttModelPath, sttModelPath, sttModelPath, tempFile));

      Process process = pb.start();
      process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS);

      String result = new String(process.getInputStream().readAllBytes()).trim();
      java.nio.file.Files.deleteIfExists(tempFile);

      log.debug("Python STT result: '{}'", result);
      return result.isEmpty() ? "No speech detected" : result;

    } catch (Exception e) {
      log.error("Error in Python STT transcription: {}", e.getMessage(), e);
      return "Error in speech transcription";
    }
  }

  /** Synthesizes speech using Python sherpa-onnx CLI. */
  private static byte[] synthesizeWithPython(String text) {
    try {
      String ttsModelPath = System.getenv("TTS_MODEL_PATH");
      if (ttsModelPath == null) {
        log.warn("TTS_MODEL_PATH not set for Python synthesis");
        return generateMockWavData(text);
      }

      var tempFile = java.nio.file.Files.createTempFile("sherpa-tts", ".wav");

      // Call Python sherpa-onnx TTS
      ProcessBuilder pb =
          new ProcessBuilder(
              "python3",
              "-c",
              String.format(
                  "import sherpa_onnx; "
                      + "tts = sherpa_onnx.OfflineTts.from_vits('%s/model.onnx', '%s/lexicon.txt', '%s/tokens.txt'); "
                      + "audio = tts.generate('%s', speed=1.0); "
                      + "import soundfile; "
                      + "soundfile.write('%s', audio.samples, tts.sample_rate)",
                  ttsModelPath,
                  ttsModelPath,
                  ttsModelPath,
                  text.replace("'", "\\'"),
                  tempFile.toString()));

      Process process = pb.start();
      process.waitFor(15, java.util.concurrent.TimeUnit.SECONDS);

      if (java.nio.file.Files.exists(tempFile) && java.nio.file.Files.size(tempFile) > 0) {
        byte[] audioData = java.nio.file.Files.readAllBytes(tempFile);
        java.nio.file.Files.deleteIfExists(tempFile);
        log.debug("Python TTS generated {} bytes", audioData.length);
        return audioData;
      } else {
        log.warn("Python TTS did not generate audio file");
        java.nio.file.Files.deleteIfExists(tempFile);
        return generateMockWavData(text);
      }

    } catch (Exception e) {
      log.error("Error in Python TTS synthesis: {}", e.getMessage(), e);
      return generateMockWavData(text);
    }
  }
}
