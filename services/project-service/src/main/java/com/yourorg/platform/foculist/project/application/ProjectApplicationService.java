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
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectApplicationService {
    private final ProjectRepositoryPort projectRepository;
    private final ProjectSettingsRepositoryPort projectSettingsRepository;
    private final Clock clock;

    public ProjectApplicationService(
            ProjectRepositoryPort projectRepository,
            ProjectSettingsRepositoryPort projectSettingsRepository
    ) {
        this(projectRepository, projectSettingsRepository, Clock.systemUTC());
    }

    ProjectApplicationService(
            ProjectRepositoryPort projectRepository,
            ProjectSettingsRepositoryPort projectSettingsRepository,
            Clock clock
    ) {
        this.projectRepository = projectRepository;
        this.projectSettingsRepository = projectSettingsRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<ProjectSummaryView> listProjects(String tenantId, int page, int size) {
        return projectRepository.findByTenantId(tenantId, page, size).stream()
                .map(this::toSummaryView)
                .toList();
    }

    @Transactional
    public ProjectSummaryView createProject(String tenantId, CreateProjectCommand command) {
        Instant now = Instant.now(clock);
        Project project = Project.create(
                tenantId,
                command.name(),
                command.description(),
                ProjectStatus.from(command.status()),
                ProjectPriority.from(command.priority()),
                parseDueDate(command.dueDate(), now),
                now
        );
        Project saved = projectRepository.save(project);
        projectSettingsRepository.save(ProjectSettings.createDefault(saved.id(), tenantId, now));
        return toSummaryView(saved);
    }

    @Transactional
    public ProjectSettingsView updateSettings(
            String tenantId,
            UUID projectId,
            UpdateProjectSettingsCommand command
    ) {
        Project project = projectRepository.findByIdAndTenantId(projectId, tenantId)
                .orElseThrow(() -> new ProjectNotFoundException("Project does not exist for tenant: " + projectId));

        Instant now = Instant.now(clock);
        ProjectSettings current = projectSettingsRepository.findByProjectIdAndTenantId(project.id(), tenantId)
                .orElseGet(() -> ProjectSettings.createDefault(project.id(), tenantId, now));

        ProjectSettings saved = projectSettingsRepository.save(current.update(
                command.workflowStatuses(),
                ProjectDefaultView.fromNullable(command.defaultView()),
                now
        ));
        return toSettingsView(saved);
    }

    private LocalDate parseDueDate(String rawDueDate, Instant now) {
        if (rawDueDate == null || rawDueDate.isBlank()) {
            return null;
        }
        try {
            LocalDate dueDate = LocalDate.parse(rawDueDate.trim());
            LocalDate today = LocalDate.ofInstant(now, ZoneOffset.UTC);
            if (dueDate.isBefore(today)) {
                throw new ProjectDomainException("dueDate cannot be in the past");
            }
            return dueDate;
        } catch (DateTimeParseException ex) {
            throw new ProjectDomainException("Invalid dueDate format. Expected yyyy-MM-dd");
        }
    }

    private ProjectSummaryView toSummaryView(Project project) {
        return new ProjectSummaryView(
                project.id(),
                project.name(),
                project.description(),
                project.status().name(),
                project.priority().name(),
                project.dueDate(),
                project.tenantId()
        );
    }

    private ProjectSettingsView toSettingsView(ProjectSettings settings) {
        return new ProjectSettingsView(
                settings.projectId(),
                settings.workflowStatuses(),
                settings.defaultView().name(),
                settings.tenantId(),
                settings.updatedAt()
        );
    }
}
