package schultedev.conversationalai4j;

import io.github.givimad.piperjni.PiperJNI;
import io.github.givimad.piperjni.PiperVoice;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Clean wrapper for Piper JNI for text-to-speech functionality. */
class PiperNative {

  private static final Logger log = LoggerFactory.getLogger(PiperNative.class);
  private static boolean libraryLoaded = false;
  private static PiperJNI piper;

  private PiperNative() {
    // Utility class - prevent instantiation
  }

  /** Initialize Piper library and load native components. */
  static synchronized boolean initialize() {
    if (libraryLoaded) {
      return true;
    }

    try {
      piper = new PiperJNI();
      piper.initialize(true, false); // Enable logging, disable debug
      libraryLoaded = true;
      log.info("Piper JNI library loaded successfully");
      return true;
    } catch (Exception e) {
      log.warn("Piper JNI library not available: {}", e.getMessage());
      return false;
    }
  }

  /** Creates a Piper voice for synthesis. */
  static PiperVoice createVoice(String modelPath, String configPath) {
    if (!initialize()) {
      throw new RuntimeException("Piper library not initialized");
    }

    try {
      var voice = piper.loadVoice(Paths.get(modelPath), Paths.get(configPath));
      log.info("Piper voice loaded from model: {}", modelPath);
      return voice;
    } catch (Exception e) {
      log.error("Failed to create Piper voice with model {}: {}", modelPath, e.getMessage(), e);
      throw new RuntimeException("Failed to create Piper voice", e);
    }
  }

  /** Synthesizes text to audio using Piper. */
  static byte[] synthesize(PiperVoice voice, String text) {
    if (!libraryLoaded || piper == null || voice == null) {
      return new byte[0];
    }

    if (text == null || text.trim().isEmpty()) {
      return new byte[0];
    }

    try {
      var audioSamples = piper.textToAudio(voice, text.trim());
      var wavData = convertToWav(audioSamples, 22050); // Piper default sample rate

      log.debug("Piper synthesized '{}' to {} bytes WAV", text, wavData.length);
      return wavData;

    } catch (Exception e) {
      log.error("Error during Piper synthesis for text '{}': {}", text, e.getMessage(), e);
      return new byte[0];
    }
  }

  /** Converts raw audio samples to WAV format. */
  private static byte[] convertToWav(short[] samples, int sampleRate) {
    var outputStream = new ByteArrayOutputStream();

    try {
      // WAV header
      outputStream.write("RIFF".getBytes());
      writeInt32LE(outputStream, 36 + samples.length * 2); // File size - 8
      outputStream.write("WAVE".getBytes());
      outputStream.write("fmt ".getBytes());
      writeInt32LE(outputStream, 16); // PCM format chunk size
      writeInt16LE(outputStream, 1); // PCM format
      writeInt16LE(outputStream, 1); // Mono
      writeInt32LE(outputStream, sampleRate);
      writeInt32LE(outputStream, sampleRate * 2); // Byte rate
      writeInt16LE(outputStream, 2); // Block align
      writeInt16LE(outputStream, 16); // Bits per sample
      outputStream.write("data".getBytes());
      writeInt32LE(outputStream, samples.length * 2); // Data size

      // Audio data
      var buffer = ByteBuffer.allocate(samples.length * 2);
      buffer.order(ByteOrder.LITTLE_ENDIAN);
      for (var sample : samples) {
        buffer.putShort(sample);
      }
      outputStream.write(buffer.array());

      return outputStream.toByteArray();
    } catch (Exception e) {
      log.error("Error converting samples to WAV: {}", e.getMessage(), e);
      return new byte[0];
    }
  }

  private static void writeInt32LE(ByteArrayOutputStream stream, int value) {
    stream.write(value & 0xFF);
    stream.write((value >> 8) & 0xFF);
    stream.write((value >> 16) & 0xFF);
    stream.write((value >> 24) & 0xFF);
  }

  private static void writeInt16LE(ByteArrayOutputStream stream, int value) {
    stream.write(value & 0xFF);
    stream.write((value >> 8) & 0xFF);
  }

  /** Closes and releases a Piper voice. */
  public static void closeVoice(PiperVoice voice) {
    if (voice != null) {
      try {
        voice.close();
        log.debug("Piper voice released");
      } catch (Exception e) {
        log.warn("Error closing Piper voice: {}", e.getMessage(), e);
      }
    }
  }
}
