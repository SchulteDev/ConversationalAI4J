package schultedev.conversationalai4j.demo;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/** Simple tests for WebSocketConfig. */
class WebSocketConfigTest {

  @Mock private VoiceStreamHandler mockHandler;

  @Mock private WebSocketHandlerRegistry mockRegistry;

  @Mock private WebSocketHandlerRegistration mockRegistration;

  private WebSocketConfig config;
  private AutoCloseable mocks;

  @BeforeEach
  void setUp() {
    mocks = MockitoAnnotations.openMocks(this);
    config = new WebSocketConfig(mockHandler);
  }

  @AfterEach
  void tearDown() throws Exception {
    if (mocks != null) {
      mocks.close();
    }
  }

  @Test
  void constructor_WithValidHandler_ShouldInitialize() {
    // Given & When
    var config = new WebSocketConfig(mockHandler);

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
