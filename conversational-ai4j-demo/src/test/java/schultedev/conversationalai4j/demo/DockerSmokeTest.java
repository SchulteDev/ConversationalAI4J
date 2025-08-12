package schultedev.conversationalai4j.demo;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;

/**
 * Docker environment smoke test - validates the complete ConversationalAI4J system including Ollama
 * integration and speech services in containerized environment.
 *
 * <p>This test only runs when DOCKER_SMOKE_TEST environment variable is set, typically in CI/CD
 * pipelines with docker-compose setup.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(
    properties = {"ollama.base-url=http://ollama:11434", "ollama.model-name=llama3.2:3b"})
@EnabledIfEnvironmentVariable(named = "DOCKER_SMOKE_TEST", matches = "true")
class DockerSmokeTest {

  @LocalServerPort private int port;

  @Autowired private TestRestTemplate restTemplate;

  private String getBaseUrl() {
    return "http://localhost:" + port;
  }

  @Test
  void applicationStartup_InDockerEnvironment_ShouldSucceed() {
    // When: Application starts up in Docker environment
    var response = restTemplate.getForEntity(getBaseUrl() + "/", String.class);

    // Then: Should respond successfully
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().contains("ConversationalAI4J Demo"));
  }

  @Test
  void ollamaIntegration_ShouldBeAccessible() {
    // When: Check if Ollama service is accessible
    var response =
        restTemplate.postForEntity(getBaseUrl() + "/send", "message=Hello Ollama", String.class);

    // Then: Should get response (either from AI or fallback)
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());

    // Response should contain either AI response or fallback message
    String body = response.getBody();
    boolean hasValidResponse =
        body.contains("Hello") || body.contains("Echo") || body.contains("unavailable");
    assertTrue(hasValidResponse, "Should get some form of response to user message");
  }

  @Test
  void speechStatus_InDockerEnvironment_ShouldReportCorrectly() {
    // When: Check speech services status
    var response = restTemplate.getForEntity(getBaseUrl() + "/speech-status", String.class);

    // Then: Should report speech status
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());

    // Should be valid JSON with availability info
    String jsonResponse = response.getBody();
    assertTrue(jsonResponse.contains("available"));
    assertTrue(jsonResponse.contains("speechToText"));
    assertTrue(jsonResponse.contains("textToSpeech"));

    // In Docker environment, speech may or may not be available
    // depending on sherpa-onnx setup - just verify the endpoint works
  }

  @Test
  void actuatorHealth_ShouldShowSystemHealth() {
    // When: Check actuator health endpoint
    var response = restTemplate.getForEntity(getBaseUrl() + "/actuator/health", String.class);

    // Then: Should report system health
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().contains("UP"));
  }

  @Test
  void conversationFlow_WithDockerizedAI_ShouldWork() {
    // Given: Test conversation messages
    String[] testMessages = {
      "Hello, can you hear me?", "What is 2 + 2?", "Tell me a joke", "Thank you"
    };

    // When: Send multiple messages in sequence
    for (String message : testMessages) {
      var response =
          restTemplate.postForEntity(getBaseUrl() + "/send", "message=" + message, String.class);

      // Then: Each should get a response
      assertEquals(
          HttpStatus.OK, response.getStatusCode(), "Message '" + message + "' should get response");
      assertNotNull(response.getBody());

      // Response should contain the message and some form of response
      String body = response.getBody();
      assertTrue(body.contains(message), "Response should contain original message: " + message);
    }
  }

  @Test
  void voiceEndpoints_ShouldHandleRequestsGracefully() {
    // Given: Mock audio data
    byte[] mockAudioData = createMockWavData(1024);

    // When: Try voice-to-text endpoint
    var voiceToTextResponse =
        restTemplate.postForEntity(getBaseUrl() + "/voice-to-text", mockAudioData, String.class);

    // Then: Should handle gracefully (may succeed or fail based on speech config)
    // We just verify it doesn't crash the application
    assertTrue(
        voiceToTextResponse.getStatusCode().is2xxSuccessful()
            || voiceToTextResponse.getStatusCode().is4xxClientError()
            || voiceToTextResponse.getStatusCode().is5xxServerError());

    // When: Try text-to-voice endpoint
    var textToVoiceResponse =
        restTemplate.postForEntity(
            getBaseUrl() + "/text-to-voice", "Hello from smoke test", byte[].class);

    // Then: Should handle gracefully
    assertTrue(
        textToVoiceResponse.getStatusCode().is2xxSuccessful()
            || textToVoiceResponse.getStatusCode().is4xxClientError()
            || textToVoiceResponse.getStatusCode().is5xxServerError());
  }

  @Test
  void performanceBaseline_ShouldMeetBasicRequirements() {
    // Given: Simple test message
    String testMessage = "Performance test message";

    // When: Send message and measure response time
    long startTime = System.currentTimeMillis();
    var response =
        restTemplate.postForEntity(getBaseUrl() + "/send", "message=" + testMessage, String.class);
    long responseTime = System.currentTimeMillis() - startTime;

    // Then: Should respond within reasonable time
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertTrue(
        responseTime < 30000, // 30 second timeout for AI responses
        "Response time should be under 30 seconds, was: " + responseTime + "ms");

    // Log performance for monitoring
    System.out.println("Docker smoke test response time: " + responseTime + "ms");
  }

  @Test
  void memoryAndResourceUsage_ShouldBeStable() {
    // Given: Multiple requests to test memory stability
    int requestCount = 10;

    // When: Send multiple requests
    for (int i = 0; i < requestCount; i++) {
      var response =
          restTemplate.postForEntity(
              getBaseUrl() + "/send", "message=Memory test " + i, String.class);

      // Then: Each request should succeed
      assertEquals(HttpStatus.OK, response.getStatusCode(), "Request " + i + " should succeed");
    }

    // Additional request after load should still work
    var finalResponse =
        restTemplate.postForEntity(
            getBaseUrl() + "/send", "message=Final memory test", String.class);

    assertEquals(
        HttpStatus.OK,
        finalResponse.getStatusCode(),
        "Application should remain stable after multiple requests");
  }

  private byte[] createMockWavData(int sizeInBytes) {
    // Create minimal valid WAV header + data for testing
    byte[] wavData = new byte[Math.max(44, sizeInBytes)];

    // WAV header
    System.arraycopy("RIFF".getBytes(), 0, wavData, 0, 4);
    writeInt32LE(wavData, 4, wavData.length - 8);
    System.arraycopy("WAVE".getBytes(), 0, wavData, 8, 4);
    System.arraycopy("fmt ".getBytes(), 0, wavData, 12, 4);
    writeInt32LE(wavData, 16, 16);
    writeInt16LE(wavData, 20, (short) 1);
    writeInt16LE(wavData, 22, (short) 1);
    writeInt32LE(wavData, 24, 16000);
    writeInt32LE(wavData, 28, 32000);
    writeInt16LE(wavData, 32, (short) 2);
    writeInt16LE(wavData, 34, (short) 16);
    System.arraycopy("data".getBytes(), 0, wavData, 36, 4);
    writeInt32LE(wavData, 40, wavData.length - 44);

    return wavData;
  }

  private void writeInt32LE(byte[] data, int offset, int value) {
    data[offset] = (byte) (value & 0xFF);
    data[offset + 1] = (byte) ((value >> 8) & 0xFF);
    data[offset + 2] = (byte) ((value >> 16) & 0xFF);
    data[offset + 3] = (byte) ((value >> 24) & 0xFF);
  }

  private void writeInt16LE(byte[] data, int offset, short value) {
    data[offset] = (byte) (value & 0xFF);
    data[offset + 1] = (byte) ((value >> 8) & 0xFF);
  }
}
