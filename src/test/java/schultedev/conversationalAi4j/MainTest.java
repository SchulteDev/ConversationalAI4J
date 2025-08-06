package schultedev.conversationalAi4j;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MainTest {

  @Test
  void sayHelloAndCount() {
    assertTrue(new Main().sayHelloAndCount());
  }

}
