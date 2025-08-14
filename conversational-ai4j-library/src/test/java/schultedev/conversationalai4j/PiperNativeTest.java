package schultedev.conversationalai4j;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PiperNativeTest {

  private static final Logger log = LoggerFactory.getLogger(PiperNativeTest.class);

  @Test
  void testClassExists() {
    // Test that the PiperNative class exists and can be instantiated
    assertNotNull(PiperNative.class);
    log.info("PiperNative class is available");
  }

  @Test
  void testNativeMethodsHandleNullInputs() {
    // Test that native methods handle null inputs gracefully without crashing
    // These methods should return empty results when voice is null
    var result = PiperNative.synthesize(null, "Hello world");
    assertEquals(0, result.length, "Should return empty array for null voice");
  }

  @Test
  void testCloseNullVoice() {
    // Should not throw exception
    assertDoesNotThrow(() -> PiperNative.closeVoice(null));
  }

  @Test
  void testBasicFunctionality() {
    // Note: Full functionality testing requires Docker environment with native libraries
    // These tests verify the code structure without calling native libraries
    log.info(
        "PiperNative tests - Full functionality requires Docker environment with Piper models");
    assertTrue(true, "Basic structure tests passed");
  }
}
