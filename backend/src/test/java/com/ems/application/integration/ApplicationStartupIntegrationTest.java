package com.ems.application.integration;

import com.ems.EmployeeManagementApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** Verifies the actual entry point, embedded HTTP server and database wiring together. */
class ApplicationStartupIntegrationTest {
    private static final AtomicReference<ConfigurableApplicationContext> STARTED = new AtomicReference<>();

    public static class CaptureContext implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext context) {
            if (context.getEnvironment().getProperty("ems.startup-test", Boolean.class, false)) {
                STARTED.set(context);
            }
        }
    }

    @Test
    void mainStartsHealthyServerWithWorkingPersistenceAndClosesCleanly() throws Exception {
        try {
            EmployeeManagementApplication.main(new String[] {
                "--server.port=0",
                "--server.address=127.0.0.1",
                "--spring.datasource.url=jdbc:h2:mem:startup-" + UUID.randomUUID(),
                "--spring.jpa.hibernate.ddl-auto=create-drop",
                "--ems.startup-test=true"
            });
            var context = (ServletWebServerApplicationContext) STARTED.get();
            assertNotNull(context);
            String base = "http://127.0.0.1:" + context.getWebServer().getPort();
            var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
            var mapper = context.getBean(ObjectMapper.class);

            var health = client.send(HttpRequest.newBuilder(URI.create(base + "/actuator/health"))
                .timeout(Duration.ofSeconds(10)).GET().build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(200, health.statusCode());
            assertEquals("UP", mapper.readTree(health.body()).path("status").asText());

            var created = client.send(HttpRequest.newBuilder(URI.create(base + "/api/v1/employees"))
                .timeout(Duration.ofSeconds(10)).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("""
                    {"firstName":"Synthetic","lastName":"Startup","email":"startup@example.com",
                     "department":"IT","jobTitle":"Engineer","status":"ACTIVE"}
                    """)).build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(201, created.statusCode());
            long id = mapper.readTree(created.body()).path("id").asLong();
            var stored = client.send(HttpRequest.newBuilder(URI.create(base + "/api/v1/employees/" + id))
                .timeout(Duration.ofSeconds(10)).GET().build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(200, stored.statusCode());
            assertEquals("startup@example.com", mapper.readTree(stored.body()).path("email").asText());
            assertEquals("Synthetic", mapper.readTree(stored.body()).path("firstName").asText());
        } finally {
            var context = STARTED.getAndSet(null);
            if (context != null) {
                context.close();
                assertFalse(context.isActive(), "The temporary application context must stop");
            }
        }
    }
}
