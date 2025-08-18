package schultedev.conversationalai4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Advanced audio processing utilities for format conversion, preprocessing,
 * and stream handling. Centralizes all audio manipulation logic.
 */
public class AudioProcessor {
  
  private static final Logger log = LoggerFactory.getLogger(AudioProcessor.class);
  
  /**
   * Converts audio data to the target format.
   */
  public static byte[] convert(byte[] audioData, AudioFormat sourceFormat, AudioFormat targetFormat) {
    if (audioData == null || audioData.length == 0) {
      return new byte[0];
    }
    
    log.debug("Converting audio from {} to {}", sourceFormat, targetFormat);
    
    // If formats are the same, return as-is
    if (sourceFormat.getType() == targetFormat.getType()) {
      return audioData;
    }
    
    try {
      // Convert to float samples first (common intermediate format)
      float[] samples = convertToFloatSamples(audioData, sourceFormat);
      
      // Convert from float samples to target format
      return convertFromFloatSamples(samples, targetFormat);
      
    } catch (Exception e) {
      log.error("Error converting audio format: {}", e.getMessage(), e);
      return audioData; // Return original on error
    }
  }
  
  /**
   * Preprocesses audio data with gain control, noise reduction, etc.
   */
  public static byte[] preprocess(byte[] audioData, AudioFormat format) {
    if (audioData == null || audioData.length == 0) {
      return audioData;
    }
    
    log.debug("Preprocessing {} bytes of audio", audioData.length);
    
    try {
      float[] samples = convertToFloatSamples(audioData, format);
      
      // Apply preprocessing
      samples = applyGainControl(samples);
      samples = normalizeAudio(samples);
      
      return convertFromFloatSamples(samples, format);
      
    } catch (Exception e) {
      log.error("Error preprocessing audio: {}", e.getMessage(), e);
      return audioData;
    }
  }
  
  /**
   * Combines multiple audio chunks into a single buffer.
   */
  public static byte[] combineAudioChunks(List<byte[]> chunks) {
    if (chunks == null || chunks.isEmpty()) {
      return new byte[0];
    }
    
    try (var output = new ByteArrayOutputStream()) {
      for (byte[] chunk : chunks) {
        if (chunk != null && chunk.length > 0) {
          output.write(chunk);
        }
      }
      
      byte[] combined = output.toByteArray();
      log.debug("Combined {} chunks into {} bytes", chunks.size(), combined.length);
      return combined;
      
    } catch (IOException e) {
      log.error("Error combining audio chunks: {}", e.getMessage(), e);
      return new byte[0];
    }
  }
  
  /**
   * Converts audio data to normalized float samples.
   */
  public static float[] convertToFloatSamples(byte[] audioData, AudioFormat format) {
    if (audioData == null || audioData.length == 0) {
      return new float[0];
    }
    
    switch (format.getType()) {
      case WAV:
        return convertWavToFloatSamples(audioData);
      case WEBM_OPUS:
        // For WebM/Opus, we'll convert the first chunk to see if it's actually WAV data
        // Many browsers send WebM header but the actual data might be in different format
        log.info("Processing WebM/Opus data - attempting smart conversion");
        return convertWebMToFloatSamples(audioData);
      case RAW_PCM:
        return convertRawPcmToFloatSamples(audioData);
      default:
        log.warn("Unknown audio format, trying as raw PCM");
        return convertRawPcmToFloatSamples(audioData);
    }
  }
  
  /**
   * Converts float samples back to audio format.
   */
  public static byte[] convertFromFloatSamples(float[] samples, AudioFormat format) {
    if (samples == null || samples.length == 0) {
      return new byte[0];
    }
    
    switch (format.getType()) {
      case WAV:
        return convertFloatSamplesToWav(samples, format);
      case RAW_PCM:
        return convertFloatSamplesToRawPcm(samples);
      default:
        log.warn("Conversion to {} not implemented, using raw PCM", format.getType());
        return convertFloatSamplesToRawPcm(samples);
    }
  }
  
  private static float[] convertWavToFloatSamples(byte[] wavBytes) {
    if (wavBytes.length < 44) {
      return new float[0];
    }
    
    // Skip WAV header (44 bytes) and read PCM data
    int dataSize = wavBytes.length - 44;
    int sampleCount = dataSize / 2; // 16-bit samples
    float[] samples = new float[sampleCount];
    
    ByteBuffer buffer = ByteBuffer.wrap(wavBytes, 44, dataSize);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    
    for (int i = 0; i < sampleCount; i++) {
      samples[i] = buffer.getShort() / 32768.0f; // Normalize to [-1, 1]
    }
    
    return samples;
  }
  
  private static float[] convertRawPcmToFloatSamples(byte[] pcmBytes) {
    int sampleCount = pcmBytes.length / 2; // 16-bit samples
    float[] samples = new float[sampleCount];
    ByteBuffer buffer = ByteBuffer.wrap(pcmBytes);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    
    for (int i = 0; i < sampleCount; i++) {
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
    if (webmBytes.length >= 4 && 
        webmBytes[0] == 0x1A && webmBytes[1] == 0x45 && 
        webmBytes[2] == (byte) 0xDF && webmBytes[3] == (byte) 0xA3) {
      log.info("Detected WebM container - using FFmpeg to decode properly");
      // Skip all heuristics and go straight to FFmpeg decoding
      return decodeWebMOpusWithFFmpeg(webmBytes);
    }
    
    // Real WebM/Opus requires external decoding using FFmpeg
    log.info("WebM/Opus audio detected - attempting FFmpeg decoding to PCM");
    return decodeWebMOpusWithFFmpeg(webmBytes);
  }
  
  private static boolean isWavFormat(byte[] audioBytes) {
    return audioBytes.length >= 12 
        && audioBytes[0] == 'R' && audioBytes[1] == 'I' 
        && audioBytes[2] == 'F' && audioBytes[3] == 'F'
        && audioBytes[8] == 'W' && audioBytes[9] == 'A'
        && audioBytes[10] == 'V' && audioBytes[11] == 'E';
  }
  
  private static double calculateRMS(float[] samples) {
    if (samples.length == 0) return 0.0;
    
    double sumSquares = 0;
    for (float sample : samples) {
      sumSquares += sample * sample;
    }
    return Math.sqrt(sumSquares / samples.length);
  }
  
  /**
   * Decodes WebM/Opus audio using FFmpeg to raw PCM samples.
   */
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
      ProcessBuilder processBuilder = new ProcessBuilder(
          "ffmpeg", 
          "-y",                          // Overwrite output files
          "-i", inputFile.toString(),    // Input WebM file
          "-f", "f32le",                 // Output format: 32-bit float little-endian
          "-acodec", "pcm_f32le",        // Audio codec: PCM float 32-bit
          "-ar", "16000",                // Sample rate: 16kHz  
          "-ac", "1",                    // Channels: mono
          outputFile.toString()          // Output PCM file
      );
      
      processBuilder.redirectErrorStream(true); // Merge stderr with stdout
      
      log.debug("Starting FFmpeg process to decode {} bytes of WebM/Opus audio", webmBytes.length);
      Process process = processBuilder.start();
      
      // Wait for FFmpeg to complete with timeout
      boolean finished = process.waitFor(30, java.util.concurrent.TimeUnit.SECONDS);
      
      if (!finished) {
        process.destroyForcibly();
        log.error("FFmpeg process timed out after 30 seconds");
        return new float[0];
      }
      
      int exitCode = process.exitValue();
      if (exitCode != 0) {
        // Read process output for debugging
        String output = new String(process.getInputStream().readAllBytes());
        log.error("FFmpeg failed with exit code {}: {}", exitCode, output);
        return new float[0];
      }
      
      // Read the decoded PCM data
      byte[] pcmBytes = java.nio.file.Files.readAllBytes(outputFile);
      
      if (pcmBytes.length == 0) {
        log.warn("FFmpeg produced no output for WebM/Opus decoding");
        return new float[0];
      }
      
      // Convert 32-bit float PCM bytes to float array
      java.nio.ByteBuffer buffer = java.nio.ByteBuffer.wrap(pcmBytes);
      buffer.order(java.nio.ByteOrder.LITTLE_ENDIAN);
      
      int sampleCount = pcmBytes.length / 4; // 4 bytes per float32 sample
      float[] samples = new float[sampleCount];
      
      for (int i = 0; i < sampleCount; i++) {
        samples[i] = buffer.getFloat();
      }
      
      log.info("Successfully decoded {} bytes WebM/Opus to {} float samples using FFmpeg", 
               webmBytes.length, samples.length);
      
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
  
  private static float[] generateLowLevelAudioPattern(int dataLength) {
    // Generate a low-level audio pattern that indicates "no speech" to Whisper
    // Base sample count on typical 16kHz rate
    int sampleCount = Math.max(16000, dataLength / 4); // At least 1 second of audio
    float[] samples = new float[sampleCount];
    
    // Generate very low level white noise that signals "no speech content"
    java.util.Random random = new java.util.Random();
    for (int i = 0; i < sampleCount; i++) {
      samples[i] = (random.nextFloat() - 0.5f) * 0.001f; // Very quiet noise
    }
    
    log.debug("Generated {} low-level audio samples as WebM fallback", sampleCount);
    return samples;
  }
  
  private static float[] generateNoisePattern(int dataLength) {
    // Generate a low-level noise pattern that won't trigger Whisper hallucinations
    // Base sample count on typical 16kHz rate
    int sampleCount = Math.max(16000, dataLength / 4); // At least 1 second of audio
    float[] samples = new float[sampleCount];
    
    // Generate very low level white noise
    java.util.Random random = new java.util.Random();
    for (int i = 0; i < sampleCount; i++) {
      samples[i] = (random.nextFloat() - 0.5f) * 0.001f; // Very quiet noise
    }
    
    log.debug("Generated {} noise samples for WebM data", sampleCount);
    return samples;
  }
  
  private static byte[] convertFloatSamplesToWav(float[] samples, AudioFormat format) {
    int sampleCount = samples.length;
    int dataSize = sampleCount * 2; // 16-bit samples
    byte[] wavData = new byte[44 + dataSize];
    
    // WAV header
    System.arraycopy("RIFF".getBytes(), 0, wavData, 0, 4);
    writeInt32LE(wavData, 4, wavData.length - 8);
    System.arraycopy("WAVE".getBytes(), 0, wavData, 8, 4);
    System.arraycopy("fmt ".getBytes(), 0, wavData, 12, 4);
    writeInt32LE(wavData, 16, 16); // fmt chunk size
    writeInt16LE(wavData, 20, (short) 1); // PCM format
    writeInt16LE(wavData, 22, (short) format.getChannels());
    writeInt32LE(wavData, 24, format.getSampleRate());
    writeInt32LE(wavData, 28, format.getSampleRate() * format.getChannels() * 2);
    writeInt16LE(wavData, 32, (short) (format.getChannels() * 2));
    writeInt16LE(wavData, 34, (short) 16);
    System.arraycopy("data".getBytes(), 0, wavData, 36, 4);
    writeInt32LE(wavData, 40, dataSize);
    
    // Convert samples to 16-bit PCM
    ByteBuffer buffer = ByteBuffer.wrap(wavData, 44, dataSize);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    
    for (float sample : samples) {
      // Clamp and convert to 16-bit
      sample = Math.max(-1.0f, Math.min(1.0f, sample));
      short pcmValue = (short) (sample * 32767);
      buffer.putShort(pcmValue);
    }
    
    return wavData;
  }
  
  private static byte[] convertFloatSamplesToRawPcm(float[] samples) {
    ByteBuffer buffer = ByteBuffer.allocate(samples.length * 2);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    
    for (float sample : samples) {
      sample = Math.max(-1.0f, Math.min(1.0f, sample));
      short pcmValue = (short) (sample * 32767);
      buffer.putShort(pcmValue);
    }
    
    return buffer.array();
  }
  
  private static float[] applyGainControl(float[] samples) {
    if (samples.length == 0) return samples;
    
    // Calculate RMS level
    double sumSquares = 0;
    for (float sample : samples) {
      sumSquares += sample * sample;
    }
    double rmsLevel = Math.sqrt(sumSquares / samples.length);
    
    // Apply gain if audio is too quiet
    if (rmsLevel < 0.01) { // Very quiet
      float gain = 3.0f;
      for (int i = 0; i < samples.length; i++) {
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
    for (float sample : samples) {
      peak = Math.max(peak, Math.abs(sample));
    }
    
    // Normalize if peak is too low or too high
    if (peak > 0.1f && peak < 0.9f) {
      float normalizationFactor = 0.8f / peak;
      for (int i = 0; i < samples.length; i++) {
        samples[i] *= normalizationFactor;
      }
      log.debug("Applied normalization: {}x factor for peak {}", normalizationFactor, peak);
    }
    
    return samples;
  }
  
  private static float[] generateSilentSamples(int sampleCount) {
    return new float[sampleCount]; // All zeros = silence
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