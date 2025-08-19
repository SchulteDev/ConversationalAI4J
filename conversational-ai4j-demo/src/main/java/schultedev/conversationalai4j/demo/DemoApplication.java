package schultedev.conversationalai4j.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Main Spring Boot application class for the ConversationalAI4J demo. Provides embedded web server
 * capabilities for development and deployment.
 */
@SpringBootApplication
@EnableConfigurationProperties(AppConfig.class)
public class DemoApplication {

  public static void main(String[] args) {
    SpringApplication.run(DemoApplication.class, args);
  }
}
