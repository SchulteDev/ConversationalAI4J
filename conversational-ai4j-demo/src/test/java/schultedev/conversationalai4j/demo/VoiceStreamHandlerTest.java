package schultedev.conversationalai4j.demo;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.nio.ByteBuffer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.socket.*;

/** Tests for VoiceStreamHandler WebSocket voice processing pipeline. */
class VoiceStreamHandlerTest {

  @Mock private WebSocketSession mockSession;
  private VoiceStreamHandler handler;
  private AutoCloseable mocks;

  @BeforeEach
  void setUp() {
    mocks = MockitoAnnotations.openMocks(this);
    var testSessionId = "test-session-123";
    when(mockSession.getId()).thenReturn(testSessionId);
    when(mockSession.isOpen()).thenReturn(true);

    handler = new VoiceStreamHandler();
  }

  @AfterEach
  void tearDown() throws Exception {
    if (mocks != null) {
      mocks.close();
    }
  }

  @Test
  void afterConnectionEstablished_ShouldInitializeSession() throws Exception {
    // When
    handler.afterConnectionEstablished(mockSession);

    // Then - should send initial status message
    verify(mockSession, atLeast(1)).sendMessage(any(TextMessage.class));
  }

  @Test
  void afterConnectionClosed_ShouldCleanupSession() throws Exception {
    // Given
    handler.afterConnectionEstablished(mockSession);

    // When
    handler.afterConnectionClosed(mockSession, CloseStatus.NORMAL);

    // Then - should complete without errors (cleanup internal state)
    // This test mainly ensures no exceptions are thrown during cleanup
  }

  @Test
  void handleMessage_WithStartRecording_ShouldStartRecording() throws Exception {
    // Given
    handler.afterConnectionEstablished(mockSession);
    var startMessage = new TextMessage("start_recording");

    // When
    handler.handleMessage(mockSession, startMessage);

    // Then - should send status message
    verify(mockSession, atLeast(2)).sendMessage(any(TextMessage.class));
  }

  @Test
  void handleMessage_WithCheckStatus_ShouldSendSpeechStatus() throws Exception {
    // Given
    handler.afterConnectionEstablished(mockSession);

    // When
    handler.handleMessage(mockSession, new TextMessage("check_status"));

    // Then - should send speech status
    verify(mockSession, atLeast(2)).sendMessage(any(TextMessage.class));
  }

  @Test
  void handleMessage_WithBinaryData_WhileNotRecording_ShouldIgnore() throws Exception {
    // Given
    handler.afterConnectionEstablished(mockSession);
    var audioData = ByteBuffer.wrap(new byte[] {1, 2, 3});

    // When (not recording - default state)
    handler.handleMessage(mockSession, new BinaryMessage(audioData));

    // Then - should handle gracefully without errors
    // This test mainly ensures binary messages don't crash the handler
  }

  @Test
  void supportsPartialMessages_ShouldReturnTrue() {
    // When & Then
    assert handler.supportsPartialMessages();
  }

  @Test
  void handleTransportError_ShouldNotThrow() {
    // Given
    Exception testException = new RuntimeException("Test transport error");

    // When & Then (should not throw)
    handler.handleTransportError(mockSession, testException);
  }

  @Test
  void handleMessage_FullVoicePipeline_ShouldProcessCorrectly() throws Exception {
    // Given: Established connection and recording started
    handler.afterConnectionEstablished(mockSession);
    handler.handleMessage(mockSession, new TextMessage("start_recording"));

    // Add some audio chunks
    var audioChunk1 = ByteBuffer.wrap(createMockWavData(256));
    var audioChunk2 = ByteBuffer.wrap(createMockWavData(256));

    handler.handleMessage(mockSession, new BinaryMessage(audioChunk1));
    handler.handleMessage(mockSession, new BinaryMessage(audioChunk2));

    // When: Stop recording (triggers processing)
    handler.handleMessage(mockSession, new TextMessage("stop_recording"));

    // Then: Should send multiple status messages during processing
    var messageCaptor = ArgumentCaptor.forClass(TextMessage.class);
    verify(mockSession, atLeast(3)).sendMessage(messageCaptor.capture());

    // Verify we get status messages for different processing stages
    var sentMessages = messageCaptor.getAllValues();
    var foundProcessingStatus =
        sentMessages.stream().anyMatch(msg -> msg.getPayload().contains("processing"));

    assertTrue(foundProcessingStatus, "Should send processing status during audio processing");
  }

  @Test
  void handleMessage_StopRecordingWithoutAudio_ShouldHandleGracefully() throws Exception {
    // Given: Recording started but no audio sent
    handler.afterConnectionEstablished(mockSession);
    handler.handleMessage(mockSession, new TextMessage("start_recording"));

    // When: Stop recording with no audio chunks
    handler.handleMessage(mockSession, new TextMessage("stop_recording"));

    // Wait for async processing to complete
    Thread.sleep(100);

    // Then: Should handle gracefully and send error status
    var messageCaptor = ArgumentCaptor.forClass(TextMessage.class);
    verify(mockSession, atLeast(2)).sendMessage(messageCaptor.capture());

    var sentMessages = messageCaptor.getAllValues();
    var foundErrorStatus =
        sentMessages.stream()
            .anyMatch(
                msg -> msg.getPayload().contains("error") || msg.getPayload().contains("No audio"));

    assertTrue(foundErrorStatus, "Should send error status when no audio data received");
  }

  @Test
  void handleMessage_WithInvalidCommand_ShouldIgnoreGracefully() throws Exception {
    // Given: Established connection
    handler.afterConnectionEstablished(mockSession);

    // When: Send invalid command
    handler.handleMessage(mockSession, new TextMessage("invalid_command"));

    // Then: Should not crash (may or may not respond)
    verify(mockSession, atLeast(1)).sendMessage(any(TextMessage.class));
  }

  @Test
  void handleMessage_RecordingStateManagement_ShouldWorkCorrectly() throws Exception {
    // Given: Established connection
    handler.afterConnectionEstablished(mockSession);

    // When: Start and stop recording multiple times
    handler.handleMessage(mockSession, new TextMessage("start_recording"));

    var audioData = ByteBuffer.wrap(createMockWavData(128));
    handler.handleMessage(mockSession, new BinaryMessage(audioData));

    handler.handleMessage(mockSession, new TextMessage("stop_recording"));

    // Start again
    handler.handleMessage(mockSession, new TextMessage("start_recording"));
    handler.handleMessage(mockSession, new BinaryMessage(audioData));
    handler.handleMessage(mockSession, new TextMessage("stop_recording"));

    // Then: Should handle multiple recording sessions
    verify(mockSession, atLeast(5)).sendMessage(any(TextMessage.class));
  }

  @Test
  void handleMessage_LargeAudioChunks_ShouldAccumulate() throws Exception {
    // Given: Recording session started
    handler.afterConnectionEstablished(mockSession);
    handler.handleMessage(mockSession, new TextMessage("start_recording"));

    // When: Send multiple large audio chunks
    for (var i = 0; i < 10; i++) {
      var largeChunk = ByteBuffer.wrap(createMockWavData(1024));
      handler.handleMessage(mockSession, new BinaryMessage(largeChunk));
    }

    handler.handleMessage(mockSession, new TextMessage("stop_recording"));

    // Then: Should process all accumulated chunks
    var messageCaptor = ArgumentCaptor.forClass(TextMessage.class);
    verify(mockSession, atLeast(3)).sendMessage(messageCaptor.capture());

    // Should eventually complete processing
    var messages = messageCaptor.getAllValues();
    var foundCompleteStatus =
        messages.stream()
            .anyMatch(
                msg -> msg.getPayload().contains("complete") || msg.getPayload().contains("ready"));

    assertTrue(foundCompleteStatus, "Should complete processing of large audio chunks");
  }

  @Test
  void afterConnectionClosed_WithActiveRecording_ShouldCleanup() throws Exception {
    // Given: Active recording session
    handler.afterConnectionEstablished(mockSession);
    handler.handleMessage(mockSession, new TextMessage("start_recording"));

    var audioData = ByteBuffer.wrap(createMockWavData(256));
    handler.handleMessage(mockSession, new BinaryMessage(audioData));

    // When: Connection closed during active recording
    handler.afterConnectionClosed(mockSession, CloseStatus.NORMAL);

    // Then: Should cleanup without throwing exceptions
    // (This test mainly ensures cleanup doesn't crash)
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
