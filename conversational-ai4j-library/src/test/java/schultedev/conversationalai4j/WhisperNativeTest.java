package schultedev.conversationalai4j;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

class WhisperNativeTest {

  @Test
  void testIsAvailable() {
    // Should not throw exception
    assertDoesNotThrow(WhisperNative::isAvailable);
  }

  @Test
  @EnabledIf("schultedev.conversationalai4j.WhisperNativeTest#isWhisperAvailable")
  void testInitialize() {
    assertTrue(WhisperNative.initialize());
  }

  @Test
  void testCreateContextWithInvalidPath() {
    if (WhisperNative.isAvailable()) {
      assertThrows(
          RuntimeException.class, () -> WhisperNative.createContext("/nonexistent/model.bin"));
    }
  }

  @Test
  void testTranscribeWithNullContext() {
    if (WhisperNative.isAvailable()) {
      var audioSamples = new float[1000]; // Mock audio
      // Should handle null context gracefully
      assertDoesNotThrow(() -> WhisperNative.transcribe(null, audioSamples));
    }
  }

  @Test
  void testCloseNullContext() {
    // Should not throw exception
    assertDoesNotThrow(() -> WhisperNative.closeContext(null));
  }

  static boolean isWhisperAvailable() {
    return WhisperNative.isAvailable();
  }
}
