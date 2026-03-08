package com.yourorg.platform.foculist.gateway.web;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class GatewayResourceRouteIntegrationTest {
    private static final MockWebServer RESOURCE_SERVER = startMockServer();

    @Autowired
    private WebTestClient webTestClient;

    @org.springframework.boot.test.mock.mockito.MockBean
    private org.springframework.security.oauth2.jwt.ReactiveJwtDecoder jwtDecoder;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("app.gateway.downstream.resource", () -> RESOURCE_SERVER.url("/").toString());
    }

    @AfterAll
    static void shutdownServer() throws IOException {
        RESOURCE_SERVER.shutdown();
    }

    @Test
    void rewritesResourcePathAndPropagatesTenantHeader() throws Exception {
        RESOURCE_SERVER.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("[{\"id\":\"bookmark-1\",\"title\":\"Docs\"}]"));

        webTestClient.mutateWith(org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt()).get()
                .uri("/api/resources/bookmarks")
                .header("X-Tenant-ID", "tenant-r")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("X-Tenant-ID", "tenant-r")
                .expectBody()
                .jsonPath("$[0].id").isEqualTo("bookmark-1");

        RecordedRequest forwarded = RESOURCE_SERVER.takeRequest(2, TimeUnit.SECONDS);
        assertThat(forwarded).isNotNull();
        assertThat(forwarded.getMethod()).isEqualTo("GET");
        assertThat(forwarded.getPath()).isEqualTo("/v1/bookmarks");
        assertThat(forwarded.getHeader("X-Tenant-ID")).isEqualTo("tenant-r");
    }

    private static MockWebServer startMockServer() {
        MockWebServer server = new MockWebServer();
        try {
            server.start();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to start mock downstream", exception);
        }
        return server;
    }
}
