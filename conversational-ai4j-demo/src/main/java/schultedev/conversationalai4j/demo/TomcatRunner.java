package schultedev.conversationalai4j.demo;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.webresources.StandardRoot;
import org.apache.catalina.webresources.DirResourceSet;

import java.io.File;
import java.util.logging.Logger;

/**
 * Main class to run the ConversationalAI4J demo web application using embedded Tomcat.
 * Provides embedded server capabilities for development and deployment.
 */
public class TomcatRunner {

    private static final Logger LOGGER = Logger.getLogger(TomcatRunner.class.getName());

    public static void main(String[] args) throws Exception {
        LOGGER.info("Starting ConversationalAI4J Demo with embedded Tomcat...");

        Tomcat tomcat = new Tomcat();
        tomcat.setPort(8080);
        tomcat.getConnector();

        // Create web application context
        String webappDirLocation = findWebappDirectory();
        Context ctx = tomcat.addWebapp("", new File(webappDirLocation).getAbsolutePath());
        
        // Configure classpath to include compiled classes for CDI
        File additionWebInfClasses = new File(findDemoModuleRoot() + "/build/classes/java/main");
        WebResourceRoot resources = new StandardRoot(ctx);
        
        if (additionWebInfClasses.exists()) {
            resources.addPreResources(new DirResourceSet(resources, "/WEB-INF/classes", 
                    additionWebInfClasses.getAbsolutePath(), "/"));
        }
        
        // Add resources directory for CDI beans.xml and other resources
        File resourcesDir = new File(findDemoModuleRoot() + "/src/main/resources");
        if (resourcesDir.exists()) {
            resources.addPreResources(new DirResourceSet(resources, "/WEB-INF/classes", 
                    resourcesDir.getAbsolutePath(), "/"));
        }
        
        ctx.setResources(resources);

        LOGGER.info("ConversationalAI4J Demo starting on http://localhost:8080");
        
        tomcat.start();
        
        LOGGER.info("ConversationalAI4J Demo started successfully!");
        LOGGER.info("Access the application at: http://localhost:8080/index.xhtml");

        // Keep the application running
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Shutting down Tomcat...");
            try {
                tomcat.stop();
                tomcat.destroy();
            } catch (Exception e) {
                LOGGER.severe("Error during shutdown: " + e.getMessage());
            }
        }));

        tomcat.getServer().await();
    }

    private static String findDemoModuleRoot() {
        File currentDir = new File(System.getProperty("user.dir"));
        
        // Check if we're already in the demo module
        if (new File(currentDir, "src/main/webapp").exists()) {
            return currentDir.getAbsolutePath();
        }
        
        // Check if we're in the root project and need to navigate to demo module
        File demoModule = new File(currentDir, "conversational-ai4j-demo");
        if (demoModule.exists() && new File(demoModule, "src/main/webapp").exists()) {
            return demoModule.getAbsolutePath();
        }
        
        throw new RuntimeException("Could not locate demo module directory with webapp folder");
    }
    
    private static String findWebappDirectory() {
        String demoRoot = findDemoModuleRoot();
        return demoRoot + "/src/main/webapp";
    }
}