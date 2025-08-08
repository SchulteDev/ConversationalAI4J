package schultedev.conversationalai4j;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ConversationMemoryTest {

  @Test
  void testSlidingMemory() {
    // When: Create sliding memory
    var memory = ConversationMemory.sliding(5);

    // Then: Should create successfully
    assertNotNull(memory);
  }

  @Test
  void testSlidingMemoryWithInvalidSize() {
    // When/Then: Should throw exception for invalid size
    var exception1 =
        assertThrows(IllegalArgumentException.class, () -> ConversationMemory.sliding(0));
    assertEquals("maxMessages must be positive", exception1.getMessage());

    var exception2 =
        assertThrows(IllegalArgumentException.class, () -> ConversationMemory.sliding(-1));
    assertEquals("maxMessages must be positive", exception2.getMessage());
  }

  @Test
  void testDefaultMemory() {
    // When: Create default memory
    var memory = ConversationMemory.defaultMemory();

    // Then: Should create successfully
    assertNotNull(memory);
  }
}
