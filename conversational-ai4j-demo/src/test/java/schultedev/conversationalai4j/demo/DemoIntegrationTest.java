package schultedev.conversationalai4j.demo;

import static org.junit.jupiter.api.Assertions.*;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.util.LinkedMultiValueMap;

/**
 * Integration tests for the ConversationalAI4J demo web application. Tests the complete web
 * application functionality including Spring Boot server startup, Thymeleaf rendering, and user
 * interaction flows.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "ollama.base-url=http://localhost:99999", // Non-existing port to force echo mode
    "spring.main.lazy-initialization=true",
    "server.port=0" // Use random port to avoid conflicts
})
class DemoIntegrationTest {

  @LocalServerPort private int port;

  @Autowired private TestRestTemplate restTemplate;

  private String getBaseUrl() {
    return "http://localhost:" + port;
  }

  @Test
  void testApplicationStartup() {
    // When
    var response = restTemplate.getForEntity(getBaseUrl() + "/", String.class);

    // Then
    assertEquals(
        HttpStatus.OK, response.getStatusCode(), "Application should respond with HTTP 200");

    var content = response.getBody();
    assertNotNull(content, "Response content should not be null");
    assertTrue(
        content.contains("ConversationalAI4J Demo"), "Page should contain application title");
  }

  @Test
  void testConversationPageRendering() {
    // When
    var response = restTemplate.getForEntity(getBaseUrl() + "/", String.class);

    // Then
    assertEquals(HttpStatus.OK, response.getStatusCode());

    var content = response.getBody();
    assertNotNull(content, "Response body should not be null");
    var doc = Jsoup.parse(content);

    // Verify form elements are present
    var messageInput = doc.selectFirst("input[name='message']");
    assertNotNull(messageInput, "Message input field should be present");

    var submitButton = doc.selectFirst("button#send-button");
    assertNotNull(submitButton, "Send button should be present");

    // Verify welcome text is displayed
    assertTrue(content.contains("ConversationalAI4J Demo"), "Welcome text should be displayed");
  }

  @Test
  void testConversationFlow() {
    // When - Submit a message via JSON API
    var testMessage = "Hello, AI!";
    var formData = new LinkedMultiValueMap<String, String>();
    formData.add("message", testMessage);

    var response = restTemplate.postForEntity(getBaseUrl() + "/chat", formData, String.class);

    // Then
    assertEquals(HttpStatus.OK, response.getStatusCode());

    var content = response.getBody();
    assertNotNull(content, "Response content should not be null");

    // Parse JSON response - in test environment with invalid Ollama URL, we expect echo mode or timeout error
    assertTrue(
        content.contains("\"response\"") && 
        (content.contains("Echo (AI unavailable): " + testMessage) || 
         content.contains(testMessage) || 
         content.contains("request timed out") ||
         content.contains("trouble processing")), 
        "JSON response should contain echo fallback, message, or timeout error. Actual content: " + content);
    
    assertTrue(content.contains("\"hasAudio\""), "JSON response should contain hasAudio field");
  }

  @Test
  void testEmptyMessageHandling() {
    // When - Submit empty message
    var formData = new LinkedMultiValueMap<String, String>();
    formData.add("message", "");

    var response = restTemplate.postForEntity(getBaseUrl() + "/chat", formData, String.class);

    // Then
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

    var content = response.getBody();
    assertNotNull(content, "Response body should not be null");
    assertTrue(
        content.contains("Message is required"),
        "Should return JSON error for empty input");
  }

  @Test
  void testActuatorHealthEndpoint() {
    // When
    var response = restTemplate.getForEntity(getBaseUrl() + "/actuator/health", String.class);

    // Then
    assertEquals(HttpStatus.OK, response.getStatusCode());
    var content = response.getBody();
    assertNotNull(content, "Health endpoint response should not be null");
    assertTrue(content.contains("UP"), "Health endpoint should return UP status");
  }
}
