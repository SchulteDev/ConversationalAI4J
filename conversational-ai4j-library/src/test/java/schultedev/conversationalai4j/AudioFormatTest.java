package schultedev.conversationalai4j;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AudioFormatTest {

  @Test
  void testWavFormatDetection() {
    // Create a simple WAV header
    byte[] wavHeader = {
      'R', 'I', 'F', 'F', // RIFF
      0x24, 0, 0, 0, // File size
      'W', 'A', 'V', 'E', // WAVE
      'f', 'm', 't', ' ', // fmt chunk
      0x10, 0, 0, 0, // fmt chunk size
    };

    var detected = AudioFormat.detect(wavHeader);
    assertEquals(AudioFormat.Type.WAV, detected.type());
    assertEquals(16000, detected.sampleRate());
    assertEquals(1, detected.channels());
    assertEquals(16, detected.bitsPerSample());
  }

  @Test
  void testWebMFormatDetection() {
    // WebM magic bytes
    byte[] webmHeader = {0x1A, 0x45, (byte) 0xDF, (byte) 0xA3};

    var detected = AudioFormat.detect(webmHeader);
    assertEquals(AudioFormat.Type.WEBM_OPUS, detected.type());
    assertEquals(48000, detected.sampleRate());
  }

  @Test
  void testUnknownFormatDetection() {
    byte[] unknownData = {0x00, 0x01, 0x02, 0x03};

    var detected = AudioFormat.detect(unknownData);
    assertEquals(AudioFormat.Type.RAW_PCM, detected.type());
    assertEquals(16000, detected.sampleRate());
  }

  @Test
  void testEmptyDataDetection() {
    byte[] emptyData = {};

    var detected = AudioFormat.detect(emptyData);
    assertEquals(AudioFormat.Type.UNKNOWN, detected.type());
  }

  @Test
  void testPreDefinedFormats() {
    var wav = AudioFormat.wav16kMono();
    assertEquals(AudioFormat.Type.WAV, wav.type());
    assertEquals(16000, wav.sampleRate());
    assertEquals(1, wav.channels());

    var webm = AudioFormat.webmOpus();
    assertEquals(AudioFormat.Type.WEBM_OPUS, webm.type());
    assertEquals(48000, webm.sampleRate());

    var pcm = AudioFormat.rawPcm16kMono();
    assertEquals(AudioFormat.Type.RAW_PCM, pcm.type());
    assertEquals(16000, pcm.sampleRate());
  }

  @Test
  void testToString() {
    var format = AudioFormat.wav16kMono();
    var str = format.toString();
    assertTrue(str.contains("WAV"));
    assertTrue(str.contains("16000"));
    assertTrue(str.contains("1"));
    assertTrue(str.contains("16"));
  }
}
