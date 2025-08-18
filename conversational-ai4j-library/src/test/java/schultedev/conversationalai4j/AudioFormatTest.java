package schultedev.conversationalai4j;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class AudioFormatTest {

  @Test
  void testWavFormatDetection() {
    // Create a simple WAV header
    byte[] wavHeader = {
      'R', 'I', 'F', 'F',   // RIFF
      0x24, 0, 0, 0,        // File size
      'W', 'A', 'V', 'E',   // WAVE
      'f', 'm', 't', ' ',   // fmt chunk
      0x10, 0, 0, 0,        // fmt chunk size
    };
    
    AudioFormat detected = AudioFormat.detect(wavHeader);
    assertEquals(AudioFormat.Type.WAV, detected.getType());
    assertEquals(16000, detected.getSampleRate());
    assertEquals(1, detected.getChannels());
    assertEquals(16, detected.getBitsPerSample());
  }

  @Test
  void testWebMFormatDetection() {
    // WebM magic bytes
    byte[] webmHeader = {
      0x1A, 0x45, (byte) 0xDF, (byte) 0xA3
    };
    
    AudioFormat detected = AudioFormat.detect(webmHeader);
    assertEquals(AudioFormat.Type.WEBM_OPUS, detected.getType());
    assertEquals(48000, detected.getSampleRate());
  }

  @Test
  void testUnknownFormatDetection() {
    byte[] unknownData = { 0x00, 0x01, 0x02, 0x03 };
    
    AudioFormat detected = AudioFormat.detect(unknownData);
    assertEquals(AudioFormat.Type.RAW_PCM, detected.getType());
    assertEquals(16000, detected.getSampleRate());
  }

  @Test
  void testEmptyDataDetection() {
    byte[] emptyData = {};
    
    AudioFormat detected = AudioFormat.detect(emptyData);
    assertEquals(AudioFormat.Type.UNKNOWN, detected.getType());
  }

  @Test
  void testPreDefinedFormats() {
    AudioFormat wav = AudioFormat.wav16kMono();
    assertEquals(AudioFormat.Type.WAV, wav.getType());
    assertEquals(16000, wav.getSampleRate());
    assertEquals(1, wav.getChannels());

    AudioFormat webm = AudioFormat.webmOpus();
    assertEquals(AudioFormat.Type.WEBM_OPUS, webm.getType());
    assertEquals(48000, webm.getSampleRate());

    AudioFormat pcm = AudioFormat.rawPcm16kMono();
    assertEquals(AudioFormat.Type.RAW_PCM, pcm.getType());
    assertEquals(16000, pcm.getSampleRate());
  }

  @Test
  void testToString() {
    AudioFormat format = AudioFormat.wav16kMono();
    String str = format.toString();
    assertTrue(str.contains("WAV"));
    assertTrue(str.contains("16000"));
    assertTrue(str.contains("1"));
    assertTrue(str.contains("16"));
  }
}