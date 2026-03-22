package com.yourorg.platform.foculist.project.web;

import com.yourorg.platform.foculist.project.application.CreateTaskCommand;
import com.yourorg.platform.foculist.project.application.TaskApplicationService;
import com.yourorg.platform.foculist.project.application.UpdateTaskCommand;
import com.yourorg.platform.foculist.project.domain.model.Task;
import com.yourorg.platform.foculist.tenancy.TenantContext;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class TaskController {

    private final TaskApplicationService taskService;

    @PostMapping("/projects/{projectId}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public Task createTask(
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateTaskRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getClaimAsString("userId"));
        String orgRole = jwt.getClaimAsString("orgRole");

        return taskService.createTask(
                userId,
                orgRole,
                projectId,
                new CreateTaskCommand(
                        request.title(),
                        request.description(),
                        request.status(),
                        request.priority(),
                        request.type(),
                        request.storyPoints(),
                        request.assigneeId(),
                        request.dueDate()
                ),
                TenantContext.require()
        );
    }

    @PatchMapping("/tasks/{taskId}")
    public Task updateTask(
            @PathVariable UUID taskId,
            @Valid @RequestBody UpdateTaskRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getClaimAsString("userId"));
        String orgRole = jwt.getClaimAsString("orgRole");

        return taskService.updateTask(
                userId,
                orgRole,
                taskId,
                new UpdateTaskCommand(
                        request.title(),
                        request.description(),
                        request.status(),
                        request.priority(),
                        request.type(),
                        request.storyPoints(),
                        request.assigneeId(),
                        request.dueDate()
                ),
                TenantContext.require()
        );
    }

    @GetMapping("/projects/{projectId}/tasks")
    public List<Task> listTasks(
            @PathVariable UUID projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getClaimAsString("userId"));
        String orgRole = jwt.getClaimAsString("orgRole");

        return taskService.listTasks(
                userId,
                orgRole,
                projectId,
                TenantContext.require(),
                page,
                size
        );
    }

    public record CreateTaskRequest(
            String title,
            String description,
            String status,
            String priority,
            String type,
            Integer storyPoints,
            UUID assigneeId,
            java.time.LocalDate dueDate
    ) {}

    public record UpdateTaskRequest(
            String title,
            String description,
            String status,
            String priority,
            String type,
            Integer storyPoints,
            UUID assigneeId,
            java.time.LocalDate dueDate
    ) {}
}
