package com.yourorg.platform.foculist.planning.web;

import com.yourorg.platform.foculist.planning.application.CreateTaskCommand;
import com.yourorg.platform.foculist.planning.application.PlanningApplicationService;
import com.yourorg.platform.foculist.planning.application.SprintView;
import com.yourorg.platform.foculist.planning.application.TaskView;
import com.yourorg.platform.foculist.planning.application.BoardView;
import com.yourorg.platform.foculist.planning.application.BoardColumnView;
import com.yourorg.platform.foculist.planning.application.EpicView;
import com.yourorg.platform.foculist.planning.application.CreateBoardCommand;
import com.yourorg.platform.foculist.planning.application.CreateBoardColumnCommand;
import com.yourorg.platform.foculist.planning.application.CreateEpicCommand;
import com.yourorg.platform.foculist.planning.application.UpdateTaskPlanningCommand;
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
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/v1")
@Validated
public class PlanningController {
    private final PlanningApplicationService planningApplicationService;
    private final TaskResponseMapperRegistry taskResponseMapperRegistry;

    public PlanningController(PlanningApplicationService planningApplicationService,
            TaskResponseMapperRegistry taskResponseMapperRegistry) {
        this.planningApplicationService = planningApplicationService;
        this.taskResponseMapperRegistry = taskResponseMapperRegistry;
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/sprints")
    public List<SprintView> listSprints(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        int boundedSize = Math.min(Math.max(size, 1), 200);
        int boundedPage = Math.max(page, 0);
        return planningApplicationService.listSprints(TenantContext.require(), boundedPage, boundedSize);
    }

    @GetMapping("/workflow/statuses")
    public List<String> workflowStatuses() {
        return planningApplicationService.workflowStatuses();
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/tasks")
    public ResponseEntity<TaskView> createTask(@Valid @RequestBody CreateTaskRequest request) {
        TaskView created = planningApplicationService.createTask(
                TenantContext.require(),
                new CreateTaskCommand(
                        request.title(),
                        request.description(),
                        request.status(),
                        request.priority(),
                        request.sprintId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/tasks")
    public ApiResponse<?> listTasks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String after,
            @RequestHeader(value = "Accept-Version", required = false) String version) {
        int boundedSize = Math.min(Math.max(size, 1), 200);
        int boundedPage = Math.max(page, 0);
        List<TaskView> tasks = planningApplicationService.listTasks(TenantContext.require(), boundedPage, boundedSize,
                after);
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
    public ResponseEntity<TaskView> updateTask(@PathVariable UUID taskId,
            @Valid @RequestBody UpdateTaskRequest request) {
        TaskView updated = planningApplicationService.updateTask(
                TenantContext.require(),
                taskId,
                new com.yourorg.platform.foculist.planning.application.UpdateTaskCommand(
                        request.sprintId(),
                        request.title(),
                        request.description(),
                        request.priority()));
        return ResponseEntity.ok(updated);
    }

    @PreAuthorize("hasPermission(#taskId, 'Task', 'WRITE')")
    @PatchMapping("/tasks/{taskId}/status")
    public ResponseEntity<TaskView> updateTaskStatus(@PathVariable UUID taskId,
            @Valid @RequestBody UpdateTaskStatusRequest request) {
        return ResponseEntity
                .ok(planningApplicationService.updateTaskStatus(TenantContext.require(), taskId, request.status()));
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
                        request.endDate()));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PreAuthorize("hasPermission(#sprintId, 'Sprint', 'WRITE')")
    @PutMapping("/sprints/{sprintId}")
    public ResponseEntity<SprintView> updateSprint(@PathVariable UUID sprintId,
            @Valid @RequestBody CreateSprintRequest request) {
        SprintView updated = planningApplicationService.updateSprint(
                TenantContext.require(),
                sprintId,
                new com.yourorg.platform.foculist.planning.application.CreateSprintCommand(
                        request.name(),
                        request.startDate(),
                        request.endDate()));
        return ResponseEntity.ok(updated);
    }

    @PreAuthorize("hasPermission(#sprintId, 'Sprint', 'ADMIN')")
    @DeleteMapping("/sprints/{sprintId}")
    public ResponseEntity<Void> deleteSprint(@PathVariable UUID sprintId) {
        planningApplicationService.deleteSprint(TenantContext.require(), sprintId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/boards")
    public List<BoardView> listBoards(
            @RequestParam(required = false) UUID projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return planningApplicationService.listBoards(TenantContext.require(), projectId, page, size);
    }

    @PostMapping("/boards")
    public ResponseEntity<BoardView> createBoard(@Valid @RequestBody CreateBoardRequest request) {
        BoardView created = planningApplicationService.createBoard(
                TenantContext.require(),
                new CreateBoardCommand(request.projectId(), request.name(), request.type()));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/boards/{boardId}/columns")
    public List<BoardColumnView> listBoardColumns(@PathVariable UUID boardId) {
        return planningApplicationService.listBoardColumns(TenantContext.require(), boardId);
    }

    @PostMapping("/boards/{boardId}/columns")
    public ResponseEntity<BoardColumnView> createBoardColumn(@PathVariable UUID boardId,
            @Valid @RequestBody CreateBoardColumnRequest request) {
        BoardColumnView created = planningApplicationService.createBoardColumn(
                TenantContext.require(),
                boardId,
                new CreateBoardColumnCommand(request.name(), request.statusMapping(), request.orderIndex()));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/epics")
    public List<EpicView> listEpics(
            @RequestParam(required = false) UUID projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return planningApplicationService.listEpics(TenantContext.require(), projectId, page, size);
    }

    @PostMapping("/epics")
    public ResponseEntity<EpicView> createEpic(@Valid @RequestBody CreateEpicRequest request) {
        EpicView created = planningApplicationService.createEpic(
                TenantContext.require(),
                new CreateEpicCommand(request.projectId(), request.name(), request.summary(), request.color(),
                        request.startDate(), request.targetDate()));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PreAuthorize("hasPermission(#taskId, 'Task', 'WRITE')")
    @PutMapping("/tasks/{taskId}/planning")
    public ResponseEntity<TaskView> updateTaskPlanning(@PathVariable UUID taskId,
            @Valid @RequestBody UpdateTaskPlanningRequest request) {
        TaskView updated = planningApplicationService.updateTaskPlanning(
                TenantContext.require(),
                taskId,
                new UpdateTaskPlanningCommand(request.sprintId(), request.epicId(), request.boardColumnId()));
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/boards/{boardId}")
    public ResponseEntity<BoardView> updateBoard(@PathVariable UUID boardId,
            @Valid @RequestBody UpdateBoardRequest request) {
        return ResponseEntity
                .ok(planningApplicationService.updateBoard(TenantContext.require(), boardId, request.name()));
    }

    @DeleteMapping("/boards/{boardId}")
    public ResponseEntity<Void> deleteBoard(@PathVariable UUID boardId) {
        planningApplicationService.deleteBoard(TenantContext.require(), boardId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/boards/{boardId}/columns/{columnId}")
    public ResponseEntity<BoardColumnView> updateBoardColumn(@PathVariable UUID boardId, @PathVariable UUID columnId,
            @Valid @RequestBody UpdateBoardColumnRequest request) {
        return ResponseEntity.ok(planningApplicationService.updateBoardColumn(TenantContext.require(), columnId,
                request.name(), request.orderIndex()));
    }

    @DeleteMapping("/boards/{boardId}/columns/{columnId}")
    public ResponseEntity<Void> deleteBoardColumn(@PathVariable UUID boardId, @PathVariable UUID columnId) {
        planningApplicationService.deleteBoardColumn(TenantContext.require(), columnId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/epics/{epicId}")
    public ResponseEntity<EpicView> updateEpic(@PathVariable UUID epicId,
            @Valid @RequestBody UpdateEpicRequest request) {
        return ResponseEntity.ok(planningApplicationService.updateEpic(TenantContext.require(), epicId, request.name(),
                request.summary(), request.color(), request.status()));
    }

    @DeleteMapping("/epics/{epicId}")
    public ResponseEntity<Void> deleteEpic(@PathVariable UUID epicId) {
        planningApplicationService.deleteEpic(TenantContext.require(), epicId);
        return ResponseEntity.noContent().build();
    }

    public record CreateTaskRequest(
            @NotBlank String title,
            String description,
            String status,
            String priority,
            String sprintId) {
    }

    public record UpdateTaskRequest(
            @NotBlank String title,
            String description,
            String priority,
            String sprintId) {
    }

    public record UpdateTaskStatusRequest(@NotBlank String status) {
    }

    public record CreateSprintRequest(
            @NotBlank String name,
            @NotBlank String startDate,
            @NotBlank String endDate) {
    }

    public record CreateBoardRequest(
            UUID projectId,
            @NotBlank String name,
            @NotBlank String type) {
    }

    public record CreateBoardColumnRequest(
            @NotBlank String name,
            String statusMapping,
            Integer orderIndex) {
    }

    public record CreateEpicRequest(
            UUID projectId,
            @NotBlank String name,
            String summary,
            String color,
            String startDate,
            String targetDate) {
    }

    public record UpdateTaskPlanningRequest(
            String sprintId,
            String epicId,
            String boardColumnId) {
    }

    public record UpdateBoardRequest(@NotBlank String name) {
    }

    public record UpdateBoardColumnRequest(String name, Integer orderIndex) {
    }

    public record UpdateEpicRequest(
            String name,
            String summary,
            String color,
            String status) {
    }
}
