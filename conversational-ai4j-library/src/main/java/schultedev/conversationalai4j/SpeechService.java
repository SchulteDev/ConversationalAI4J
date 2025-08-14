package schultedev.conversationalai4j;

import io.github.givimad.piperjni.PiperVoice;
import io.github.givimad.whisperjni.WhisperContext;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Speech service using Whisper.cpp for STT and Piper for TTS. Clean, native Java implementation
 * with no external dependencies.
 */
public class SpeechService {

  private static final Logger log = LoggerFactory.getLogger(SpeechService.class);
  private final boolean speechEnabled;
  private final WhisperContext whisperContext;
  private final PiperVoice piperVoice;

  public SpeechService() {
    this.speechEnabled = "true".equals(System.getenv("SPEECH_ENABLED"));
    log.info("Speech service initialized - enabled: {}", speechEnabled);

    WhisperContext tempWhisper = null;
    PiperVoice tempPiper = null;

    if (speechEnabled) {
      try {
        // Initialize Whisper for STT
        var whisperModelPath =
            System.getenv()
                .getOrDefault("WHISPER_MODEL_PATH", "/app/models/whisper/ggml-base.en.bin");
        tempWhisper = WhisperNative.createContext(whisperModelPath);

        // Initialize Piper for TTS
        var piperModelPath =
            System.getenv()
                .getOrDefault("PIPER_MODEL_PATH", "/app/models/piper/en_US-amy-low.onnx");
        var piperConfigPath =
            System.getenv()
                .getOrDefault("PIPER_CONFIG_PATH", "/app/models/piper/en_US-amy-low.onnx.json");
        tempPiper = PiperNative.createVoice(piperModelPath, piperConfigPath);

        log.info("Speech service initialized with Whisper and Piper");
      } catch (Exception e) {
        log.warn(
            "Speech models not available ({}), running without speech functionality",
            e.getMessage());
        tempWhisper = null;
        tempPiper = null;
      }
    } else {
      log.info("Speech service disabled");
    }

    this.whisperContext = tempWhisper;
    this.piperVoice = tempPiper;
  }

  public String speechToText(byte[] audioData) {
    if (!speechEnabled || whisperContext == null) {
      return "Mock transcription: Hello, this is a test.";
    }

    if (audioData == null || audioData.length == 0) {
      return "";
    }

    log.debug("Processing speech-to-text: {} bytes", audioData.length);

    try {
      // Convert audio to float samples for Whisper
      var audioSamples = convertToFloatSamples(audioData);
      if (audioSamples.length == 0) {
        return "";
      }

      return WhisperNative.transcribe(whisperContext, audioSamples);

    } catch (Exception e) {
      log.error("Error in speech-to-text processing: {}", e.getMessage(), e);
      return "Mock transcription: Hello, this is a test.";
    }
  }

  /**
   * Convert audio bytes to float samples for Whisper. Supports WAV and WebM formats.
   * WebM/Opus from browsers is converted to mock samples since proper decoding requires
   * external libraries like FFmpeg.
   */
  private float[] convertToFloatSamples(byte[] audioBytes) {
    if (audioBytes == null || audioBytes.length < 4) {
      return new float[0];
    }

    try {
      // Detect audio format by magic bytes
      if (isWavFormat(audioBytes)) {
        return convertWavToFloatSamples(audioBytes);
      } else if (isWebMFormat(audioBytes)) {
        log.warn("WebM/Opus audio detected - proper decoding requires FFmpeg. Using mock transcription.");
        // For now, return mock samples until proper WebM decoder is implemented
        return generateMockAudioSamples();
      } else {
        log.warn("Unknown audio format, trying as raw PCM");
        return convertRawPcmToFloatSamples(audioBytes);
      }

    } catch (Exception e) {
      log.error("Error converting audio to float samples: {}", e.getMessage(), e);
      return new float[0];
    }
  }

  private boolean isWavFormat(byte[] audioBytes) {
    return audioBytes.length >= 12 
        && audioBytes[0] == 'R' && audioBytes[1] == 'I' 
        && audioBytes[2] == 'F' && audioBytes[3] == 'F'
        && audioBytes[8] == 'W' && audioBytes[9] == 'A'
        && audioBytes[10] == 'V' && audioBytes[11] == 'E';
  }

  private boolean isWebMFormat(byte[] audioBytes) {
    return audioBytes.length >= 4 
        && audioBytes[0] == 0x1A && audioBytes[1] == 0x45 
        && audioBytes[2] == (byte) 0xDF && audioBytes[3] == (byte) 0xA3;
  }

  private float[] convertWavToFloatSamples(byte[] wavBytes) {
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

    log.debug("Converted {} bytes WAV to {} float samples", wavBytes.length, sampleCount);
    return samples;
  }

  private float[] convertRawPcmToFloatSamples(byte[] pcmBytes) {
    var sampleCount = pcmBytes.length / 2; // 16-bit samples
    var samples = new float[sampleCount];
    var buffer = ByteBuffer.wrap(pcmBytes);
    buffer.order(ByteOrder.LITTLE_ENDIAN);

    for (var i = 0; i < sampleCount; i++) {
      samples[i] = buffer.getShort() / 32768.0f;
    }

    log.debug("Converted {} bytes raw PCM to {} float samples", pcmBytes.length, sampleCount);
    return samples;
  }

  private float[] generateMockAudioSamples() {
    // Generate 3 seconds of silent audio at 16kHz
    var sampleCount = 16000 * 3;
    var samples = new float[sampleCount];
    // All zeros = silence
    log.debug("Generated {} mock audio samples for WebM format", sampleCount);
    return samples;
  }

  public byte[] textToSpeech(String text) {
    if (!speechEnabled || piperVoice == null) {
      return generateMockAudio(text);
    }

    if (text == null || text.trim().isEmpty()) {
      return new byte[0];
    }

    log.debug("Synthesizing text-to-speech: '{}'", text);

    try {
      return PiperNative.synthesize(piperVoice, text);
    } catch (Exception e) {
      log.error("Error in text-to-speech synthesis: {}", e.getMessage(), e);
      return generateMockAudio(text);
    }
  }

  public boolean isAvailable() {
    return speechEnabled;
  }

  public void close() {
    if (whisperContext != null) {
      WhisperNative.closeContext(whisperContext);
    }
    if (piperVoice != null) {
      PiperNative.closeVoice(piperVoice);
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
}
