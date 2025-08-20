package schultedev.conversationalai4j.demo;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for the ConversationalAI4J demo application. Groups related
 * configuration values using Spring's @ConfigurationProperties.
 */
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppConfig {

  // Ollama Configuration
  private String ollamaBaseUrl = "http://localhost:11434";
  private String ollamaModelName = "llama3.2:3b";

  // Speech Configuration
  private boolean speechEnabled = false;
  private String speechSttModelPath;
  private String speechTtsModelPath;
  private String speechWhisperModelPath = "/app/models/whisper/ggml-base.en.bin";
  private String speechPiperModelPath = "/app/models/piper/en_US-amy-medium.onnx";
  private String speechPiperConfigPath = "/app/models/piper/en_US-amy-medium.onnx.json";

  // Application Configuration
  private String systemPrompt =
      "You are a helpful AI assistant in a demo application. Keep responses concise and friendly.";
  private double temperature = 0.7;

  // Getters and setters
  public String getOllamaBaseUrl() {
    return ollamaBaseUrl;
  }

  public void setOllamaBaseUrl(String ollamaBaseUrl) {
    this.ollamaBaseUrl = ollamaBaseUrl;
  }

  public String getOllamaModelName() {
    return ollamaModelName;
  }

  public void setOllamaModelName(String ollamaModelName) {
    this.ollamaModelName = ollamaModelName;
  }

  public boolean isSpeechEnabled() {
    return speechEnabled;
  }

  public void setSpeechEnabled(boolean speechEnabled) {
    this.speechEnabled = speechEnabled;
  }

  public String getSpeechSttModelPath() {
    return speechSttModelPath;
  }

  public void setSpeechSttModelPath(String speechSttModelPath) {
    this.speechSttModelPath = speechSttModelPath;
  }

  public String getSpeechTtsModelPath() {
    return speechTtsModelPath;
  }

  public void setSpeechTtsModelPath(String speechTtsModelPath) {
    this.speechTtsModelPath = speechTtsModelPath;
  }

  public String getSpeechWhisperModelPath() {
    return speechWhisperModelPath;
  }

  public void setSpeechWhisperModelPath(String speechWhisperModelPath) {
    this.speechWhisperModelPath = speechWhisperModelPath;
  }

  public String getSpeechPiperModelPath() {
    return speechPiperModelPath;
  }

  public void setSpeechPiperModelPath(String speechPiperModelPath) {
    this.speechPiperModelPath = speechPiperModelPath;
  }

  public String getSpeechPiperConfigPath() {
    return speechPiperConfigPath;
  }

  public void setSpeechPiperConfigPath(String speechPiperConfigPath) {
    this.speechPiperConfigPath = speechPiperConfigPath;
  }

  public String getSystemPrompt() {
    return systemPrompt;
  }

  public void setSystemPrompt(String systemPrompt) {
    this.systemPrompt = systemPrompt;
  }

  public double getTemperature() {
    return temperature;
  }

  public void setTemperature(double temperature) {
    this.temperature = temperature;
  }
}
