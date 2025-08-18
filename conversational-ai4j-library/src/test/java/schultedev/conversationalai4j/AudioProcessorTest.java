package schultedev.conversationalai4j;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;

class AudioProcessorTest {

  @Test
  void testCombineAudioChunks() {
    byte[] chunk1 = {1, 2, 3, 4};
    byte[] chunk2 = {5, 6, 7, 8};
    byte[] chunk3 = {9, 10};
    
    List<byte[]> chunks = Arrays.asList(chunk1, chunk2, chunk3);
    byte[] combined = AudioProcessor.combineAudioChunks(chunks);
    
    assertEquals(10, combined.length);
    assertArrayEquals(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10}, combined);
  }

  @Test
  void testCombineEmptyChunks() {
    List<byte[]> emptyChunks = Arrays.asList();
    byte[] combined = AudioProcessor.combineAudioChunks(emptyChunks);
    
    assertEquals(0, combined.length);
  }

  @Test
  void testCombineWithNullChunks() {
    List<byte[]> chunksWithNull = Arrays.asList(
        new byte[]{1, 2}, 
        null, 
        new byte[]{3, 4}
    );
    byte[] combined = AudioProcessor.combineAudioChunks(chunksWithNull);
    
    assertEquals(4, combined.length);
    assertArrayEquals(new byte[]{1, 2, 3, 4}, combined);
  }

  @Test
  void testConvertToFloatSamples() {
    // Create simple WAV data with header
    byte[] wavData = createSimpleWavData();
    
    float[] samples = AudioProcessor.convertToFloatSamples(wavData, AudioFormat.wav16kMono());
    
    assertTrue(samples.length > 0);
    // Check that samples are normalized to [-1, 1]
    for (float sample : samples) {
      assertTrue(sample >= -1.0f && sample <= 1.0f);
    }
  }

  @Test
  void testConvertFromFloatSamples() {
    float[] samples = {0.5f, -0.5f, 0.0f, 1.0f, -1.0f};
    
    byte[] audioData = AudioProcessor.convertFromFloatSamples(samples, AudioFormat.wav16kMono());
    
    assertTrue(audioData.length > 44); // Should include WAV header
    // Check WAV header
    assertEquals('R', audioData[0]);
    assertEquals('I', audioData[1]);
    assertEquals('F', audioData[2]);
    assertEquals('F', audioData[3]);
    assertEquals('W', audioData[8]);
    assertEquals('A', audioData[9]);
    assertEquals('V', audioData[10]);
    assertEquals('E', audioData[11]);
  }

  @Test
  void testPreprocessAudio() {
    byte[] audioData = createSimpleWavData();
    AudioFormat format = AudioFormat.wav16kMono();
    
    byte[] preprocessed = AudioProcessor.preprocess(audioData, format);
    
    assertNotNull(preprocessed);
    assertTrue(preprocessed.length > 0);
  }

  @Test
  void testConvertFormats() {
    byte[] wavData = createSimpleWavData();
    AudioFormat wavFormat = AudioFormat.wav16kMono();
    AudioFormat pcmFormat = AudioFormat.rawPcm16kMono();
    
    byte[] converted = AudioProcessor.convert(wavData, wavFormat, pcmFormat);
    
    assertNotNull(converted);
    assertTrue(converted.length > 0);
    // PCM should be shorter than WAV (no header)
    assertTrue(converted.length < wavData.length);
  }

  @Test
  void testConvertSameFormat() {
    byte[] audioData = {1, 2, 3, 4};
    AudioFormat format = AudioFormat.rawPcm16kMono();
    
    byte[] converted = AudioProcessor.convert(audioData, format, format);
    
    // Should return same data for identical formats
    assertArrayEquals(audioData, converted);
  }

  private byte[] createSimpleWavData() {
    // Create a minimal valid WAV file with silent audio
    byte[] wavData = new byte[44 + 1000]; // Header + 500 samples
    
    // WAV header
    System.arraycopy("RIFF".getBytes(), 0, wavData, 0, 4);
    writeInt32LE(wavData, 4, wavData.length - 8);
    System.arraycopy("WAVE".getBytes(), 0, wavData, 8, 4);
    System.arraycopy("fmt ".getBytes(), 0, wavData, 12, 4);
    writeInt32LE(wavData, 16, 16); // fmt chunk size
    writeInt16LE(wavData, 20, (short) 1); // PCM format
    writeInt16LE(wavData, 22, (short) 1); // mono
    writeInt32LE(wavData, 24, 16000); // sample rate
    writeInt32LE(wavData, 28, 32000); // byte rate
    writeInt16LE(wavData, 32, (short) 2); // block align
    writeInt16LE(wavData, 34, (short) 16); // bits per sample
    System.arraycopy("data".getBytes(), 0, wavData, 36, 4);
    writeInt32LE(wavData, 40, 1000); // data size
    
    // Fill with some simple audio data (silence with a few samples)
    for (int i = 44; i < wavData.length; i += 2) {
      writeInt16LE(wavData, i, (short) (Math.sin((i - 44) * 0.1) * 1000));
    }
    
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