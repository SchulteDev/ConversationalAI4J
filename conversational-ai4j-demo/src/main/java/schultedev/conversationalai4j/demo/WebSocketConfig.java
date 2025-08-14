package schultedev.conversationalai4j.demo;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/** Simple WebSocket configuration for voice streaming. */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

  private final VoiceStreamHandler voiceStreamHandler;

  public WebSocketConfig(VoiceStreamHandler voiceStreamHandler) {
    this.voiceStreamHandler = voiceStreamHandler;
  }

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry
        .addHandler(voiceStreamHandler, "/voice-stream")
        .setAllowedOrigins("*"); // For demo purposes - restrict in production
  }
}
