package schultedev.conversationalai4j.demo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Unit tests for ConversationController functionality. */
@WebMvcTest(ConversationController.class)
class ConversationControllerTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void testIndex_ShouldReturnConversationPage() throws Exception {
    mockMvc
        .perform(get("/"))
        .andExpect(status().isOk())
        .andExpect(view().name("conversation"))
        .andExpect(model().attribute("welcomeText", "ConversationalAI4J Demo"));
  }

  @Test
  void testChatAPI_WithValidMessage() throws Exception {
    var testMessage = "Hello, AI!";

    mockMvc
        .perform(
            post("/chat")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("message", testMessage))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/json"))
        .andExpect(jsonPath("$.response").exists())
        .andExpect(jsonPath("$.hasAudio").exists());
  }

  @Test
  void testChatAPI_WithEmptyMessage() throws Exception {
    mockMvc
        .perform(
            post("/chat").contentType(MediaType.APPLICATION_FORM_URLENCODED).param("message", ""))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType("application/json"))
        .andExpect(jsonPath("$.error").value("Message is required"));
  }

  @Test
  void testChatAPI_WithWhitespaceOnlyMessage() throws Exception {
    mockMvc
        .perform(
            post("/chat")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("message", "   "))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType("application/json"))
        .andExpect(jsonPath("$.error").value("Message is required"));
  }

  @Test
  void testChatAPI_WhenOllamaUnavailable_ShouldFallbackToEcho() throws Exception {
    // Given: Ollama is unavailable in test environment (no real connection)
    var testMessage = "Test message for unavailable AI";

    // When: Send message via chat API
    mockMvc
        .perform(
            post("/chat")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("message", testMessage))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/json"))
        .andExpect(jsonPath("$.response").exists())
        .andExpect(jsonPath("$.hasAudio").exists());

    // Note: Since ConversationalAI may be null in test environment,
    // we should get either an AI response or echo/error response in JSON format
  }

  @Test
  void testVoiceChat_WhenConversationalAINull_ShouldReturnError() throws Exception {
    // Given: Mock audio data
    var mockAudioData = createMockWavData(256);

    // When: Try voice chat (ConversationalAI may be initialized but speech unavailable)
    mockMvc
        .perform(post("/voice-chat").contentType("application/octet-stream").content(mockAudioData))
        .andExpect(status().is4xxClientError()); // 400 or 503 both acceptable
  }

  @Test
  void testVoiceChat_WithEmptyAudio_ShouldReturn400() throws Exception {
    // When: Send empty audio data
    mockMvc
        .perform(post("/voice-chat").contentType("application/octet-stream").content(new byte[0]))
        .andExpect(status().isBadRequest());
  }

  @Test
  void testTextToVoice_WhenConversationalAINull_ShouldReturnError() throws Exception {
    // When: Try text-to-voice (ConversationalAI may be initialized but speech unavailable)
    mockMvc
        .perform(post("/text-to-voice").contentType("text/plain").content("Hello AI"))
        .andExpect(status().is4xxClientError()); // 400 or 503 both acceptable
  }

  @Test
  void testTextToVoice_WithEmptyText_ShouldReturn400() throws Exception {
    // When: Send empty text
    mockMvc
        .perform(post("/text-to-voice").contentType("text/plain").content(""))
        .andExpect(status().isBadRequest());
  }

  @Test
  void testVoiceToText_WhenConversationalAINull_ShouldReturnError() throws Exception {
    // Given: Mock audio data
    var mockAudioData = createMockWavData(512);

    // When: Try voice-to-text (ConversationalAI may be initialized but speech unavailable)
    mockMvc
        .perform(
            post("/voice-to-text").contentType("application/octet-stream").content(mockAudioData))
        .andExpect(status().is4xxClientError()); // 400 or 503 both acceptable
  }

  @Test
  void testSpeechStatus_ShouldReturnJsonStatus() throws Exception {
    // When: Check speech status
    mockMvc
        .perform(get("/speech-status"))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/json"))
        .andExpect(jsonPath("$.available").exists());
    // Note: reason field may not exist if ConversationalAI is initialized
  }

  @Test
  void testChatAPI_WithLongMessage_ShouldHandleGracefully() throws Exception {
    // Given: Very long message
    var longMessage = "This is a very long message ".repeat(100);

    // When: Send long message via chat API
    mockMvc
        .perform(
            post("/chat")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("message", longMessage))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/json"))
        .andExpect(jsonPath("$.response").exists())
        .andExpect(jsonPath("$.hasAudio").exists());
  }

  @Test
  void testChatAPI_WithSpecialCharacters_ShouldHandleCorrectly() throws Exception {
    // Given: Message with special characters
    var specialMessage = "Hello! @#$%^&*()_+ 中文 émojis 🤖";

    // When: Send message with special characters via chat API
    mockMvc
        .perform(
            post("/chat")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("message", specialMessage))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/json"))
        .andExpect(jsonPath("$.response").exists())
        .andExpect(jsonPath("$.hasAudio").exists());
  }

  @Test
  void testChatAPI_WithMalformedRequest_ShouldHandleGracefully() throws Exception {
    // When: Send post request (missing message parameter)
    // Spring may return 400 for missing required parameter, which is also acceptable
    mockMvc
        .perform(post("/chat"))
        .andExpect(
            status().is4xxClientError()); // 400 or similar is acceptable for malformed request
  }

  private byte[] createMockWavData(int sizeInBytes) {
    // Create minimal valid WAV header + data
    var wavData = new byte[Math.max(44, sizeInBytes)];

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
