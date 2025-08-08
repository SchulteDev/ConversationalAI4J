package schultedev.conversationalai4j.demo;

import static org.junit.jupiter.api.Assertions.*;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Integration tests for the ConversationalAI4J demo web application. Tests the complete web
 * application functionality including Spring Boot server startup, Thymeleaf rendering, and user
 * interaction flows.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DemoIntegrationTest {

  @LocalServerPort private int port;

  @Autowired private TestRestTemplate restTemplate;

  private String getBaseUrl() {
    return "http://localhost:" + port;
  }

  @Test
  void testApplicationStartup() {
    // When
    ResponseEntity<String> response = restTemplate.getForEntity(getBaseUrl() + "/", String.class);

    // Then
    assertEquals(
        HttpStatus.OK, response.getStatusCode(), "Application should respond with HTTP 200");

    String content = response.getBody();
    assertNotNull(content, "Response content should not be null");
    assertTrue(
        content.contains("ConversationalAI4J Demo"), "Page should contain application title");
  }

  @Test
  void testConversationPageRendering() {
    // When
    ResponseEntity<String> response = restTemplate.getForEntity(getBaseUrl() + "/", String.class);

    // Then
    assertEquals(HttpStatus.OK, response.getStatusCode());

    String content = response.getBody();
    Document doc = Jsoup.parse(content);

    // Verify form elements are present
    Element messageInput = doc.selectFirst("input[name='message']");
    assertNotNull(messageInput, "Message input field should be present");

    Element submitButton = doc.selectFirst("button[type='submit']");
    assertNotNull(submitButton, "Submit button should be present");

    // Verify welcome text is displayed
    assertTrue(content.contains("ConversationalAI4J Demo"), "Welcome text should be displayed");
  }

  @Test
  void testConversationFlow() {
    // When - Submit a message
    String testMessage = "Hello, AI!";
    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
    formData.add("message", testMessage);

    ResponseEntity<String> response =
        restTemplate.postForEntity(getBaseUrl() + "/send", formData, String.class);

    // Then
    assertEquals(HttpStatus.OK, response.getStatusCode());

    String content = response.getBody();
    assertNotNull(content, "Response content should not be null");

    // Verify the response is displayed - either AI response or fallback echo
    assertTrue(
        content.contains(testMessage), "Response should contain the test message in some form");

    // Verify the message is preserved in the form
    assertTrue(
        content.contains("value=\"" + testMessage + "\""),
        "Input field should preserve the submitted message");
  }

  @Test
  void testEmptyMessageHandling() {
    // When - Submit empty message
    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
    formData.add("message", "");

    ResponseEntity<String> response =
        restTemplate.postForEntity(getBaseUrl() + "/send", formData, String.class);

    // Then
    assertEquals(HttpStatus.OK, response.getStatusCode());

    String content = response.getBody();
    assertTrue(
        content.contains("Please enter a message."),
        "Should display error message for empty input");
  }

  @Test
  void testActuatorHealthEndpoint() {
    // When
    ResponseEntity<String> response =
        restTemplate.getForEntity(getBaseUrl() + "/actuator/health", String.class);

    // Then
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertTrue(response.getBody().contains("UP"), "Health endpoint should return UP status");
  }
}
