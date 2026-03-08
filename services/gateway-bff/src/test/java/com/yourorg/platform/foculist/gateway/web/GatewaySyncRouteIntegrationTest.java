package com.yourorg.platform.foculist.gateway.web;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class GatewaySyncRouteIntegrationTest {
    private static final MockWebServer SYNC_SERVER = startMockServer();

    @Autowired
    private WebTestClient webTestClient;

    @org.springframework.boot.test.mock.mockito.MockBean
    private org.springframework.security.oauth2.jwt.ReactiveJwtDecoder jwtDecoder;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("app.gateway.downstream.sync", () -> SYNC_SERVER.url("/").toString());
    }

    @AfterAll
    static void shutdownServer() throws IOException {
        SYNC_SERVER.shutdown();
    }

    @Test
    void rewritesSyncPushPathAndPropagatesTenantHeader() throws Exception {
        SYNC_SERVER.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"accepted\":true,\"tenantId\":\"tenant-a\"}"));

        webTestClient.mutateWith(org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt()).post()
                .uri("/api/sync/push")
                .header("X-Tenant-ID", "tenant-a")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "deviceId", "device-a",
                        "pendingChanges", 2,
                        "payloadVersion", "v1",
                        "payload", Map.of("entity", "task"),
                        "clientSyncTime", "2026-02-21T11:00:00Z"
                ))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("X-Tenant-ID", "tenant-a")
                .expectBody()
                .jsonPath("$.accepted").isEqualTo(true)
                .jsonPath("$.tenantId").isEqualTo("tenant-a");

        RecordedRequest forwarded = SYNC_SERVER.takeRequest(2, TimeUnit.SECONDS);
        assertThat(forwarded).isNotNull();
        assertThat(forwarded.getMethod()).isEqualTo("POST");
        assertThat(forwarded.getPath()).isEqualTo("/v1/sync/push");
        assertThat(forwarded.getHeader("X-Tenant-ID")).isEqualTo("tenant-a");
        assertThat(forwarded.getBody().readUtf8()).contains("\"deviceId\":\"device-a\"");
    }

    @Test
    void resolvesTenantFromQueryAndForwardsPullRequest() throws Exception {
        SYNC_SERVER.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"changeCount\":0,\"changes\":[],\"tenantId\":\"tenant-q\"}"));

        webTestClient.mutateWith(org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt()).post()
                .uri("/api/sync/pull?tenant=tenant-q")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "deviceId", "device-q",
                        "lastSync", "2026-02-21T11:10:00Z"
                ))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("X-Tenant-ID", "tenant-q")
                .expectBody()
                .jsonPath("$.changeCount").isEqualTo(0)
                .jsonPath("$.tenantId").isEqualTo("tenant-q");

        RecordedRequest forwarded = SYNC_SERVER.takeRequest(2, TimeUnit.SECONDS);
        assertThat(forwarded).isNotNull();
        assertThat(forwarded.getMethod()).isEqualTo("POST");
        assertThat(forwarded.getPath()).isEqualTo("/v1/sync/pull?tenant=tenant-q");
        assertThat(forwarded.getHeader("X-Tenant-ID")).isEqualTo("tenant-q");
    }

    @Test
    void blocksSyncRequestWhenTenantIsMissing() {
        int requestCountBefore = SYNC_SERVER.getRequestCount();

        webTestClient.mutateWith(org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt()).post()
                .uri("/api/sync/pull")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "deviceId", "device-z",
                        "lastSync", "2026-02-21T11:15:00Z"
                ))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error").isEqualTo("TENANCY_RESOLUTION_ERROR")
                .jsonPath("$.message").isEqualTo("Tenant identifier is missing");

        assertThat(SYNC_SERVER.getRequestCount()).isEqualTo(requestCountBefore);
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
