package schultedev.conversationalai4j;

/**
 * Audio format specification for processing various audio types. Supports format detection and
 * conversion parameters.
 */
public record AudioFormat(Type type, int sampleRate, int channels, int bitsPerSample) {

  public static AudioFormat wav16kMono() {
    return new AudioFormat(Type.WAV, 16000, 1, 16);
  }

  public static AudioFormat webmOpus() {
    return new AudioFormat(Type.WEBM_OPUS, 48000, 1, 16);
  }

  public static AudioFormat rawPcm16kMono() {
    return new AudioFormat(Type.RAW_PCM, 16000, 1, 16);
  }

  public static AudioFormat detect(byte[] audioData) {
    if (audioData == null || audioData.length < 4) {
      return new AudioFormat(Type.UNKNOWN, 16000, 1, 16);
    }

    // Detect WAV format
    if (audioData.length >= 12
        && audioData[0] == 'R'
        && audioData[1] == 'I'
        && audioData[2] == 'F'
        && audioData[3] == 'F'
        && audioData[8] == 'W'
        && audioData[9] == 'A'
        && audioData[10] == 'V'
        && audioData[11] == 'E') {
      return wav16kMono();
    }

    // Detect WebM format
    if (audioData.length >= 4
        && audioData[0] == 0x1A
        && audioData[1] == 0x45
        && audioData[2] == (byte) 0xDF
        && audioData[3] == (byte) 0xA3) {
      return webmOpus();
    }

    // Default to raw PCM
    return rawPcm16kMono();
  }

  @Override
  public String toString() {
    return String.format(
        "AudioFormat{type=%s, sampleRate=%d, channels=%d, bitsPerSample=%d}",
        type, sampleRate, channels, bitsPerSample);
  }

  public enum Type {
    WAV,
    WEBM_OPUS,
    RAW_PCM,
    UNKNOWN
  }
}
