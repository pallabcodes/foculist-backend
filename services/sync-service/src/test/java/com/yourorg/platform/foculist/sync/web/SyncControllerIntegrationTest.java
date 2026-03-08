package com.yourorg.platform.foculist.sync.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:syncit;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration-h2"
})
@AutoConfigureMockMvc
@WithMockUser(username = "integration")
class SyncControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetTables() {
        jdbcTemplate.execute("delete from sync_change_event");
        jdbcTemplate.execute("delete from sync_push_envelope");
        jdbcTemplate.execute("delete from sync_device_cursor");
    }

    @Test
    void appliesFlywayMigrationsForSyncDomainTables() {
        Integer applied = jdbcTemplate.queryForObject(
                "select count(*) from flyway_schema_history where version = '2' and success = true",
                Integer.class
        );
        assertThat(applied).isNotNull();
        assertThat(applied).isEqualTo(1);
    }

    @Test
    void pushThenPullReturnsPersistedTenantScopedChanges() throws Exception {
        String pushBody = objectMapper.writeValueAsString(Map.of(
                "deviceId", "device-a",
                "pendingChanges", 2,
                "payloadVersion", "v1",
                "payload", Map.of("entity", "task", "count", 2),
                "clientSyncTime", "2026-02-21T11:00:00Z"
        ));

        mockMvc.perform(post("/v1/sync/push")
                        .with(csrf())
                        .header("X-Tenant-ID", "tenant-a")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pushBody))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Tenant-ID", "tenant-a"))
                .andExpect(jsonPath("$.accepted").value(true))
                .andExpect(jsonPath("$.deviceId").value("device-a"))
                .andExpect(jsonPath("$.pendingChanges").value(2))
                .andExpect(jsonPath("$.tenantId").value("tenant-a"));

        String pullBody = objectMapper.writeValueAsString(Map.of(
                "deviceId", "device-a",
                "lastSync", "1970-01-01T00:00:00Z"
        ));

        mockMvc.perform(post("/v1/sync/pull")
                        .with(csrf())
                        .header("X-Tenant-ID", "tenant-a")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pullBody))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Tenant-ID", "tenant-a"))
                .andExpect(jsonPath("$.changeCount").value(1))
                .andExpect(jsonPath("$.changes[0].type").value("BATCH"))
                .andExpect(jsonPath("$.tenantId").value("tenant-a"));
    }

    @Test
    void keepsChangesIsolatedByTenant() throws Exception {
        String pushBody = objectMapper.writeValueAsString(Map.of(
                "deviceId", "device-a",
                "pendingChanges", 1,
                "payloadVersion", "v1",
                "payload", Map.of("entity", "calendar_event"),
                "clientSyncTime", "2026-02-21T11:10:00Z"
        ));

        mockMvc.perform(post("/v1/sync/push")
                        .with(csrf())
                        .header("X-Tenant-ID", "tenant-a")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pushBody))
                .andExpect(status().isOk());

        String pullBody = objectMapper.writeValueAsString(Map.of(
                "deviceId", "device-a",
                "lastSync", "2026-02-21T00:00:00Z"
        ));

        mockMvc.perform(post("/v1/sync/pull")
                        .with(csrf())
                        .header("X-Tenant-ID", "tenant-b")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pullBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.changeCount").value(0))
                .andExpect(jsonPath("$.changes").isEmpty())
                .andExpect(jsonPath("$.tenantId").value("tenant-b"));
    }

    @Test
    void rejectsMissingTenantIdentifier() throws Exception {
        String pullBody = objectMapper.writeValueAsString(Map.of(
                "deviceId", "device-a",
                "lastSync", "2026-02-21T00:00:00Z"
        ));

        mockMvc.perform(post("/v1/sync/pull")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pullBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("TENANCY_RESOLUTION_ERROR"))
                .andExpect(jsonPath("$.message").value("Tenant identifier is missing"));
    }
}
