package schultedev.conversationalai4j;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

class MainTest {

  @Test
  void mainMethodRunsWithoutErrors() {
    // Test that main method runs without throwing exceptions
    assertDoesNotThrow(() -> Main.main(new String[]{}));
  }

}
