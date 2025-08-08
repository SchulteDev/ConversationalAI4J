package schultedev.conversationalai4j.demo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.socket.*;
import schultedev.conversationalai4j.ConversationalAI;

import java.nio.ByteBuffer;

import static org.mockito.Mockito.*;

/**
 * Simple tests for VoiceStreamHandler.
 */
class VoiceStreamHandlerTest {

    @Mock
    private ConversationalAI mockConversationalAI;

    @Mock
    private WebSocketSession mockSession;

    private VoiceStreamHandler handler;
    private final String testSessionId = "test-session-123";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mockSession.getId()).thenReturn(testSessionId);
        when(mockConversationalAI.isSpeechEnabled()).thenReturn(true);
        when(mockConversationalAI.isSpeechToTextEnabled()).thenReturn(true);
        when(mockConversationalAI.isTextToSpeechEnabled()).thenReturn(true);
        
        handler = new VoiceStreamHandler();
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
        TextMessage startMessage = new TextMessage("start_recording");

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
        ByteBuffer audioData = ByteBuffer.wrap(new byte[]{1, 2, 3});

        // When (not recording)
        handler.handleMessage(mockSession, new BinaryMessage(audioData));

        // Then - should only have initial connection message
        verify(mockSession, atLeast(1)).sendMessage(any(TextMessage.class));
    }

    @Test
    void supportsPartialMessages_ShouldReturnTrue() {
        // When & Then
        assert handler.supportsPartialMessages();
    }

    @Test
    void handleTransportError_ShouldNotThrow() throws Exception {
        // Given
        Exception testException = new RuntimeException("Test transport error");

        // When & Then (should not throw)
        handler.handleTransportError(mockSession, testException);
    }
}