package schultedev.conversationalai4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Advanced audio processing utilities for format conversion, preprocessing, and stream handling.
 * Centralizes all audio manipulation logic.
 */
class AudioProcessor {

  private static final Logger log = LoggerFactory.getLogger(AudioProcessor.class);

  private AudioProcessor() {
    // Utility class - prevent instantiation
  }

  /** Converts audio data to the target format. */
  static byte[] convert(byte[] audioData, AudioFormat sourceFormat, AudioFormat targetFormat) {
    if (audioData == null || audioData.length == 0) {
      return new byte[0];
    }

    log.debug("Converting audio from {} to {}", sourceFormat, targetFormat);

    // If formats are the same, return as-is
    if (sourceFormat.type() == targetFormat.type()) {
      return audioData;
    }

    try {
      // Convert to float samples first (common intermediate format)
      var samples = convertToFloatSamples(audioData, sourceFormat);

      // Convert from float samples to target format
      return convertFromFloatSamples(samples, targetFormat);

    } catch (Exception e) {
      log.error("Error converting audio format: {}", e.getMessage(), e);
      return audioData; // Return original on error
    }
  }

  /** Preprocesses audio data with gain control, noise reduction, etc. */
  static byte[] preprocess(byte[] audioData, AudioFormat format) {
    if (audioData == null || audioData.length == 0) {
      return audioData;
    }

    log.debug("Preprocessing {} bytes of audio", audioData.length);

    try {
      var samples = convertToFloatSamples(audioData, format);

      // Apply preprocessing (methods modify in-place but return for chaining)
      applyGainControl(samples);
      normalizeAudio(samples);

      return convertFromFloatSamples(samples, format);

    } catch (Exception e) {
      log.error("Error preprocessing audio: {}", e.getMessage(), e);
      return audioData;
    }
  }

  /** Combines multiple audio chunks into a single buffer. */
  static byte[] combineAudioChunks(List<byte[]> chunks) {
    if (chunks == null || chunks.isEmpty()) {
      return new byte[0];
    }

    try (var output = new ByteArrayOutputStream()) {
      for (var chunk : chunks) {
        if (chunk != null && chunk.length > 0) {
          output.write(chunk);
        }
      }

      var combined = output.toByteArray();
      log.debug("Combined {} chunks into {} bytes", chunks.size(), combined.length);
      return combined;

    } catch (IOException e) {
      log.error("Error combining audio chunks: {}", e.getMessage(), e);
      return new byte[0];
    }
  }

  /** Converts audio data to normalized float samples. */
  static float[] convertToFloatSamples(byte[] audioData, AudioFormat format) {
    if (audioData == null || audioData.length == 0) {
      return new float[0];
    }

    return switch (format.type()) {
      case WAV -> convertWavToFloatSamples(audioData);
      case WEBM_OPUS -> {
        // For WebM/Opus, we'll convert the first chunk to see if it's actually WAV data
        // Many browsers send WebM header but the actual data might be in different format
        log.debug("Processing WebM/Opus data");
        yield convertWebMToFloatSamples(audioData);
      }
      case RAW_PCM -> convertRawPcmToFloatSamples(audioData);
      default -> {
        log.warn("Unknown audio format, trying as raw PCM");
        yield convertRawPcmToFloatSamples(audioData);
      }
    };
  }

  /** Converts float samples back to audio format. */
  static byte[] convertFromFloatSamples(float[] samples, AudioFormat format) {
    if (samples == null || samples.length == 0) {
      return new byte[0];
    }

    return switch (format.type()) {
      case WAV -> convertFloatSamplesToWav(samples, format);
      case RAW_PCM -> convertFloatSamplesToRawPcm(samples);
      default -> {
        log.warn("Conversion to {} not implemented, using raw PCM", format.type());
        yield convertFloatSamplesToRawPcm(samples);
      }
    };
  }

  private static float[] convertWavToFloatSamples(byte[] wavBytes) {
    if (wavBytes.length < 44) {
      return new float[0];
    }

    // Skip WAV header (44 bytes) and read PCM data
    var dataSize = wavBytes.length - 44;
    var sampleCount = dataSize / 2; // 16-bit samples
    var samples = new float[sampleCount];

    var buffer = ByteBuffer.wrap(wavBytes, 44, dataSize);
    buffer.order(ByteOrder.LITTLE_ENDIAN);

    for (var i = 0; i < sampleCount; i++) {
      samples[i] = buffer.getShort() / 32768.0f; // Normalize to [-1, 1]
    }

    return samples;
  }

  private static float[] convertRawPcmToFloatSamples(byte[] pcmBytes) {
    var sampleCount = pcmBytes.length / 2; // 16-bit samples
    var samples = new float[sampleCount];
    var buffer = ByteBuffer.wrap(pcmBytes);
    buffer.order(ByteOrder.LITTLE_ENDIAN);

    for (var i = 0; i < sampleCount; i++) {
      samples[i] = buffer.getShort() / 32768.0f;
    }

    return samples;
  }

  private static float[] convertWebMToFloatSamples(byte[] webmBytes) {
    log.debug("Attempting WebM/Opus conversion for {} bytes", webmBytes.length);

    // First, check if this might actually be WAV data with WebM mime type
    if (isWavFormat(webmBytes)) {
      log.info("WebM data is actually WAV format - converting as WAV");
      return convertWavToFloatSamples(webmBytes);
    }

    // Check for WebM container
    if (webmBytes.length >= 4
        && webmBytes[0] == 0x1A
        && webmBytes[1] == 0x45
        && webmBytes[2] == (byte) 0xDF
        && webmBytes[3] == (byte) 0xA3) {
      log.info("Detected WebM container - using FFmpeg to decode properly");
      // Skip all heuristics and go straight to FFmpeg decoding
      return decodeWebMOpusWithFFmpeg(webmBytes);
    }

    // Real WebM/Opus requires external decoding using FFmpeg
    log.debug("Using FFmpeg to decode WebM/Opus audio");
    return decodeWebMOpusWithFFmpeg(webmBytes);
  }

  private static boolean isWavFormat(byte[] audioBytes) {
    return audioBytes.length >= 12
        && audioBytes[0] == 'R'
        && audioBytes[1] == 'I'
        && audioBytes[2] == 'F'
        && audioBytes[3] == 'F'
        && audioBytes[8] == 'W'
        && audioBytes[9] == 'A'
        && audioBytes[10] == 'V'
        && audioBytes[11] == 'E';
  }

  /** Decodes WebM/Opus audio using FFmpeg to raw PCM samples. */
  private static float[] decodeWebMOpusWithFFmpeg(byte[] webmBytes) {
    java.nio.file.Path inputFile = null;
    java.nio.file.Path outputFile = null;

    try {
      // Create temporary files for FFmpeg processing
      inputFile = java.nio.file.Files.createTempFile("audio_input", ".webm");
      outputFile = java.nio.file.Files.createTempFile("audio_output", ".pcm");

      // Write WebM data to temporary input file
      java.nio.file.Files.write(inputFile, webmBytes);

      // Use FFmpeg to decode WebM/Opus to 16kHz mono PCM
      var processBuilder =
          new ProcessBuilder(
              "ffmpeg",
              "-y", // Overwrite output files
              "-i",
              inputFile.toString(), // Input WebM file
              "-f",
              "f32le", // Output format: 32-bit float little-endian
              "-acodec",
              "pcm_f32le", // Audio codec: PCM float 32-bit
              "-ar",
              "16000", // Sample rate: 16kHz
              "-ac",
              "1", // Channels: mono
              outputFile.toString() // Output PCM file
              );

      processBuilder.redirectErrorStream(true); // Merge stderr with stdout

      log.debug("Starting FFmpeg process to decode {} bytes of WebM/Opus audio", webmBytes.length);
      var process = processBuilder.start();

      // Wait for FFmpeg to complete with timeout
      var finished = process.waitFor(30, java.util.concurrent.TimeUnit.SECONDS);

      if (!finished) {
        process.destroyForcibly();
        log.error("FFmpeg process timed out after 30 seconds");
        return new float[0];
      }

      var exitCode = process.exitValue();
      if (exitCode != 0) {
        // Read process output for debugging
        var output = new String(process.getInputStream().readAllBytes());
        log.error("FFmpeg failed with exit code {}: {}", exitCode, output);
        return new float[0];
      }

      // Read the decoded PCM data
      var pcmBytes = java.nio.file.Files.readAllBytes(outputFile);

      if (pcmBytes.length == 0) {
        log.warn("FFmpeg produced no output for WebM/Opus decoding");
        return new float[0];
      }

      // Convert 32-bit float PCM bytes to float array
      var buffer = java.nio.ByteBuffer.wrap(pcmBytes);
      buffer.order(java.nio.ByteOrder.LITTLE_ENDIAN);

      var sampleCount = pcmBytes.length / 4; // 4 bytes per float32 sample
      var samples = new float[sampleCount];

      for (var i = 0; i < sampleCount; i++) {
        samples[i] = buffer.getFloat();
      }

      log.info(
          "Successfully decoded {} bytes WebM/Opus to {} float samples using FFmpeg",
          webmBytes.length,
          samples.length);

      return samples;

    } catch (Exception e) {
      log.error("Failed to decode WebM/Opus with FFmpeg: {}", e.getMessage(), e);
      return new float[0];
    } finally {
      // Clean up temporary files
      try {
        if (inputFile != null) java.nio.file.Files.deleteIfExists(inputFile);
        if (outputFile != null) java.nio.file.Files.deleteIfExists(outputFile);
      } catch (Exception e) {
        log.warn("Failed to clean up temporary files: {}", e.getMessage());
      }
    }
  }

  private static byte[] convertFloatSamplesToWav(float[] samples, AudioFormat format) {
    var sampleCount = samples.length;
    var dataSize = sampleCount * 2; // 16-bit samples
    var wavData = new byte[44 + dataSize];

    // WAV header
    System.arraycopy("RIFF".getBytes(), 0, wavData, 0, 4);
    writeInt32LE(wavData, 4, wavData.length - 8);
    System.arraycopy("WAVE".getBytes(), 0, wavData, 8, 4);
    System.arraycopy("fmt ".getBytes(), 0, wavData, 12, 4);
    writeInt32LE(wavData, 16, 16); // fmt chunk size
    writeInt16LE(wavData, 20, (short) 1); // PCM format
    writeInt16LE(wavData, 22, (short) format.channels());
    writeInt32LE(wavData, 24, format.sampleRate());
    writeInt32LE(wavData, 28, format.sampleRate() * format.channels() * 2);
    writeInt16LE(wavData, 32, (short) (format.channels() * 2));
    writeInt16LE(wavData, 34, (short) 16);
    System.arraycopy("data".getBytes(), 0, wavData, 36, 4);
    writeInt32LE(wavData, 40, dataSize);

    // Convert samples to 16-bit PCM
    var buffer = ByteBuffer.wrap(wavData, 44, dataSize);
    buffer.order(ByteOrder.LITTLE_ENDIAN);

    for (var sample : samples) {
      // Clamp and convert to 16-bit
      sample = Math.max(-1.0f, Math.min(1.0f, sample));
      var pcmValue = (short) (sample * 32767);
      buffer.putShort(pcmValue);
    }

    return wavData;
  }

  private static byte[] convertFloatSamplesToRawPcm(float[] samples) {
    var buffer = ByteBuffer.allocate(samples.length * 2);
    buffer.order(ByteOrder.LITTLE_ENDIAN);

    for (var sample : samples) {
      sample = Math.max(-1.0f, Math.min(1.0f, sample));
      var pcmValue = (short) (sample * 32767);
      buffer.putShort(pcmValue);
    }

    return buffer.array();
  }

  private static float[] applyGainControl(float[] samples) {
    if (samples.length == 0) return samples;

    // Calculate RMS level
    double sumSquares = 0;
    for (var sample : samples) {
      sumSquares += sample * sample;
    }
    var rmsLevel = Math.sqrt(sumSquares / samples.length);

    // Apply gain if audio is too quiet
    if (rmsLevel < 0.01) { // Very quiet
      var gain = 3.0f;
      for (var i = 0; i < samples.length; i++) {
        samples[i] = Math.max(-1.0f, Math.min(1.0f, samples[i] * gain));
      }
      log.debug("Applied gain control: {}x gain for RMS level {}", gain, rmsLevel);
    }

    return samples;
  }

  private static float[] normalizeAudio(float[] samples) {
    if (samples.length == 0) return samples;

    // Find peak amplitude
    float peak = 0;
    for (var sample : samples) {
      peak = Math.max(peak, Math.abs(sample));
    }

    // Normalize if peak is too low or too high
    if (peak > 0.1f && peak < 0.9f) {
      var normalizationFactor = 0.8f / peak;
      for (var i = 0; i < samples.length; i++) {
        samples[i] *= normalizationFactor;
      }
      log.debug("Applied normalization: {}x factor for peak {}", normalizationFactor, peak);
    }

    return samples;
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
}
