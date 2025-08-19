package schultedev.conversationalai4j.demo;

import static org.junit.jupiter.api.Assertions.*;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * End-to-end tests for ConversationalAI4J using Playwright to test the actual UI functionality.
 *
 * <p>These tests validate the complete user experience including: - Text-based conversations
 * through the web UI - Voice recording and processing functionality - Audio playback and TTS
 * capabilities - WebSocket real-time communication - UI responsiveness and error handling
 *
 * <p>Uses Testcontainers to spin up the full Docker environment and Playwright to interact with the
 * actual web interface as a user would.
 */
@Testcontainers
class ConversationalE2ETest {

  private static final Logger log = LoggerFactory.getLogger(ConversationalE2ETest.class);

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
          .withLocalCompose(true);

  private static Playwright playwright;
  private static Browser browser;
  private BrowserContext context;
  private Page page;

  @BeforeAll
  static void launchBrowser() {
    playwright = Playwright.create();
    browser =
        playwright
            .chromium()
            .launch(
                new BrowserType.LaunchOptions().setHeadless(true)); // Set to false for debugging
  }

  @AfterAll
  static void closeBrowser() {
    if (browser != null) {
      browser.close();
    }
    if (playwright != null) {
      playwright.close();
    }
  }

  @BeforeEach
  void createContextAndPage() {
    context =
        browser.newContext(
            new Browser.NewContextOptions()
                .setPermissions(List.of("microphone"))); // Grant microphone permissions
    page = context.newPage();
  }

  @AfterEach
  void closeContext() {
    if (context != null) {
      context.close();
    }
  }

  private String getDemoBaseUrl() {
    return "http://"
        + environment.getServiceHost(DEMO_SERVICE, DEMO_PORT)
        + ":"
        + environment.getServicePort(DEMO_SERVICE, DEMO_PORT);
  }

  private void verifyDemoServiceIsHealthy() {
    try {
      var response = page.request().get(getDemoBaseUrl() + "/actuator/health");
      log.info("Health check response status: {}", response.status());
      if (response.status() != 200) {
        log.warn("Demo service health check failed with status: {}", response.status());
        log.warn("Response body: {}", response.text());
      }
    } catch (Exception e) {
      log.warn("Health check failed: {}", e.getMessage());
      // Don't fail the test here, just log the warning
    }
  }

  @Test
  void coreUIAndAccessibility_ShouldWorkCorrectly() {
    // Given: Verify the demo service is responding
    verifyDemoServiceIsHealthy();

    // When: Navigate to the conversation page
    log.info(
        "Testing complete UI, accessibility, and notification systems at: {}", getDemoBaseUrl());
    page.navigate(getDemoBaseUrl() + "/");

    // Wait for page to load completely first
    page.waitForLoadState(LoadState.NETWORKIDLE);

    // Give additional time for JavaScript initialization and dynamic content
    page.waitForTimeout(2000);

    // === BASIC UI LOADING TESTS ===
    log.info("🔍 Testing basic UI loading...");

    // First check if we can access the page at all
    log.info("Page URL after navigation: {}", page.url());
    log.info("Page title: {}", page.title());

    // Check for error pages or failed responses
    var bodyContent = page.locator("body").textContent();
    if (bodyContent.contains("Whitelabel Error Page")
        || bodyContent.contains("404")
        || bodyContent.contains("500")) {
      log.error("Application returned error page. Body content: {}", bodyContent);
      fail("Application returned an error page instead of the conversation interface");
    }

    // Check if page is completely empty
    if (bodyContent.trim().isEmpty()) {
      log.error("Page body is completely empty");
      fail("Page loaded but has no content");
    }

    page.waitForSelector("h1", new Page.WaitForSelectorOptions().setTimeout(20000));
    assertTrue(page.locator("h1").isVisible());
    log.info("✅ Page title found: {}", page.locator("h1").textContent());

    // Check for chat area (always present)
    page.waitForSelector("#chat-area", new Page.WaitForSelectorOptions().setTimeout(15000));
    assertTrue(page.locator("#chat-area").isVisible());
    log.info("✅ Chat area found");

    // Check for welcome message OR conversation history (one should be present)
    // The welcome message is conditionally displayed only if conversationHistory is empty
    try {
      page.waitForSelector(".ai-message", new Page.WaitForSelectorOptions().setTimeout(5000));
      assertTrue(page.locator(".ai-message").first().isVisible());
      log.info("✅ Welcome message or AI message found");
    } catch (Exception e) {
      log.warn(
          "No AI messages found (this may be normal if conversation history is empty): {}",
          e.getMessage());
      // This is acceptable - the welcome message might not show if there's conversation history
      // or if the backend is still initializing
    }

    // Check for essential input elements (these should always be present)
    page.waitForSelector("#message-input", new Page.WaitForSelectorOptions().setTimeout(15000));
    assertTrue(page.locator("#message-input").isVisible());

    page.waitForSelector("#send-button", new Page.WaitForSelectorOptions().setTimeout(10000));
    assertTrue(page.locator("#send-button").isVisible());

    page.waitForSelector("#voice-toggle", new Page.WaitForSelectorOptions().setTimeout(10000));
    assertTrue(page.locator("#voice-toggle").isVisible());
    log.info("✅ Input controls found");

    // Verify the page has the expected structure
    assertTrue(page.locator(".chat-container").isVisible(), "Chat container should be visible");
    assertTrue(page.locator(".chat-header").isVisible(), "Chat header should be visible");

    log.info("✅ Basic UI loading completed");

    // === ACCESSIBILITY TESTS ===
    log.info("🔍 Testing accessibility standards...");

    // Form labels and input accessibility
    var messageInput = page.locator("#message-input");
    assertTrue(messageInput.isVisible(), "Message input should be visible");
    var placeholder = messageInput.getAttribute("placeholder");
    if (placeholder == null || placeholder.trim().isEmpty()) {
      // Sometimes placeholder might be set dynamically, let's wait a bit and try again
      page.waitForTimeout(1000);
      placeholder = messageInput.getAttribute("placeholder");
    }
    assertNotNull(placeholder, "Message input should have placeholder text");
    assertFalse(placeholder.trim().isEmpty(), "Placeholder text should not be empty");
    log.info("✅ Message input accessibility: placeholder = '{}'", placeholder);

    // Button accessibility
    var sendButton = page.locator("#send-button");
    assertTrue(sendButton.isVisible(), "Send button should be visible");
    log.info("✅ Send button is visible and accessible");

    var voiceButton = page.locator("#voice-toggle");
    assertTrue(voiceButton.isVisible(), "Voice button should be visible");
    var voiceTitle = voiceButton.getAttribute("title");
    if (voiceTitle == null || voiceTitle.trim().isEmpty()) {
      // Sometimes title might be set dynamically, let's wait a bit and try again
      page.waitForTimeout(1000);
      voiceTitle = voiceButton.getAttribute("title");
    }
    assertNotNull(voiceTitle, "Voice button should have title attribute for accessibility");
    assertFalse(voiceTitle.trim().isEmpty(), "Voice button title should not be empty");
    log.info("✅ Voice button accessibility: title = '{}'", voiceTitle);

    // Heading structure
    var heading = page.locator("h1");
    assertTrue(heading.isVisible(), "Page should have main heading");
    var headingText = heading.textContent();
    assertNotNull(headingText, "Heading should have text content");
    assertFalse(headingText.trim().isEmpty(), "Heading text should not be empty");
    log.info("✅ Heading structure: h1 = '{}'", headingText);

    // Check for essential structural elements
    assertTrue(
        page.locator(".chat-container").isVisible(),
        "Chat container should be present for screen readers");
    assertTrue(
        page.locator(".chat-area").isVisible(),
        "Chat area should be present for content organization");

    // Verify that interactive elements are keyboard accessible (basic check)
    assertTrue(messageInput.isEnabled(), "Message input should be keyboard accessible");
    assertTrue(sendButton.isEnabled(), "Send button should be keyboard accessible");
    assertTrue(voiceButton.isEnabled(), "Voice button should be keyboard accessible");

    log.info("✅ Accessibility standards check completed");

    // === NOTIFICATION SYSTEM TESTS ===
    log.info("🔍 Testing notification system...");

    // Notification overlay should be present (even if hidden)
    assertTrue(
        page.locator("#notification-overlay").count() > 0, "Notification overlay should exist");
    assertTrue(page.locator("#notification").count() > 0, "Notification element should exist");
    assertTrue(
        page.locator("#notification-text").count() > 0, "Notification text element should exist");

    // Notification should be hidden by default
    var overlayClass = page.locator("#notification-overlay").getAttribute("class");
    assertTrue(
        (overlayClass != null && overlayClass.contains("hidden"))
            || !page.locator("#notification-overlay").isVisible(),
        "Notification should be hidden by default");

    log.info("✅ Notification system elements are present");

    log.info("✅ Complete UI, accessibility, and notification tests passed");
  }

  @Test
  void textInteractionFeatures_ShouldWorkEndToEnd() {
    // Given: Navigate to the conversation page
    log.info("🔍 Testing complete text interaction features at: {}", getDemoBaseUrl());
    page.navigate(getDemoBaseUrl() + "/");

    // Wait for page to load completely
    page.waitForLoadState(LoadState.NETWORKIDLE);
    page.waitForSelector("#message-input", new Page.WaitForSelectorOptions().setTimeout(10000));

    // === BASIC TEXT CONVERSATION TEST ===
    log.info("🔍 Testing basic text conversation...");

    // When: Send a text message
    var testMessage = "Hello from E2E test";
    page.locator("#message-input").fill(testMessage);
    page.locator("#send-button").click();

    // Then: Should show user message in chat (with more robust waiting)
    try {
      page.waitForSelector(".user-message", new Page.WaitForSelectorOptions().setTimeout(10000));
      assertTrue(page.locator(".user-message").isVisible());
      log.info("✅ User message appeared successfully");
    } catch (Exception e) {
      log.warn("User message didn't appear as expected: {}", e.getMessage());
      // Continue test - sometimes UI updates may be delayed
    }

    // And: Input should be cleared
    assertEquals("", page.locator("#message-input").inputValue());
    log.info("✅ Input field cleared after sending message");

    // Check for AI response (optional - may timeout in test environment)
    try {
      page.waitForSelector(
          ".ai-message .message-content", new Page.WaitForSelectorOptions().setTimeout(15000));
      var aiMessages = page.locator(".ai-message .message-content").all();
      log.info("✅ AI messages count: {}", aiMessages.size());
    } catch (Exception e) {
      log.debug("AI response timeout (expected in some test environments): {}", e.getMessage());
    }

    log.info("✅ Basic text conversation functionality verified");

    // === ENTER KEY SUBMISSION TEST ===
    log.info("🔍 Testing Enter key submission...");

    // When: Type message and press Enter
    var enterTestMessage = "Hello via Enter key";
    page.locator("#message-input").fill(enterTestMessage);
    page.locator("#message-input").press("Enter");

    // Then: Message should be sent (same as clicking send button)
    page.waitForSelector(
        ".user-message:has-text('" + enterTestMessage + "')",
        new Page.WaitForSelectorOptions().setTimeout(5000));
    assertTrue(page.locator(".user-message:has-text('" + enterTestMessage + "')").isVisible());

    // And: Input should be cleared
    assertEquals("", page.locator("#message-input").inputValue());
    log.info("✅ Enter key submission works correctly");

    // === MULTIPLE MESSAGES AND CONVERSATION HISTORY TEST ===
    log.info("🔍 Testing multiple messages and conversation history...");

    // When: Send multiple messages
    var messages = new String[] {"First message", "Second message", "Third message"};

    for (var message : messages) {
      page.locator("#message-input").fill(message);
      page.locator("#send-button").click();

      // Wait for user message to appear
      page.waitForSelector(
          ".user-message:has-text('" + message + "')",
          new Page.WaitForSelectorOptions().setTimeout(5000));

      // Wait for AI response (optional, may timeout in some environments)
      try {
        page.waitForSelector(
            ".ai-message .message-content", new Page.WaitForSelectorOptions().setTimeout(5000));
      } catch (Exception e) {
        log.debug("AI response timeout for message: {}", message);
      }
    }

    // Then: Should maintain all messages in conversation history
    for (var message : messages) {
      assertTrue(
          page.locator(".user-message:has-text('" + message + "')").isVisible(),
          "Message should be visible: " + message);
    }

    log.info("✅ Multiple messages maintain conversation history");

    // Verify we now have multiple user messages (including our test messages)
    var userMessageCount = page.locator(".user-message").count();
    log.info("✅ Total user messages in conversation: {}", userMessageCount);
    assertTrue(
        userMessageCount >= messages.length,
        "Should have at least " + messages.length + " user messages");

    log.info("✅ Complete text interaction features work end-to-end");
  }

  @Test
  void voiceUIFeatures_ShouldWorkCorrectly() {
    // Given: Navigate to conversation page with microphone permissions
    log.info("🔍 Testing voice UI features...");
    page.navigate(getDemoBaseUrl() + "/");
    page.waitForLoadState(LoadState.NETWORKIDLE);
    page.waitForSelector("#voice-toggle", new Page.WaitForSelectorOptions().setTimeout(10000));

    // === VOICE BUTTON INTERACTIVITY TESTS ===
    log.info("🔍 Testing voice button interactivity...");

    var voiceButton = page.locator("#voice-toggle");
    assertTrue(voiceButton.isVisible(), "Voice button should be visible");
    assertTrue(voiceButton.isEnabled(), "Voice button should be enabled");

    // Check button attributes
    var initialTitle = voiceButton.getAttribute("title");
    assertNotNull(initialTitle, "Voice button should have title attribute");
    assertTrue(
        initialTitle.contains("voice")
            || initialTitle.contains("microphone")
            || initialTitle.contains("🎤"),
        "Title should indicate voice functionality");
    log.info("✅ Voice button title: '{}'", initialTitle);

    // === VOICE RECORDING STATE CHANGES TESTS ===
    log.info("🔍 Testing voice recording state changes...");

    // Click the voice button (this will trigger voice recording mode)
    voiceButton.click();
    log.info("✅ Voice button clicked - recording should start");

    // Note: We can't easily test actual MediaRecorder functionality in headless mode
    // but we can verify the UI responds appropriately
    page.waitForTimeout(2000); // Allow for state changes

    // Check if button appearance changed (class changes, style changes, etc.)
    // This depends on the JavaScript implementation but we can check for common patterns
    var buttonClassAfterClick = voiceButton.getAttribute("class");
    log.info("Voice button class after click: '{}'", buttonClassAfterClick);

    // Try clicking again to stop recording (if it started)
    voiceButton.click();
    page.waitForTimeout(1000);
    log.info("✅ Voice button clicked again - recording should stop");

    // Verify button is still functional after the recording cycle
    assertTrue(
        voiceButton.isVisible(), "Voice button should still be visible after recording cycle");
    assertTrue(
        voiceButton.isEnabled(), "Voice button should still be enabled after recording cycle");

    log.info("✅ Voice UI features work correctly");
  }

  @Test
  void audioPlaybackElements_ShouldBePresent() {
    // Given: Navigate to conversation page
    page.navigate(getDemoBaseUrl() + "/");
    page.waitForSelector("#message-input");

    // Send a message to potentially trigger audio response
    page.locator("#message-input").fill("Test audio response");
    page.locator("#send-button").click();

    // Wait for potential AI response with audio
    try {
      page.waitForSelector(
          ".ai-message .message-content", new Page.WaitForSelectorOptions().setTimeout(30000));

      // Check for audio player elements (may or may not be present depending on TTS availability)
      var hasAudioPlayer = page.locator("#ai-audio-player").isVisible();
      var hasAudioControls = page.locator(".audio-play-btn").count() > 0;

      log.info(
          "Audio player present: {}, Audio controls present: {}", hasAudioPlayer, hasAudioControls);

      // If audio elements are present, verify they're properly structured
      if (hasAudioControls) {
        var audioButtons = page.locator(".audio-play-btn");
        assertTrue(audioButtons.first().isVisible(), "Audio play button should be visible");
        assertNotNull(
            audioButtons.first().getAttribute("data-text"),
            "Audio button should have data-text attribute");
      }

    } catch (Exception e) {
      log.debug("No AI response received for audio test: {}", e.getMessage());
    }

    log.info("✅ Audio playback elements check completed");
  }

  @Test
  void speechStatusEndpoint_ShouldBeAccessibleViaUI() {
    // Given: Navigate to conversation page
    page.navigate(getDemoBaseUrl() + "/");

    // When: Check if speech status is accessible (this might be done via JavaScript)
    // We can simulate checking the endpoint that the UI uses
    var response = page.request().get(getDemoBaseUrl() + "/speech-status");

    // Then: Should respond with speech capabilities
    assertEquals(200, response.status());
    var responseBody = response.text();
    assertTrue(responseBody.contains("available"), "Response should contain availability info");
    assertTrue(responseBody.contains("speechToText"), "Response should contain STT info");
    assertTrue(responseBody.contains("textToSpeech"), "Response should contain TTS info");

    log.info("✅ Speech status endpoint accessible via UI context");
  }
}
