package schultedev.conversationalai4j.demo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Simple tests for WebSocketConfig.
 */
class WebSocketConfigTest {

    @Mock
    private VoiceStreamHandler mockHandler;

    @Mock
    private WebSocketHandlerRegistry mockRegistry;

    @Mock
    private WebSocketHandlerRegistration mockRegistration;

    private WebSocketConfig config;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        config = new WebSocketConfig(mockHandler);
    }

    @Test
    void constructor_WithValidHandler_ShouldInitialize() {
        // Given & When
        WebSocketConfig config = new WebSocketConfig(mockHandler);
        
        // Then
        assertNotNull(config);
    }

    @Test
    void registerWebSocketHandlers_ShouldConfigureHandler() {
        // Given
        when(mockRegistry.addHandler(any(), any())).thenReturn(mockRegistration);
        when(mockRegistration.setAllowedOrigins(any())).thenReturn(mockRegistration);

        // When
        config.registerWebSocketHandlers(mockRegistry);

        // Then
        verify(mockRegistry).addHandler(eq(mockHandler), eq("/voice-stream"));
        verify(mockRegistration).setAllowedOrigins(eq("*"));
    }
}