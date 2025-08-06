package schultedev.conversationalai4j.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

class DemoApplicationTest {

  @Test
  void run() {
    int exitCode = new CommandLine(new DemoApplication()).execute();
    assertEquals(0, exitCode);
  }
}
