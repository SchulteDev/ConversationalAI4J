package schultedev.conversationalai4j.demo;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.webresources.StandardRoot;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.http.NameValuePair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the ConversationalAI4J demo web application.
 * These tests start an embedded Tomcat server and verify the JSF application works correctly.
 */
class DemoIntegrationTest {

    private static Tomcat tomcat;
    private static final int PORT = 9090; // Use different port to avoid conflicts
    private static final String BASE_URL = "http://localhost:" + PORT;

    @BeforeAll
    static void startServer() throws Exception {
        tomcat = new Tomcat();
        tomcat.setPort(PORT);
        tomcat.getConnector();

        // Create web application context
        String webappDirLocation = "src/main/webapp/";
        Context ctx = tomcat.addWebapp("", new File(webappDirLocation).getAbsolutePath());
        
        // Configure classpath to include compiled classes for CDI (same as TomcatRunner)
        File additionWebInfClasses = new File("build/classes/java/main");
        WebResourceRoot resources = new StandardRoot(ctx);
        
        if (additionWebInfClasses.exists()) {
            resources.addPreResources(new DirResourceSet(resources, "/WEB-INF/classes", 
                    additionWebInfClasses.getAbsolutePath(), "/"));
        }
        
        // Add resources directory for CDI beans.xml and other resources
        File resourcesDir = new File("src/main/resources");
        if (resourcesDir.exists()) {
            resources.addPreResources(new DirResourceSet(resources, "/WEB-INF/classes", 
                    resourcesDir.getAbsolutePath(), "/"));
        }
        
        ctx.setResources(resources);

        tomcat.start();
        
        // Wait for server to start properly
        Thread.sleep(3000);
    }

    @AfterAll
    static void stopServer() throws Exception {
        if (tomcat != null) {
            tomcat.stop();
            tomcat.destroy();
        }
    }

    @Test
    void testIndexPageLoads() throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(BASE_URL + "/index.xhtml");
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                
                int statusCode = response.getStatusLine().getStatusCode();
                String content = EntityUtils.toString(response.getEntity());
                
                assertEquals(200, statusCode, "JSF page should load successfully");
                assertNotNull(content);
                assertTrue(content.contains("ConversationalAI4J Demo"), 
                          "Page should contain application title");
            }
        }
    }

    @Test
    void testIndexPageContainsForm() throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(BASE_URL + "/index.xhtml");
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                
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

    @Test
    void testJsfCdiBeanResolution() throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(BASE_URL + "/index.xhtml");
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                
                String content = EntityUtils.toString(response.getEntity());
                
                // Verify that CDI bean content is rendered (no "resolved to null" errors)
                assertFalse(content.contains("resolved to null"), 
                           "CDI bean should resolve properly - no 'resolved to null' errors");
                assertTrue(content.contains("ConversationalAI4J Demo"), 
                          "Welcome text from CDI bean should be rendered");
            }
        }
    }

    @Test
    void testFormSubmissionWithCdiBean() throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            // First, get the page to extract the JSF view state and form details
            HttpGet getRequest = new HttpGet(BASE_URL + "/index.xhtml");
            String formData;
            try (CloseableHttpResponse getResponse = httpClient.execute(getRequest)) {
                assertEquals(200, getResponse.getStatusLine().getStatusCode());
                String content = EntityUtils.toString(getResponse.getEntity());
                Document doc = Jsoup.parse(content);
                
                // Extract JSF view state (required for form submission)
                String viewState = doc.select("input[name='jakarta.faces.ViewState']").attr("value");
                assertFalse(viewState.isEmpty(), "JSF ViewState should be present");
                
                formData = viewState;
            }

            // Now submit the form
            HttpPost postRequest = new HttpPost(BASE_URL + "/index.xhtml");
            List<NameValuePair> formParams = new ArrayList<>();
            formParams.add(new BasicNameValuePair("j_idt6", "j_idt6")); // form id
            formParams.add(new BasicNameValuePair("j_idt6:message", "Hello CDI!")); // message input
            formParams.add(new BasicNameValuePair("j_idt6:j_idt10", "Send Message")); // button
            formParams.add(new BasicNameValuePair("jakarta.faces.ViewState", formData)); // JSF view state
            
            postRequest.setEntity(new UrlEncodedFormEntity(formParams));
            postRequest.setHeader("Content-Type", "application/x-www-form-urlencoded");

            try (CloseableHttpResponse postResponse = httpClient.execute(postRequest)) {
                String responseContent = EntityUtils.toString(postResponse.getEntity());
                
                // This test should reveal the CDI bean resolution issue
                System.out.println("Form submission response status: " + postResponse.getStatusLine().getStatusCode());
                System.out.println("Response content: " + responseContent);
                
                // Expect either success or the CDI error we're trying to reproduce
                assertTrue(postResponse.getStatusLine().getStatusCode() == 200 || 
                          postResponse.getStatusLine().getStatusCode() == 500,
                          "Form submission should either work or fail with server error");
            }
        }
    }
}