package schultedev.conversationalai4j.demo;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Paths;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Docker environment smoke test using Testcontainers - validates the complete ConversationalAI4J
 * system using the actual docker-compose.yml configuration.
 *
 * <p>This test boots the full Docker environment automatically using your docker-compose.yml and
 * runs comprehensive integration tests against the running containers. It's self-contained and
 * doesn't require a pre-existing Docker environment.
 */
@Testcontainers
class DockerSmokeTest {

  private static final Logger log = LoggerFactory.getLogger(DockerSmokeTest.class);

  private static final String OLLAMA_SERVICE = "ollama";
  private static final String DEMO_SERVICE = "demo";
  private static final int OLLAMA_PORT = 11434;
  private static final int DEMO_PORT = 8080;

  @Container
  static final ComposeContainer environment =
      new ComposeContainer(Paths.get("..").resolve("docker-compose.yml").toFile())
          .withExposedService(
              OLLAMA_SERVICE,
              OLLAMA_PORT,
              Wait.forHttp("/api/tags").withStartupTimeout(Duration.ofMinutes(3)))
          .withExposedService(
              DEMO_SERVICE,
              DEMO_PORT,
              Wait.forHttp("/actuator/health").withStartupTimeout(Duration.ofMinutes(5)))
          .withLogConsumer(
              OLLAMA_SERVICE,
              outputFrame -> log.debug("OLLAMA: {}", outputFrame.getUtf8String().strip()))
          .withLogConsumer(
              DEMO_SERVICE,
              outputFrame -> log.debug("DEMO: {}", outputFrame.getUtf8String().strip()))
          .withLocalCompose(true); // Use local docker-compose instead of embedded

  private final TestRestTemplate restTemplate = new TestRestTemplate();

  private String getDemoBaseUrl() {
    return "http://"
        + environment.getServiceHost(DEMO_SERVICE, DEMO_PORT)
        + ":"
        + environment.getServicePort(DEMO_SERVICE, DEMO_PORT);
  }

  private String getOllamaBaseUrl() {
    return "http://"
        + environment.getServiceHost(OLLAMA_SERVICE, OLLAMA_PORT)
        + ":"
        + environment.getServicePort(OLLAMA_SERVICE, OLLAMA_PORT);
  }

  @Test
  void applicationStartup_InDockerEnvironment_ShouldSucceed() {
    // When: Application starts up in Docker environment
    log.info("Testing application startup at: {}", getDemoBaseUrl());
    var response = restTemplate.getForEntity(getDemoBaseUrl() + "/", String.class);

    // Then: Should respond successfully
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().contains("ConversationalAI4J Demo"));
    log.info("✅ Application startup test passed");
  }

  @Test
  void ollamaIntegration_ShouldBeAccessible() {
    // First: Verify Ollama service is directly accessible
    log.info("Testing Ollama service at: {}", getOllamaBaseUrl());
    var ollamaResponse = restTemplate.getForEntity(getOllamaBaseUrl() + "/api/tags", String.class);
    assertEquals(HttpStatus.OK, ollamaResponse.getStatusCode(), "Ollama API should be accessible");
    log.info("Ollama API response: {}", ollamaResponse.getBody());

    // When: Check if Demo can communicate with Ollama (using proper form data)
    log.info("Testing Ollama integration through demo at: {}", getDemoBaseUrl());

    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
    formData.add("message", "Hello from Docker Compose");

    var headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    var requestEntity = new HttpEntity<MultiValueMap<String, String>>(formData, headers);

    var response =
        restTemplate.postForEntity(getDemoBaseUrl() + "/send", requestEntity, String.class);

    // Then: Should get response (either from AI or fallback)
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());

    // Response should contain either AI response or fallback message
    var body = response.getBody();
    var hasValidResponse =
        body.contains("Hello from Docker Compose")
            || body.contains("Echo")
            || body.contains("unavailable");
    assertTrue(hasValidResponse, "Should get some form of response to user message");
    log.info("✅ Ollama integration test passed");
  }

  @Test
  void actuatorHealth_ShouldShowSystemHealth() {
    // When: Check actuator health endpoint
    log.info("Testing actuator health at: {}", getDemoBaseUrl());
    var response = restTemplate.getForEntity(getDemoBaseUrl() + "/actuator/health", String.class);

    // Then: Should report system health
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().contains("UP"));
    log.info("✅ Actuator health test passed");
  }

  @Test
  void speechStatus_InDockerEnvironment_ShouldReportCorrectly() {
    // When: Check speech services status
    log.info("Testing speech status at: {}", getDemoBaseUrl());
    var response = restTemplate.getForEntity(getDemoBaseUrl() + "/speech-status", String.class);

    // Then: Should report speech status
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());

    // Should be valid JSON with availability info
    var jsonResponse = response.getBody();
    assertTrue(jsonResponse.contains("available"));
    assertTrue(jsonResponse.contains("speechToText"));
    assertTrue(jsonResponse.contains("textToSpeech"));

    // In Docker environment, speech may or may not be available
    // depending on sherpa-onnx setup - just verify the endpoint works
    log.info("✅ Speech status test passed");
  }

  @Test
  void conversationFlow_WithDockerCompose_ShouldWork() {
    // Given: Test conversation messages
    var testMessages = new String[] {"Hello Docker!", "Test message"};

    // When: Send multiple messages in sequence with proper form encoding
    log.info(
        "Testing conversation flow with {} messages at: {}", testMessages.length, getDemoBaseUrl());
    for (var message : testMessages) {
      MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
      formData.add("message", message);

      var headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
      var requestEntity = new HttpEntity<MultiValueMap<String, String>>(formData, headers);

      var response =
          restTemplate.postForEntity(getDemoBaseUrl() + "/send", requestEntity, String.class);

      // Then: Each should get a response
      assertEquals(
          HttpStatus.OK, response.getStatusCode(), "Message '" + message + "' should get response");
      assertNotNull(response.getBody());

      // Response should contain the message and some form of response
      var body = response.getBody();
      assertTrue(body.contains(message), "Response should contain original message: " + message);
    }
    log.info("✅ Conversation flow test passed");
  }
}
