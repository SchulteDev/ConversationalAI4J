package schultedev.conversationalai4j;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SpeechServiceTest {

  @Test
  void textToSpeech_ShouldReturnValidWavData() {
    SpeechService service = new SpeechService();
    byte[] result = service.textToSpeech("Hello world");

    // Should return valid WAV data
    assertNotNull(result);
    assertTrue(result.length > 44); // At least WAV header size

    // Check WAV header
    assertEquals('R', result[0]);
    assertEquals('I', result[1]);
    assertEquals('F', result[2]);
    assertEquals('F', result[3]);
    assertEquals('W', result[8]);
    assertEquals('A', result[9]);
    assertEquals('V', result[10]);
    assertEquals('E', result[11]);
  }

  @Test
  void speechToText_ShouldReturnString() {
    SpeechService service = new SpeechService();
    String result = service.speechToText(new byte[100]);

    assertNotNull(result);
    assertFalse(result.isEmpty());
  }

  @Test
  void isAvailable_ShouldReturnBoolean() {
    SpeechService service = new SpeechService();

    // Should return a boolean value
    assertNotNull(service.isAvailable());
  }
}
