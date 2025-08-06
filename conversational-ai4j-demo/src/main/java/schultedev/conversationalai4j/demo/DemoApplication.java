package schultedev.conversationalai4j.demo;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "conversational-ai4j-demo",
    mixinStandardHelpOptions = true,
    version = "1.0-SNAPSHOT",
    description = "Demo application for ConversationalAI4J library")
public class DemoApplication implements Runnable {

  @Option(
      names = {"-c", "--config"},
      description = "Configuration file path")
  private String configPath = "application.properties";

  @Override
  public void run() {
    System.out.println("ConversationalAI4J Demo Application");
    System.out.println("=====================================");
    System.out.println("Library initialization coming soon...");

    // TODO: Initialize ConversationalAI pipeline
    // TODO: Start voice interaction loop
  }

  public static void main(String[] args) {
    int exitCode = new CommandLine(new DemoApplication()).execute(args);
    System.exit(exitCode);
  }
}
