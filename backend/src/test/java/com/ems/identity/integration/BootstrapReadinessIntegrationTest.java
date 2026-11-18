package com.ems.identity.integration;

import com.ems.EmployeeManagementApplication;
import com.ems.identity.service.AccountService;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.core.Ordered;
import static org.junit.jupiter.api.Assertions.*;

class BootstrapReadinessIntegrationTest {
    static class BootstrapGate implements ApplicationRunner, Ordered {
        private final CountDownLatch entered;
        private final CountDownLatch release;
        BootstrapGate(CountDownLatch entered, CountDownLatch release) { this.entered=entered;this.release=release; }
        @Override public int getOrder() { return Ordered.HIGHEST_PRECEDENCE; }
        @Override public void run(ApplicationArguments args) throws Exception {
            entered.countDown();
            if (!release.await(30,TimeUnit.SECONDS)) throw new IllegalStateException("Synthetic bootstrap gate timed out");
        }
    }
    @Test void healthRefusesReadinessUntilBootstrapHasFinished() throws Exception {
        var entered=new CountDownLatch(1);var release=new CountDownLatch(1);var port=new AtomicInteger();
        var executor=Executors.newSingleThreadExecutor();
        var application=executor.submit(()->new SpringApplicationBuilder(EmployeeManagementApplication.class)
            .registerShutdownHook(false)
            .initializers(context->context.getBeanFactory().registerSingleton("syntheticBootstrapGate",new BootstrapGate(entered,release)))
            .listeners(event->{if(event instanceof WebServerInitializedEvent started)port.set(started.getWebServer().getPort());})
            .run("--server.port=0","--server.address=127.0.0.1","--spring.datasource.url=jdbc:h2:mem:bootstrap-readiness",
                 "--EMS_BOOTSTRAP_LOGIN=synthetic-readiness","--EMS_BOOTSTRAP_PASSWORD="+UUID.randomUUID()));
        try {
            assertTrue(entered.await(30,TimeUnit.SECONDS));
            var client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
            var request=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port.get()+"/actuator/health")).timeout(Duration.ofSeconds(5)).GET().build();
            var before=client.send(request,HttpResponse.BodyHandlers.ofString());
            assertEquals(503,before.statusCode());assertTrue(before.body().contains("OUT_OF_SERVICE"));
            release.countDown();var context=application.get(30,TimeUnit.SECONDS);
            assertTrue(context.getBean(AccountService.class).findIdentity("synthetic-readiness").isPresent());
            assertEquals(200,client.send(request,HttpResponse.BodyHandlers.ofString()).statusCode());
        } finally {
            release.countDown();
            try {application.get(30,TimeUnit.SECONDS).close();} finally {executor.shutdownNow();assertTrue(executor.awaitTermination(5,TimeUnit.SECONDS));}
        }
    }
}
