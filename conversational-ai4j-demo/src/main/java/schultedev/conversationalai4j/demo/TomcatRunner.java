package schultedev.conversationalai4j.demo;

import org.apache.catalina.startup.Tomcat;

import java.io.File;

/**
 * Simple runner to start the demo application with embedded Tomcat
 */
public class TomcatRunner {
    
    public static void main(String[] args) {
        try {
            Tomcat tomcat = new Tomcat();
            tomcat.setPort(8080);
            
            // Enable connector
            tomcat.getConnector();
            
            // Try different paths to find webapp directory
            String webappDir = "conversational-ai4j-demo/src/main/webapp";
            File webappFile = new File(webappDir);
            
            if (!webappFile.exists()) {
                webappDir = "src/main/webapp";
                webappFile = new File(webappDir);
            }
            
            System.out.println("Current directory: " + new File(".").getAbsolutePath());
            System.out.println("Looking for webapp at: " + webappFile.getAbsolutePath());
            System.out.println("Webapp exists: " + webappFile.exists());
            
            if (!webappFile.exists()) {
                System.err.println("ERROR: Could not find webapp directory!");
                return;
            }
            
            System.out.println("Adding webapp...");
            tomcat.addWebapp("", webappFile.getAbsolutePath());
            
            System.out.println("Starting Tomcat...");
            tomcat.start();
            
            System.out.println("SUCCESS: ConversationalAI4J Demo started on http://localhost:8080");
            System.out.println("Try: http://localhost:8080/index.xhtml");
            System.out.println("Press Ctrl+C to stop");
            
            tomcat.getServer().await();
            
        } catch (Exception e) {
            System.err.println("ERROR starting Tomcat: " + e.getMessage());
            e.printStackTrace();
        }
    }
}