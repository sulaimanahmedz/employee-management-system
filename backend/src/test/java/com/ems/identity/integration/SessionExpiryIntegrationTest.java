package com.ems.identity.integration;

import com.ems.identity.entity.AccountRole;
import com.ems.identity.service.AccountService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import org.apache.catalina.Context;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.embedded.tomcat.TomcatWebServer;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.datasource.url=jdbc:h2:mem:session-expiry", "spring.jpa.hibernate.ddl-auto=create-drop",
    "server.address=127.0.0.1", "server.servlet.session.cookie.secure=false"
})
@DirtiesContext
class SessionExpiryIntegrationTest {
    @Autowired ServletWebServerApplicationContext context;
    @Autowired AccountService accounts;
    @Autowired ObjectMapper json;

    @Test void containerExpiresInactiveSessionAndRejectsItsCsrfToken() throws Exception {
        String base = "http://127.0.0.1:" + context.getWebServer().getPort();
        var cookies = new CookieManager();
        var client = HttpClient.newBuilder().cookieHandler(cookies).connectTimeout(Duration.ofSeconds(5)).build();
        String password = UUID.randomUUID().toString();
        accounts.provision("synthetic-expiry", password, AccountRole.HR);
        JsonNode csrf = json.readTree(send(client, base, "/api/v1/auth/csrf").body());
        assertEquals(204, login(client, base, csrf, password).statusCode());
        csrf = json.readTree(send(client, base, "/api/v1/auth/csrf").body());
        assertEquals(200, send(client, base, "/api/v1/employees").statusCode());
        String sessionId = cookies.getCookieStore().getCookies().stream()
            .filter(cookie -> cookie.getName().equals("JSESSIONID")).findFirst().orElseThrow().getValue();
        var tomcat = ((TomcatWebServer) context.getWebServer()).getTomcat();
        var servletContext = (Context) tomcat.getHost().findChild("");
        var session = servletContext.getManager().findSession(sessionId);
        assertNotNull(session);
        assertEquals(1800, session.getMaxInactiveInterval(), "Production default must remain thirty minutes");
        // Shorten this synthetic session only, not application configuration or security filters.
        session.setMaxInactiveInterval(1);
        Thread.sleep(1200);
        assertEquals(401, send(client, base, "/api/v1/auth/session").statusCode());
        assertFalse(session.isValid());
        assertEquals(401, send(client, base, "/api/v1/employees").statusCode());
        assertEquals(403, login(client, base, csrf, password).statusCode(), "Expired CSRF token must not authenticate");
        JsonNode freshCsrf = json.readTree(send(client, base, "/api/v1/auth/csrf").body());
        assertEquals(204, login(client, base, freshCsrf, password).statusCode());
        assertEquals(200, send(client, base, "/api/v1/employees").statusCode());
        assertNotEquals(sessionId, cookies.getCookieStore().getCookies().stream()
            .filter(cookie -> cookie.getName().equals("JSESSIONID")).findFirst().orElseThrow().getValue());
    }
    private HttpResponse<String> send(HttpClient client, String base, String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(base + path)).timeout(Duration.ofSeconds(10)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }
    private HttpResponse<String> login(HttpClient client, String base, JsonNode csrf, String password) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(base + "/api/v1/auth/login")).timeout(Duration.ofSeconds(10))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header(csrf.get("headerName").asText(), csrf.get("token").asText())
            .POST(HttpRequest.BodyPublishers.ofString("username=synthetic-expiry&password=" + password)).build(), HttpResponse.BodyHandlers.ofString());
    }
}
