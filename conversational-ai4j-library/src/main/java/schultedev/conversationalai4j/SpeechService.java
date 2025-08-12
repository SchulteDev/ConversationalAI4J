package schultedev.conversationalai4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Speech service that integrates with sherpa-onnx for real speech processing. Uses real STT/TTS
 * when available, falls back to mock on unsupported platforms.
 */
public class SpeechService {

  private static final Logger log = LoggerFactory.getLogger(SpeechService.class);
  private final boolean speechEnabled;
  private final long sttRecognizer;
  private final long ttsSynthesizer;

  public SpeechService() {
    this.speechEnabled = "true".equals(System.getenv("SPEECH_ENABLED"));
    log.info("Speech service initialized - enabled: {}", speechEnabled);

    if (speechEnabled && SherpaOnnxNative.isNativeLibraryAvailable()) {
      // Initialize sherpa-onnx recognizers
      var sttModelPath = System.getenv().getOrDefault("STT_MODEL_PATH", "/app/models/stt");
      var ttsModelPath = System.getenv().getOrDefault("TTS_MODEL_PATH", "/app/models/tts");

      this.sttRecognizer = SherpaOnnxNative.createSttRecognizer(sttModelPath, "en-US");
      this.ttsSynthesizer = SherpaOnnxNative.createTtsSynthesizer(ttsModelPath, "en-US", "female");

      log.info(
          "sherpa-onnx recognizers initialized - STT: {}, TTS: {}",
          sttRecognizer > 0,
          ttsSynthesizer > 0);
    } else {
      this.sttRecognizer = 0L;
      this.ttsSynthesizer = 0L;

      if (speechEnabled) {
        log.info("Speech enabled but sherpa-onnx not available - using mock implementation");
      }
    }
  }

  public String speechToText(byte[] audioData) {
    if (!speechEnabled) {
      return "Mock transcription: Hello, this is a test.";
    }

    log.info("Processing speech-to-text: {} bytes", (audioData == null ? 0 : audioData.length));

    // Always attempt to normalize to 16kHz mono PCM WAV using ffmpeg when available
    var normalized = convertToPcm16Wav(audioData);

    if (normalized == null) {
      log.warn("Audio normalization returned null");
      return "";
    }

    if (sttRecognizer > 0) {
      // Use real sherpa-onnx transcription
      log.info("Processing {} bytes with sherpa-onnx STT", normalized.length);
      return SherpaOnnxNative.transcribeAudio(sttRecognizer, normalized);
    } else {
      // Use Python sherpa-onnx or mock
      log.info("Processing {} bytes with Python sherpa-onnx STT", normalized.length);
      return SherpaOnnxNative.transcribeAudio(1L, normalized);
    }
  }

  /**
   * Convert arbitrary audio container (e.g., WebM/Opus from browser) to 16kHz mono PCM WAV. Falls
   * back to original bytes if conversion fails. Requires ffmpeg in PATH (provided in Docker).
   */
  private byte[] convertToPcm16Wav(byte[] input) {
    if (input == null || input.length == 0) return input;

    // Quick sniffing for container/header
    if (input.length >= 12) {
      var isWav =
          input[0] == 'R'
              && input[1] == 'I'
              && input[2] == 'F'
              && input[3] == 'F'
              && input[8] == 'W'
              && input[9] == 'A'
              && input[10] == 'V'
              && input[11] == 'E';
      log.debug("Input header: {} ({} bytes)", (isWav ? "WAV/RIFF" : "unknown"), input.length);
      if (isWav) {
        try {
          var hdr = parseWavHeader(input);
          log.debug(
              "Input WAV header: sr={} Hz, ch={}, bits={}",
              hdr.sampleRate,
              hdr.channels,
              hdr.bitsPerSample);
        } catch (Exception ex) {
          log.debug("Failed to parse input WAV header: {}", ex.getMessage());
        }
      }
    }

    var t0 = System.nanoTime();
    try {
      var pb =
          new ProcessBuilder(
              "ffmpeg",
              "-hide_banner",
              "-loglevel",
              "error",
              "-i",
              "pipe:0",
              "-ar",
              "16000",
              "-ac",
              "1",
              "-f",
              "wav",
              "pipe:1");
      var p = pb.start();

      // Write input bytes to ffmpeg stdin
      try (var stdin = p.getOutputStream()) {
        stdin.write(input);
        stdin.flush();
      }

      // Read converted bytes from stdout
      var output = p.getInputStream().readAllBytes();
      var err = p.getErrorStream().readAllBytes();

      var finished = p.waitFor(10, java.util.concurrent.TimeUnit.SECONDS);
      var dtMs = (System.nanoTime() - t0) / 1_000_000;
      if (!finished) {
        p.destroyForcibly();
        log.warn(
            "ffmpeg conversion timed out after {} ms; using original audio ({} bytes)",
            dtMs,
            input.length);
        return input;
      }

      var exit = p.exitValue();
      if (exit != 0 || output.length < 44) {
        var errMsg = new String(err);
        log.warn(
            "ffmpeg conversion failed in {} ms (code {}), stderr: {}. Using original audio ({} bytes)",
            dtMs,
            exit,
            errMsg,
            input.length);
        return input;
      }

      // Verify WAV header
      if (output[0] == 'R'
          && output[1] == 'I'
          && output[2] == 'F'
          && output[3] == 'F'
          && output[8] == 'W'
          && output[9] == 'A'
          && output[10] == 'V'
          && output[11] == 'E') {
        try {
          var hdr = parseWavHeader(output);
          log.debug(
              "Audio normalized via ffmpeg ({} ms): {} -> {} bytes, sr={} Hz, ch={}, bits={} ",
              dtMs,
              input.length,
              output.length,
              hdr.sampleRate,
              hdr.channels,
              hdr.bitsPerSample);
        } catch (Exception ex) {
          log.debug(
              "Audio normalized via ffmpeg ({} ms): {} -> {} bytes (header parse failed: {})",
              dtMs,
              input.length,
              output.length,
              ex.getMessage());
        }
        return output;
      } else {
        log.warn("ffmpeg output is not WAV; using original audio ({} bytes)", input.length);
        return input;
      }

    } catch (Exception e) {
      var dtMs = (System.nanoTime() - t0) / 1_000_000;
      log.warn(
          "ffmpeg conversion error after {} ms: {}. Using original audio ({} bytes)",
          dtMs,
          e.getMessage(),
          input.length);
      return input;
    }
  }

  private WavHeaderInfo parseWavHeader(byte[] wav) {
    if (wav.length < 44) throw new IllegalArgumentException("WAV too short");
    var info = new WavHeaderInfo();
    info.channels = ((wav[23] & 0xFF) << 8) | (wav[22] & 0xFF);
    info.sampleRate =
        ((wav[27] & 0xFF) << 24)
            | ((wav[26] & 0xFF) << 16)
            | ((wav[25] & 0xFF) << 8)
            | (wav[24] & 0xFF);
    info.bitsPerSample = ((wav[35] & 0xFF) << 8) | (wav[34] & 0xFF);
    return info;
  }

  public byte[] textToSpeech(String text) {
    if (!speechEnabled) {
      return generateMockAudio(text);
    }

    if (ttsSynthesizer > 0) {
      // Use real sherpa-onnx synthesis
      log.info("Synthesizing with sherpa-onnx TTS: '{}'", text);
      return SherpaOnnxNative.synthesizeSpeech(ttsSynthesizer, text);
    } else {
      // Use Python sherpa-onnx or mock
      log.info("Synthesizing with Python sherpa-onnx TTS: '{}'", text);
      return SherpaOnnxNative.synthesizeSpeech(1L, text);
    }
  }

  public boolean isAvailable() {
    return speechEnabled;
  }

  public void close() {
    if (sttRecognizer > 0) {
      SherpaOnnxNative.releaseSttRecognizer(sttRecognizer);
    }
    if (ttsSynthesizer > 0) {
      SherpaOnnxNative.releaseTtsSynthesizer(ttsSynthesizer);
    }
    log.debug("Speech service resources released");
  }

  private byte[] generateMockAudio(String text) {
    // Generate audio length based on text length (roughly 200ms per word)
    var wordCount = text == null || text.trim().isEmpty() ? 1 : text.split("\\s+").length;
    var durationMs = Math.max(500, wordCount * 200); // Minimum 500ms
    var sampleCount = (int) (16000 * durationMs / 1000.0); // 16kHz sample rate
    var wavData = new byte[44 + sampleCount * 2]; // 16-bit samples

    // WAV header for 16kHz, 16-bit, mono
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

  private void writeInt32LE(byte[] data, int offset, int value) {
    data[offset] = (byte) (value & 0xFF);
    data[offset + 1] = (byte) ((value >> 8) & 0xFF);
    data[offset + 2] = (byte) ((value >> 16) & 0xFF);
    data[offset + 3] = (byte) ((value >> 24) & 0xFF);
  }

  private void writeInt16LE(byte[] data, int offset, short value) {
    data[offset] = (byte) (value & 0xFF);
    data[offset + 1] = (byte) ((value >> 8) & 0xFF);
  }

  private static class WavHeaderInfo {
    int sampleRate;
    int channels;
    int bitsPerSample;
  }
}
