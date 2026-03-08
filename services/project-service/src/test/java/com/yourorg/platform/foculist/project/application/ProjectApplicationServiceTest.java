package com.yourorg.platform.foculist.project.application;

import com.yourorg.platform.foculist.project.domain.model.Project;
import com.yourorg.platform.foculist.project.domain.model.ProjectDefaultView;
import com.yourorg.platform.foculist.project.domain.model.ProjectDomainException;
import com.yourorg.platform.foculist.project.domain.model.ProjectNotFoundException;
import com.yourorg.platform.foculist.project.domain.model.ProjectPriority;
import com.yourorg.platform.foculist.project.domain.model.ProjectSettings;
import com.yourorg.platform.foculist.project.domain.model.ProjectStatus;
import com.yourorg.platform.foculist.project.domain.port.ProjectRepositoryPort;
import com.yourorg.platform.foculist.project.domain.port.ProjectSettingsRepositoryPort;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
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

class ProjectApplicationServiceTest {

    @Test
    void createsProjectWithDefaultsAndSettings() {
        Instant now = Instant.parse("2026-02-01T10:00:00Z");
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        ProjectRepositoryPort projectRepository = mock(ProjectRepositoryPort.class);
        ProjectSettingsRepositoryPort settingsRepository = mock(ProjectSettingsRepositoryPort.class);

        when(projectRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(settingsRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ProjectApplicationService service = new ProjectApplicationService(projectRepository, settingsRepository, clock);

        ProjectSummaryView created = service.createProject(
                "tenant-a",
                new CreateProjectCommand(
                        "Design System Revamp",
                        "Unify component standards",
                        null,
                        "HIGH",
                        "2026-02-10"
                )
        );

        assertThat(created.tenantId()).isEqualTo("tenant-a");
        assertThat(created.status()).isEqualTo("RUNNING");
        assertThat(created.priority()).isEqualTo("HIGH");
        assertThat(created.dueDate()).isEqualTo(LocalDate.parse("2026-02-10"));

        ArgumentCaptor<ProjectSettings> settingsCaptor = ArgumentCaptor.forClass(ProjectSettings.class);
        verify(settingsRepository).save(settingsCaptor.capture());
        assertThat(settingsCaptor.getValue().defaultView()).isEqualTo(ProjectDefaultView.BOARD);
        assertThat(settingsCaptor.getValue().workflowStatuses())
                .containsExactly("TODO", "IN_PROGRESS", "REVIEW", "DONE");
    }

    @Test
    void rejectsPastDueDate() {
        Instant now = Instant.parse("2026-02-01T10:00:00Z");
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        ProjectRepositoryPort projectRepository = mock(ProjectRepositoryPort.class);
        ProjectSettingsRepositoryPort settingsRepository = mock(ProjectSettingsRepositoryPort.class);
        ProjectApplicationService service = new ProjectApplicationService(projectRepository, settingsRepository, clock);

        assertThatThrownBy(() -> service.createProject(
                "tenant-a",
                new CreateProjectCommand("Platform Cleanup", null, null, null, "2026-01-01")
        )).isInstanceOf(ProjectDomainException.class).hasMessageContaining("dueDate");
    }

    @Test
    void rejectsUpdateWhenProjectIsMissingForTenant() {
        ProjectRepositoryPort projectRepository = mock(ProjectRepositoryPort.class);
        ProjectSettingsRepositoryPort settingsRepository = mock(ProjectSettingsRepositoryPort.class);
        ProjectApplicationService service = new ProjectApplicationService(projectRepository, settingsRepository);

        UUID projectId = UUID.randomUUID();
        when(projectRepository.findByIdAndTenantId(projectId, "tenant-a")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateSettings(
                "tenant-a",
                projectId,
                new UpdateProjectSettingsCommand(List.of("TODO"), "BOARD")
        )).isInstanceOf(ProjectNotFoundException.class);
    }

    @Test
    void updatesProjectSettingsForTenant() {
        Instant now = Instant.parse("2026-02-01T10:00:00Z");
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        ProjectRepositoryPort projectRepository = mock(ProjectRepositoryPort.class);
        ProjectSettingsRepositoryPort settingsRepository = mock(ProjectSettingsRepositoryPort.class);
        ProjectApplicationService service = new ProjectApplicationService(projectRepository, settingsRepository, clock);

        Project project = new Project(
                UUID.randomUUID(),
                "tenant-a",
                "Platform Reliability",
                null,
                ProjectStatus.RUNNING,
                ProjectPriority.MEDIUM,
                null,
                now,
                now,
                0L
        );
        when(projectRepository.findByIdAndTenantId(project.id(), "tenant-a")).thenReturn(Optional.of(project));
        when(settingsRepository.findByProjectIdAndTenantId(project.id(), "tenant-a"))
                .thenReturn(Optional.of(ProjectSettings.createDefault(project.id(), "tenant-a", now)));
        when(settingsRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ProjectSettingsView updated = service.updateSettings(
                "tenant-a",
                project.id(),
                new UpdateProjectSettingsCommand(List.of("todo", "in progress", "review", "done"), "list")
        );

        assertThat(updated.defaultView()).isEqualTo("LIST");
        assertThat(updated.workflowStatuses()).containsExactly("TODO", "IN_PROGRESS", "REVIEW", "DONE");
        assertThat(updated.tenantId()).isEqualTo("tenant-a");
    }
}
