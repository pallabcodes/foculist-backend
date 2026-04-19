package com.yourorg.platform.foculist.planning.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourorg.platform.foculist.planning.application.PlanningApplicationService;
import com.yourorg.platform.foculist.planning.application.TaskView;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Import;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.when;

@org.junit.jupiter.api.Disabled("Disabled pending servlet context setup for TenantContext/RequestId in slice tests")
@WebMvcTest(controllers = PlanningController.class)
@Import({
        TaskResponseMapperRegistry.class,
        DefaultTaskResponseMapper.class,
        V2TaskResponseMapper.class,
        ApiExceptionHandler.class
})
class PlanningControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    PlanningApplicationService planningApplicationService;

    @Autowired
    ObjectMapper objectMapper;

    @BeforeEach
    void setupTenant() {
        com.yourorg.platform.foculist.tenancy.TenantContext.set("tenant-a");
    }

    @AfterEach
    void clearTenant() {
        com.yourorg.platform.foculist.tenancy.TenantContext.clear();
    }

    @Test
    void returnsV1Envelope() throws Exception {
        TaskView tv = new TaskView(
                java.util.UUID.randomUUID(),
                null,
                null,
                null,
                "title",
                "desc",
                "RUNNING",
                "HIGH",
                Instant.parse("2026-02-01T00:00:00Z"),
                "tenant-a",
                1L
        );
        when(planningApplicationService.listTasks(Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt(), Mockito.any()))
                .thenReturn(List.of(tv));

        mockMvc.perform(get("/v1/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"));
    }

    @Test
    void returnsV2Envelope() throws Exception {
        TaskView tv = new TaskView(
                java.util.UUID.randomUUID(),
                null,
                null,
                null,
                "title",
                "desc",
                "RUNNING",
                "HIGH",
                Instant.parse("2026-02-01T00:00:00Z"),
                "tenant-a",
                1L
        );
        when(planningApplicationService.listTasks(Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt(), Mockito.any()))
                .thenReturn(List.of(tv));

        mockMvc.perform(get("/v1/tasks").header("Accept-Version", "v2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"));
    }

    @Test
    void rejectsUnknownVersion() throws Exception {
        mockMvc.perform(get("/v1/tasks").header("Accept-Version", "v9"))
                .andExpect(status().isNotAcceptable())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.errors[0].code").value("UNSUPPORTED_VERSION"));
    }
}
