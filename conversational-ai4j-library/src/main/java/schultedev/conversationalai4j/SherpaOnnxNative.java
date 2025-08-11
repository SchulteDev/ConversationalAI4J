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
    var os = System.getProperty("os.name").toLowerCase();
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
      log.trace("Native STT recognizer not available - using Python mode");
      return 1L; // Mock handle for Python mode
    }

    // If we're in Docker with Python sherpa-onnx, skip native and use Python
    if (System.getenv("SPEECH_ENABLED") != null) {
      log.info("Using Python sherpa-onnx for STT with model: {}", modelPath);
      return 1L; // Mock handle for Python mode
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
      log.trace("Native TTS synthesizer not available - using Python mode");
      return 1L; // Mock handle for Python mode
    }

    // If we're in Docker with Python sherpa-onnx, skip native and use Python
    if (System.getenv("SPEECH_ENABLED") != null) {
      log.info("Using Python sherpa-onnx for TTS with model: {}", modelPath);
      return 1L; // Mock handle for Python mode
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
    var sampleRate = 16000;
    var duration = Math.max(1, text.length() / 10);
    var numSamples = sampleRate * duration;

    var wavData = new byte[44 + numSamples * 2];

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
    var frequency = 440 + (text.hashCode() % 200);
    for (var i = 0; i < numSamples; i++) {
      var sample = Math.sin(2 * Math.PI * frequency * i / sampleRate) * 0.3;
      var sampleValue = (short) (sample * Short.MAX_VALUE);
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

      var sttModelPath = System.getenv("STT_MODEL_PATH");
      if (sttModelPath == null) {
        log.warn("STT_MODEL_PATH not set for Python transcription");
        return "STT model path not configured";
      }

      // Call Python sherpa-onnx STT
      var pb =
          new ProcessBuilder(
              "python3",
              "-c",
              String.format(
                  "import sherpa_onnx; "
                      + "recognizer = sherpa_onnx.OnlineRecognizer.from_transducer('%s/tokens.txt', '%s/encoder-epoch-99-avg-1-chunk-16-left-128.int8.onnx', '%s/decoder-epoch-99-avg-1-chunk-16-left-128.int8.onnx', '%s/joiner-epoch-99-avg-1-chunk-16-left-128.int8.onnx'); "
                      + "stream = recognizer.create_stream(); "
                      + "import soundfile; "
                      + "data, sr = soundfile.read('%s'); "
                      + "stream.accept_waveform(sr, data); "
                      + "recognizer.decode_stream(stream); "
                      + "print(stream.result.text)",
                  sttModelPath, sttModelPath, sttModelPath, sttModelPath, tempFile));

      var process = pb.start();
      process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS);

      var result = new String(process.getInputStream().readAllBytes()).trim();
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
    // Check for simple shell TTS command first
    var ttsCommand = System.getenv("TTS_COMMAND");
    if (ttsCommand != null) {
      return synthesizeWithShellCommand(text, ttsCommand);
    }

    try {
      var ttsModelPath = System.getenv("TTS_MODEL_PATH");
      if (ttsModelPath == null) {
        log.warn("TTS_MODEL_PATH not set for Python synthesis");
        return generateMockWavData(text);
      }

      var tempFile = java.nio.file.Files.createTempFile("sherpa-tts", ".wav");

      // Call Python sherpa-onnx TTS (Piper model format)
      var pb =
          new ProcessBuilder(
              "python3",
              "-c",
              String.format(
                  "import sherpa_onnx; "
                      + "tts = sherpa_onnx.OfflineTts.from_piper('%s/en_US-amy-low.onnx', '%s/tokens.txt', data_dir='%s/espeak-ng-data'); "
                      + "audio = tts.generate('%s', speed=1.0); "
                      + "import soundfile; "
                      + "soundfile.write('%s', audio.samples, tts.sample_rate)",
                  ttsModelPath,
                  ttsModelPath,
                  ttsModelPath,
                  text.replace("'", "\\'"),
                  tempFile.toString()));

      var process = pb.start();
      process.waitFor(15, java.util.concurrent.TimeUnit.SECONDS);

      if (java.nio.file.Files.exists(tempFile) && java.nio.file.Files.size(tempFile) > 0) {
        var audioData = java.nio.file.Files.readAllBytes(tempFile);
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

  /** Synthesizes speech using a shell command (e.g., espeak). */
  private static byte[] synthesizeWithShellCommand(String text, String command) {
    try {
      var tempFile = java.nio.file.Files.createTempFile("shell-tts", ".wav");

      var pb = new ProcessBuilder(command, text, tempFile.toString());
      var process = pb.start();
      process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS);

      if (java.nio.file.Files.exists(tempFile) && java.nio.file.Files.size(tempFile) > 0) {
        var audioData = java.nio.file.Files.readAllBytes(tempFile);
        java.nio.file.Files.deleteIfExists(tempFile);
        log.debug("Shell TTS generated {} bytes using command: {}", audioData.length, command);
        return audioData;
      } else {
        log.warn("Shell TTS command did not generate audio file: {}", command);
        java.nio.file.Files.deleteIfExists(tempFile);
        return generateMockWavData(text);
      }

    } catch (Exception e) {
      log.error("Error in shell TTS synthesis with command '{}': {}", command, e.getMessage(), e);
      return generateMockWavData(text);
    }
  }
}
