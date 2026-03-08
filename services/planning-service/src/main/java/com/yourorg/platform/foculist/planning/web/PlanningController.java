package com.yourorg.platform.foculist.planning.web;

import com.yourorg.platform.foculist.planning.application.CreateTaskCommand;
import com.yourorg.platform.foculist.planning.application.PlanningApplicationService;
import com.yourorg.platform.foculist.planning.application.SprintView;
import com.yourorg.platform.foculist.planning.application.TaskView;
import com.yourorg.platform.foculist.tenancy.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import com.yourorg.platform.foculist.planning.domain.model.EventStore;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import java.util.UUID;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/v1")
@Validated
public class PlanningController {
    private final PlanningApplicationService planningApplicationService;
    private final TaskResponseMapperRegistry taskResponseMapperRegistry;

    public PlanningController(PlanningApplicationService planningApplicationService, TaskResponseMapperRegistry taskResponseMapperRegistry) {
        this.planningApplicationService = planningApplicationService;
        this.taskResponseMapperRegistry = taskResponseMapperRegistry;
    }

    @GetMapping("/sprints")
    public List<SprintView> listSprints(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        int boundedSize = Math.min(Math.max(size, 1), 200);
        int boundedPage = Math.max(page, 0);
        return planningApplicationService.listSprints(TenantContext.require(), boundedPage, boundedSize);
    }

    @GetMapping("/workflow/statuses")
    public List<String> workflowStatuses() {
        return planningApplicationService.workflowStatuses();
    }

    @PostMapping("/tasks")
    public ResponseEntity<TaskView> createTask(@Valid @RequestBody CreateTaskRequest request) {
        TaskView created = planningApplicationService.createTask(
                TenantContext.require(),
                new CreateTaskCommand(
                        request.title(),
                        request.description(),
                        request.status(),
                        request.priority(),
                        request.sprintId()
                )
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/tasks")
    public ApiResponse<?> listTasks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String after,
            @RequestHeader(value = "Accept-Version", required = false) String version
    ) {
        int boundedSize = Math.min(Math.max(size, 1), 200);
        int boundedPage = Math.max(page, 0);
        List<TaskView> tasks = planningApplicationService.listTasks(TenantContext.require(), boundedPage, boundedSize, after);
        boolean hasMore = tasks.size() == boundedSize;
        String nextCursor = hasMore ? buildCursor(tasks.get(tasks.size() - 1)) : null;
        String requestId = resolveRequestId();
        TaskResponseMapper mapper = taskResponseMapperRegistry.resolve(version);
        return mapper.toResponse(tasks, boundedPage, boundedSize, nextCursor, hasMore, requestId);
    }

    private String buildCursor(TaskView last) {
        return last.createdAt().toString() + "|" + last.id();
    }

    private String resolveRequestId() {
        Object attr = org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes()
                .getAttribute(com.yourorg.platform.foculist.tenancy.web.RequestIdFilter.REQUEST_ID_MDC_KEY, 0);
        if (attr instanceof String s && !s.isBlank()) {
            return s;
        }
        return java.util.UUID.randomUUID().toString();
    }

    @PreAuthorize("hasPermission(#taskId, 'Task', 'READ')")
    @GetMapping("/tasks/{taskId}/events")
    public ResponseEntity<List<EventStore>> listTaskEvents(@PathVariable UUID taskId) {
        return ResponseEntity.ok(planningApplicationService.listTaskEvents(TenantContext.require(), taskId));
    }

    @PreAuthorize("hasPermission(#taskId, 'Task', 'WRITE')")
    @PutMapping("/tasks/{taskId}")
    public ResponseEntity<TaskView> updateTask(@PathVariable UUID taskId, @Valid @RequestBody UpdateTaskRequest request) {
        TaskView updated = planningApplicationService.updateTask(
                TenantContext.require(),
                taskId,
                new com.yourorg.platform.foculist.planning.application.UpdateTaskCommand(
                        request.sprintId(),
                        request.title(),
                        request.description(),
                        request.priority()
                )
        );
        return ResponseEntity.ok(updated);
    }

    @PreAuthorize("hasPermission(#taskId, 'Task', 'WRITE')")
    @PatchMapping("/tasks/{taskId}/status")
    public ResponseEntity<TaskView> updateTaskStatus(@PathVariable UUID taskId, @Valid @RequestBody UpdateTaskStatusRequest request) {
        return ResponseEntity.ok(planningApplicationService.updateTaskStatus(TenantContext.require(), taskId, request.status()));
    }

    @PreAuthorize("hasPermission(#taskId, 'Task', 'ADMIN')")
    @DeleteMapping("/tasks/{taskId}")
    public ResponseEntity<Void> deleteTask(@PathVariable UUID taskId) {
        planningApplicationService.deleteTask(TenantContext.require(), taskId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/sprints")
    public ResponseEntity<SprintView> createSprint(@Valid @RequestBody CreateSprintRequest request) {
        SprintView created = planningApplicationService.createSprint(
                TenantContext.require(),
                new com.yourorg.platform.foculist.planning.application.CreateSprintCommand(
                        request.name(),
                        request.startDate(),
                        request.endDate()
                )
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PreAuthorize("hasPermission(#sprintId, 'Sprint', 'WRITE')")
    @PutMapping("/sprints/{sprintId}")
    public ResponseEntity<SprintView> updateSprint(@PathVariable UUID sprintId, @Valid @RequestBody CreateSprintRequest request) {
        SprintView updated = planningApplicationService.updateSprint(
                TenantContext.require(),
                sprintId,
                new com.yourorg.platform.foculist.planning.application.CreateSprintCommand(
                        request.name(),
                        request.startDate(),
                        request.endDate()
                )
        );
        return ResponseEntity.ok(updated);
    }

    @PreAuthorize("hasPermission(#sprintId, 'Sprint', 'ADMIN')")
    @DeleteMapping("/sprints/{sprintId}")
    public ResponseEntity<Void> deleteSprint(@PathVariable UUID sprintId) {
        planningApplicationService.deleteSprint(TenantContext.require(), sprintId);
        return ResponseEntity.noContent().build();
    }

    public record CreateTaskRequest(
            @NotBlank String title,
            String description,
            String status,
            String priority,
            String sprintId
    ) {
    }

    public record UpdateTaskRequest(
            @NotBlank String title,
            String description,
            String priority,
            String sprintId
    ) {
    }

    public record UpdateTaskStatusRequest(@NotBlank String status) {}

    public record CreateSprintRequest(
            @NotBlank String name,
            @NotBlank String startDate,
            @NotBlank String endDate
    ) {
    }
}
