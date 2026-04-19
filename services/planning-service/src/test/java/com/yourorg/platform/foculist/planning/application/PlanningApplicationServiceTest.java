package com.yourorg.platform.foculist.planning.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourorg.platform.foculist.planning.domain.model.PlanningDomainException;
import com.yourorg.platform.foculist.planning.domain.model.Task;
import com.yourorg.platform.foculist.planning.domain.port.OutboxEventRepositoryPort;
import com.yourorg.platform.foculist.planning.domain.port.SprintRepositoryPort;
import com.yourorg.platform.foculist.planning.domain.port.TaskRepositoryPort;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlanningApplicationServiceTest {

    @Test
    void createTaskUsesTenantDefaultsAndPersistsTask() {
        SprintRepositoryPort sprintRepository = mock(SprintRepositoryPort.class);
        TaskRepositoryPort taskRepository = mock(TaskRepositoryPort.class);
        OutboxEventRepositoryPort outboxEventRepository = mock(OutboxEventRepositoryPort.class);
        Instant now = Instant.parse("2026-02-15T10:00:00Z");

        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PlanningApplicationService service = new PlanningApplicationService(
                taskRepository,
                sprintRepository,
                mock(com.yourorg.platform.foculist.planning.domain.port.EventStoreRepositoryPort.class),
                outboxEventRepository,
                mock(com.yourorg.platform.foculist.planning.domain.port.TaskSnapshotJobRepositoryPort.class),
                mock(com.yourorg.platform.foculist.planning.domain.port.BoardRepositoryPort.class),
                mock(com.yourorg.platform.foculist.planning.domain.port.BoardColumnRepositoryPort.class),
                mock(com.yourorg.platform.foculist.planning.domain.port.EpicRepositoryPort.class),
                mock(com.yourorg.platform.foculist.tenancy.feature.FeatureToggleService.class),
                new ObjectMapper(),
                Clock.fixed(now, ZoneOffset.UTC)
        );

        TaskView created = service.createTask(
                "tenant-a",
                new CreateTaskCommand("Prepare roadmap", "Scope Q2 items", null, null, null)
        );

        assertThat(created.tenantId()).isEqualTo("tenant-a");
        assertThat(created.status()).isEqualTo("TODO");
        assertThat(created.priority()).isEqualTo("MEDIUM");
        assertThat(created.createdAt()).isEqualTo(now);

        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(taskCaptor.capture());
        assertThat(taskCaptor.getValue().tenantId()).isEqualTo("tenant-a");
        verify(outboxEventRepository).save(any());
    }

    @Test
    void createTaskRejectsUnknownSprint() {
        SprintRepositoryPort sprintRepository = mock(SprintRepositoryPort.class);
        TaskRepositoryPort taskRepository = mock(TaskRepositoryPort.class);
        OutboxEventRepositoryPort outboxEventRepository = mock(OutboxEventRepositoryPort.class);
        UUID sprintId = UUID.randomUUID();

        when(sprintRepository.findByIdAndTenantId(sprintId, "tenant-a")).thenReturn(Optional.empty());

        PlanningApplicationService service = new PlanningApplicationService(
                taskRepository,
                sprintRepository,
                mock(com.yourorg.platform.foculist.planning.domain.port.EventStoreRepositoryPort.class),
                outboxEventRepository,
                mock(com.yourorg.platform.foculist.planning.domain.port.TaskSnapshotJobRepositoryPort.class),
                mock(com.yourorg.platform.foculist.planning.domain.port.BoardRepositoryPort.class),
                mock(com.yourorg.platform.foculist.planning.domain.port.BoardColumnRepositoryPort.class),
                mock(com.yourorg.platform.foculist.planning.domain.port.EpicRepositoryPort.class),
                mock(com.yourorg.platform.foculist.tenancy.feature.FeatureToggleService.class),
                new ObjectMapper(),
                Clock.systemUTC()
        );

        assertThatThrownBy(() -> service.createTask(
                "tenant-a",
                new CreateTaskCommand("Prepare roadmap", null, "TODO", "HIGH", sprintId.toString())
        )).isInstanceOf(PlanningDomainException.class)
                .hasMessageContaining("Sprint does not exist");
    }

    @Test
    void workflowStatusesExposeDomainEnumValues() {
        SprintRepositoryPort sprintRepository = mock(SprintRepositoryPort.class);
        TaskRepositoryPort taskRepository = mock(TaskRepositoryPort.class);
        OutboxEventRepositoryPort outboxEventRepository = mock(OutboxEventRepositoryPort.class);
        PlanningApplicationService service = new PlanningApplicationService(
                taskRepository,
                sprintRepository,
                mock(com.yourorg.platform.foculist.planning.domain.port.EventStoreRepositoryPort.class),
                outboxEventRepository,
                mock(com.yourorg.platform.foculist.planning.domain.port.TaskSnapshotJobRepositoryPort.class),
                mock(com.yourorg.platform.foculist.planning.domain.port.BoardRepositoryPort.class),
                mock(com.yourorg.platform.foculist.planning.domain.port.BoardColumnRepositoryPort.class),
                mock(com.yourorg.platform.foculist.planning.domain.port.EpicRepositoryPort.class),
                mock(com.yourorg.platform.foculist.tenancy.feature.FeatureToggleService.class),
                new ObjectMapper(),
                Clock.systemUTC()
        );

        List<String> statuses = service.workflowStatuses();

        assertThat(statuses).containsExactly("TODO", "IN_PROGRESS", "REVIEW", "TESTING", "DONE");
    }
}
