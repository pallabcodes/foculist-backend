package com.yourorg.platform.foculist.project.application;

import com.yourorg.platform.foculist.project.domain.model.Project;
import com.yourorg.platform.foculist.project.domain.model.ProjectNotFoundException;
import com.yourorg.platform.foculist.project.domain.model.Task;
import com.yourorg.platform.foculist.project.domain.port.ProjectRepositoryPort;
import com.yourorg.platform.foculist.project.domain.port.TaskRepositoryPort;
import com.yourorg.platform.foculist.project.domain.service.ProjectAuthorizer;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TaskApplicationService {
    private final TaskRepositoryPort taskRepository;
    private final ProjectRepositoryPort projectRepository;
    private final ProjectAuthorizer authorizer;
    private final Clock clock = Clock.systemUTC();

    @Transactional
    public Task createTask(UUID userId, String orgRole, UUID projectId, CreateTaskCommand command, String tenantId) {
        Project project = projectRepository.findByIdAndTenantId(projectId, tenantId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found"));

        if (!authorizer.can(userId, orgRole, projectId, null, "task:create", tenantId, project.permissionSchemeId())) {
            throw new AccessDeniedException("User does not have permission to create tasks in this project");
        }

        Task task = Task.create(
                tenantId,
                projectId,
                command.title(),
                command.description(),
                command.status(),
                command.priority(),
                command.type(),
                command.storyPoints(),
                command.assigneeId(),
                userId, // reporter
                command.dueDate(),
                Instant.now(clock)
        );

        return taskRepository.save(task);
    }

    @Transactional
    public Task updateTask(UUID userId, String orgRole, UUID taskId, UpdateTaskCommand command, String tenantId) {
        Task task = taskRepository.findByIdAndTenantId(taskId, tenantId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        Project project = projectRepository.findByIdAndTenantId(task.getProjectId(), tenantId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found"));

        if (!authorizer.can(userId, orgRole, task.getProjectId(), task, "task:edit", tenantId, project.permissionSchemeId())) {
            throw new AccessDeniedException("User does not have permission to edit this task");
        }

        task.update(
                command.title(),
                command.description(),
                command.status(),
                command.priority(),
                command.type(),
                command.storyPoints(),
                command.assigneeId(),
                command.dueDate(),
                Instant.now(clock)
        );

        return taskRepository.save(task);
    }

    @Transactional(readOnly = true)
    public List<Task> listTasks(UUID userId, String orgRole, UUID projectId, String tenantId, int page, int size) {
        Project project = projectRepository.findByIdAndTenantId(projectId, tenantId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found"));

        if (!authorizer.can(userId, orgRole, projectId, null, "task:view", tenantId, project.permissionSchemeId())) {
             throw new AccessDeniedException("User does not have permission to view tasks in this project");
        }

        return taskRepository.findByProjectIdAndTenantId(projectId, tenantId, page, size);
    }
}
