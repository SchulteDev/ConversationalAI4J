package schultedev.conversationalai4j.demo;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test that starts embedded Tomcat and tests the web application.
 * This verifies that JSF pages load and render correctly.
 */
class WebAppIntegrationTest {

    private static Tomcat tomcat;
    private static final int PORT = 9090; // Use different port to avoid conflicts
    private static final String BASE_URL = "http://localhost:" + PORT;

    @BeforeAll
    static void startTomcat() throws LifecycleException {
        tomcat = new Tomcat();
        tomcat.setPort(PORT);
        tomcat.getConnector();

        // Add webapp
        String webappPath = "src/main/webapp";
        String contextPath = "";
        
        tomcat.addWebapp(contextPath, new File(webappPath).getAbsolutePath());

        tomcat.start();
        
        // Wait a moment for startup
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @AfterAll
    static void stopTomcat() throws LifecycleException {
        if (tomcat != null) {
            tomcat.stop();
            tomcat.destroy();
        }
    }

    @Test
    void testIndexPageLoads() throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(BASE_URL + "/index.xhtml");
            HttpResponse response = httpClient.execute(request);
            
            int statusCode = response.getStatusLine().getStatusCode();
            String content = EntityUtils.toString(response.getEntity());
            
            assertEquals(200, statusCode, "JSF page should load successfully");
            assertNotNull(content);
            assertTrue(content.contains("ConversationalAI4J Demo"), 
                      "Page should contain application title");
        }
    }

    @Test
    void testIndexPageContainsForm() throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(BASE_URL + "/index.xhtml");
            HttpResponse response = httpClient.execute(request);
            
            assertEquals(200, response.getStatusLine().getStatusCode());
            
            String content = EntityUtils.toString(response.getEntity());
            Document doc = Jsoup.parse(content);
            
            // Verify JSF form elements are rendered
            assertFalse(doc.select("form").isEmpty(), "Page should contain a form");
            assertFalse(doc.select("input[type=text]").isEmpty(), "Page should contain text input");
            assertTrue(doc.title().contains("ConversationalAI4J Demo"), "Page title should be correct");
        }
    }
}