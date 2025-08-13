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

    var submitButton = doc.selectFirst("button[type='submit']");
    assertNotNull(submitButton, "Submit button should be present");

    // Verify welcome text is displayed
    assertTrue(content.contains("ConversationalAI4J Demo"), "Welcome text should be displayed");
  }

  @Test
  void testConversationFlow() {
    // When - Submit a message
    var testMessage = "Hello, AI!";
    var formData = new LinkedMultiValueMap<String, String>();
    formData.add("message", testMessage);

    var response = restTemplate.postForEntity(getBaseUrl() + "/send", formData, String.class);

    // Then
    assertEquals(HttpStatus.OK, response.getStatusCode());

    var content = response.getBody();
    assertNotNull(content, "Response content should not be null");

    // Verify the response is displayed - either AI response or fallback echo
    assertTrue(
        content.contains(testMessage), "Response should contain the test message in some form");

    // Verify the conversation history shows the message (new behavior)
    // The input field should be empty for better UX
    assertFalse(
        content.contains("value=\"" + testMessage + "\""),
        "Input field should be cleared for better UX");
  }

  @Test
  void testEmptyMessageHandling() {
    // When - Submit empty message
    var formData = new LinkedMultiValueMap<String, String>();
    formData.add("message", "");

    var response = restTemplate.postForEntity(getBaseUrl() + "/send", formData, String.class);

    // Then
    assertEquals(HttpStatus.OK, response.getStatusCode());

    var content = response.getBody();
    assertNotNull(content, "Response body should not be null");
    assertTrue(
        content.contains("Please enter a message."),
        "Should display error message for empty input");
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
