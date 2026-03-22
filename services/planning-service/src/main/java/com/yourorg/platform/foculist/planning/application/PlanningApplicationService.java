package com.yourorg.platform.foculist.planning.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourorg.platform.foculist.planning.domain.event.TaskCreatedEvent;
import com.yourorg.platform.foculist.planning.domain.event.TaskDeletedEvent;
import com.yourorg.platform.foculist.planning.domain.event.TaskEvent;
import com.yourorg.platform.foculist.planning.domain.event.TaskStatusChangedEvent;
import com.yourorg.platform.foculist.planning.domain.event.TaskUpdatedEvent;
import com.yourorg.platform.foculist.planning.domain.model.Cursor;
import com.yourorg.platform.foculist.planning.domain.model.EventStore;
import com.yourorg.platform.foculist.planning.domain.model.OutboxEvent;
import com.yourorg.platform.foculist.planning.domain.model.OutboxEventStatus;
import com.yourorg.platform.foculist.planning.domain.model.PlanningDomainException;
import com.yourorg.platform.foculist.planning.domain.model.Sprint;
import com.yourorg.platform.foculist.planning.domain.model.SprintStatus;
import com.yourorg.platform.foculist.planning.domain.model.Task;
import com.yourorg.platform.foculist.planning.domain.model.TaskPriority;
import com.yourorg.platform.foculist.planning.domain.model.TaskSnapshotJob;
import com.yourorg.platform.foculist.planning.domain.model.TaskStatus;
import com.yourorg.platform.foculist.planning.domain.port.EventStoreRepositoryPort;
import com.yourorg.platform.foculist.planning.domain.port.OutboxEventRepositoryPort;
import com.yourorg.platform.foculist.planning.domain.port.SprintRepositoryPort;
import com.yourorg.platform.foculist.planning.domain.port.TaskRepositoryPort;
import com.yourorg.platform.foculist.planning.domain.port.TaskSnapshotJobRepositoryPort;
import com.yourorg.platform.foculist.planning.domain.port.BoardRepositoryPort;
import com.yourorg.platform.foculist.planning.domain.port.BoardColumnRepositoryPort;
import com.yourorg.platform.foculist.planning.domain.port.EpicRepositoryPort;
import com.yourorg.platform.foculist.planning.domain.model.Board;
import com.yourorg.platform.foculist.planning.domain.model.BoardColumn;
import com.yourorg.platform.foculist.planning.domain.model.BoardType;
import com.yourorg.platform.foculist.planning.domain.model.Epic;
import com.yourorg.platform.foculist.planning.domain.model.EpicStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlanningApplicationService {
    public static final String TASK_AGGREGATE_TYPE = "Task";
    public static final String TASK_CREATED_EVENT_TYPE = "TaskCreated";
    public static final String TASK_UPDATED_EVENT_TYPE = "TaskUpdated";
    public static final String TASK_STATUS_CHANGED_EVENT_TYPE = "TaskStatusChanged";
    public static final String TASK_DELETED_EVENT_TYPE = "TaskDeleted";

    private final TaskRepositoryPort taskRepository;
    private final SprintRepositoryPort sprintRepository;
    private final EventStoreRepositoryPort eventStoreRepository;
    private final OutboxEventRepositoryPort outboxEventRepository;
    private final TaskSnapshotJobRepositoryPort snapshotJobRepository;
    private final BoardRepositoryPort boardRepository;
    private final BoardColumnRepositoryPort boardColumnRepository;
    private final EpicRepositoryPort epicRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Transactional
    public TaskView createTask(String tenantId, CreateTaskCommand command) {
        Task task = Task.create(
                tenantId,
                parseSprintId(command.sprintId()),
                null, // epicId
                null, // boardColumnId
                command.title(),
                command.description(),
                TaskStatus.from(command.status()),
                TaskPriority.from(command.priority()),
                Instant.now(clock),
                "system" // Audit field: createdBy
        );

        Task saved = taskRepository.save(task);

        TaskCreatedEvent domainEvent = new TaskCreatedEvent(
                saved.id(), saved.tenantId(), saved.sprintId(), saved.title(),
                saved.description(), saved.status().name(), saved.priority().name(),
                saved.version(), Instant.now(clock)
        );
        saveTaskEvent(domainEvent);

        outboxEventRepository.save(OutboxEvent.newEvent(
                saved.tenantId(),
                "Task",
                saved.id(),
                TASK_CREATED_EVENT_TYPE,
                createTaskCreatedPayload(saved),
                Instant.now(clock)
        ));
        return toView(saved);
    }

    @Transactional(readOnly = true)
    public List<TaskView> listTasks(String tenantId, int page, int size, String after) {
        Cursor cursor = parseCursor(after);
        return taskRepository.findByTenantId(tenantId, page, size, cursor.createdAt(), cursor.id()).stream()
                .map(this::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EventStore> listTaskEvents(String tenantId, UUID taskId) {
        return eventStoreRepository.findByAggregateId(tenantId, taskId, TASK_AGGREGATE_TYPE);
    }

    @Transactional
    public TaskView updateTask(String tenantId, UUID taskId, UpdateTaskCommand command) {
        Task task = taskRepository.findByIdAndTenantId(taskId, tenantId)
                .orElseThrow(() -> new PlanningDomainException("Task not found"));
        Task updated = task.update(
                parseSprintId(command.sprintId()),
                task.epicId(),
                task.boardColumnId(),
                command.title(),
                command.description(),
                TaskPriority.from(command.priority()),
                Instant.now(clock),
                "system" // Audit field: updatedBy
        );
        Task saved = taskRepository.save(updated);

        TaskUpdatedEvent domainEvent = new TaskUpdatedEvent(
                saved.id(), saved.tenantId(), saved.sprintId(), saved.title(),
                saved.description(), saved.priority().name(),
                saved.version(), Instant.now(clock)
        );
        saveTaskEvent(domainEvent);

        return toView(saved);
    }

    @Transactional
    public TaskView updateTaskStatus(String tenantId, UUID taskId, String status) {
        Task task = taskRepository.findByIdAndTenantId(taskId, tenantId)
                .orElseThrow(() -> new PlanningDomainException("Task not found"));
        String oldStatus = task.status().name();
        Task updated = task.updateStatus(TaskStatus.from(status), Instant.now(clock), "system");
        Task saved = taskRepository.save(updated);

        TaskStatusChangedEvent domainEvent = new TaskStatusChangedEvent(
                saved.id(), saved.tenantId(), oldStatus, saved.status().name(),
                saved.version(), Instant.now(clock)
        );
        saveTaskEvent(domainEvent);

        return toView(saved);
    }

    @Transactional
    public TaskView updateTaskPlanning(String tenantId, UUID taskId, UpdateTaskPlanningCommand command) {
        Task task = taskRepository.findByIdAndTenantId(taskId, tenantId)
                .orElseThrow(() -> new PlanningDomainException("Task not found"));
        
        UUID sprintId = parseId(command.sprintId());
        UUID epicId = parseId(command.epicId());
        UUID boardColumnId = parseId(command.boardColumnId());
        
        Task updated = task.update(
                sprintId,
                epicId,
                boardColumnId,
                task.title(),
                task.description(),
                task.priority(),
                Instant.now(clock),
                "system"
        );
        Task saved = taskRepository.save(updated);
        
        TaskUpdatedEvent domainEvent = new TaskUpdatedEvent(
                saved.id(), saved.tenantId(), saved.sprintId(), saved.title(),
                saved.description(), saved.priority().name(),
                saved.version(), Instant.now(clock)
        );
        saveTaskEvent(domainEvent);

        return toView(saved);
    }

    @Transactional
    public void deleteTask(String tenantId, UUID taskId) {
        Task task = taskRepository.findByIdAndTenantId(taskId, tenantId)
                .orElseThrow(() -> new PlanningDomainException("Task not found"));
        taskRepository.delete(task);

        TaskDeletedEvent domainEvent = new TaskDeletedEvent(
                task.id(), task.tenantId(), task.version() + 1, Instant.now(clock)
        );
        saveTaskEvent(domainEvent);
    }

    @Transactional(readOnly = true)
    public List<SprintView> listSprints(String tenantId, int page, int size) {
        return sprintRepository.findByTenantId(tenantId, page, size).stream()
                .map(this::toSprintView)
                .toList();
    }

    @Transactional
    public SprintView createSprint(String tenantId, CreateSprintCommand command) {
        Sprint sprint = new Sprint(
                UUID.randomUUID(),
                tenantId,
                command.name(),
                SprintStatus.PLANNED,
                Instant.parse(command.startDate()),
                Instant.parse(command.endDate()),
                Instant.now(clock),
                Instant.now(clock),
                "system", // Audit: createdBy
                null,     // Audit: updatedBy
                null,     // Soft delete: deletedAt
                null,     // Metadata
                0L
        );
        Sprint saved = sprintRepository.save(sprint);
        return toSprintView(saved);
    }

    @Transactional
    public SprintView updateSprint(String tenantId, UUID sprintId, CreateSprintCommand command) {
        Sprint sprint = sprintRepository.findByIdAndTenantId(sprintId, tenantId)
                .orElseThrow(() -> new PlanningDomainException("Sprint not found"));
        Sprint updated = sprint.update(
                command.name(),
                Instant.parse(command.startDate()),
                Instant.parse(command.endDate()),
                Instant.now(clock),
                "system" // Audit: updatedBy
        );
        Sprint saved = sprintRepository.save(updated);
        return toSprintView(saved);
    }

    @Transactional
    public void deleteSprint(String tenantId, UUID sprintId) {
        Sprint sprint = sprintRepository.findByIdAndTenantId(sprintId, tenantId)
                .orElseThrow(() -> new PlanningDomainException("Sprint not found"));
        sprintRepository.delete(sprint);
    }

    public List<String> workflowStatuses() {
        return Arrays.stream(TaskStatus.values()).map(Enum::name).toList();
    }

    @Transactional(readOnly = true)
    public List<BoardView> listBoards(String tenantId, UUID projectId, int page, int size) {
        if (projectId != null) {
            return boardRepository.findByProjectIdAndTenantId(projectId, tenantId, page, size).stream().map(this::toBoardView).toList();
        }
        return boardRepository.findByTenantId(tenantId, page, size).stream().map(this::toBoardView).toList();
    }

    @Transactional
    public BoardView createBoard(String tenantId, CreateBoardCommand command) {
        Board board = new Board(
                UUID.randomUUID(), tenantId, command.projectId(), command.name(),
                BoardType.valueOf(command.type()), Instant.now(clock), Instant.now(clock),
                "system", null, null, null, 0L
        );
        return toBoardView(boardRepository.save(board));
    }

    @Transactional(readOnly = true)
    public List<BoardColumnView> listBoardColumns(String tenantId, UUID boardId) {
        return boardColumnRepository.findByBoardIdAndTenantId(boardId, tenantId).stream().map(this::toBoardColumnView).toList();
    }

    @Transactional
    public BoardColumnView createBoardColumn(String tenantId, UUID boardId, CreateBoardColumnCommand command) {
        BoardColumn column = new BoardColumn(
                UUID.randomUUID(), tenantId, boardId, command.name(), command.statusMapping(),
                command.orderIndex() != null ? command.orderIndex() : 0,
                Instant.now(clock), Instant.now(clock), "system", null, null, 0L
        );
        return toBoardColumnView(boardColumnRepository.save(column));
    }

    @Transactional(readOnly = true)
    public List<EpicView> listEpics(String tenantId, UUID projectId, int page, int size) {
        if (projectId != null) {
            return epicRepository.findByProjectIdAndTenantId(projectId, tenantId, page, size).stream().map(this::toEpicView).toList();
        }
        return epicRepository.findByTenantId(tenantId, page, size).stream().map(this::toEpicView).toList();
    }

    @Transactional
    public EpicView createEpic(String tenantId, CreateEpicCommand command) {
        Epic epic = new Epic(
                UUID.randomUUID(), tenantId, command.projectId(), command.name(),
                command.summary(), command.color(), EpicStatus.TO_DO,
                command.startDate() != null ? Instant.parse(command.startDate()) : null,
                command.targetDate() != null ? Instant.parse(command.targetDate()) : null,
                Instant.now(clock), Instant.now(clock), "system", null, null, null, 0L
        );
        return toEpicView(epicRepository.save(epic));
    }

    @Transactional
    public BoardView updateBoard(String tenantId, UUID boardId, String name) {
        Board board = boardRepository.findByIdAndTenantId(boardId, tenantId)
                .orElseThrow(() -> new PlanningDomainException("Board not found"));
        Board updated = board.update(name, "system");
        return toBoardView(boardRepository.save(updated));
    }

    @Transactional
    public void deleteBoard(String tenantId, UUID boardId) {
        Board board = boardRepository.findByIdAndTenantId(boardId, tenantId)
                .orElseThrow(() -> new PlanningDomainException("Board not found"));
        boardRepository.delete(board);
    }

    @Transactional
    public EpicView updateEpic(String tenantId, UUID epicId, String name, String summary, String color, String status) {
        Epic epic = epicRepository.findByIdAndTenantId(epicId, tenantId)
                .orElseThrow(() -> new PlanningDomainException("Epic not found"));
        Epic updated = epic.update(
                name, summary, color, 
                status != null ? EpicStatus.valueOf(status) : null, 
                epic.startDate(), epic.targetDate(), "system"
        );
        return toEpicView(epicRepository.save(updated));
    }

    @Transactional
    public void deleteEpic(String tenantId, UUID epicId) {
        Epic epic = epicRepository.findByIdAndTenantId(epicId, tenantId)
                .orElseThrow(() -> new PlanningDomainException("Epic not found"));
        epicRepository.delete(epic);
    }

    @Transactional
    public BoardColumnView updateBoardColumn(String tenantId, UUID columnId, String name, Integer orderIndex) {
        BoardColumn column = boardColumnRepository.findByIdAndTenantId(columnId, tenantId)
                .orElseThrow(() -> new PlanningDomainException("Column not found"));
        BoardColumn updated = new BoardColumn(
                column.id(), column.tenantId(), column.boardId(), 
                name != null ? name : column.name(), column.statusMapping(), 
                orderIndex != null ? orderIndex : column.orderIndex(),
                column.createdAt(), Instant.now(clock), column.createdBy(), "system", null, column.version()
        );
        return toBoardColumnView(boardColumnRepository.save(updated));
    }

    @Transactional
    public void deleteBoardColumn(String tenantId, UUID columnId) {
        BoardColumn column = boardColumnRepository.findByIdAndTenantId(columnId, tenantId)
                .orElseThrow(() -> new PlanningDomainException("Column not found"));
        boardColumnRepository.delete(column);
    }

    private void saveTaskEvent(TaskEvent domainEvent) {
        try {
            EventStore store = new EventStore(
                    UUID.randomUUID(),
                    domainEvent.tenantId(),
                    domainEvent.taskId(),
                    TASK_AGGREGATE_TYPE,
                    domainEvent.getClass().getSimpleName(),
                    objectMapper.writeValueAsString(domainEvent),
                    domainEvent.version(),
                    domainEvent.occurredAt()
            );
            eventStoreRepository.save(store);

            // Trigger snapshotting every 10 versions
            if (domainEvent.version() > 0 && domainEvent.version() % 10 == 0) {
                snapshotJobRepository.saveIfAbsent(new TaskSnapshotJob(
                        UUID.randomUUID(),
                        domainEvent.tenantId(),
                        domainEvent.taskId(),
                        domainEvent.version(),
                        OutboxEventStatus.NEW,
                        0,
                        null,
                        Instant.now(clock),
                        null
                ));
            }
        } catch (JsonProcessingException e) {
            throw new PlanningDomainException("Failed to serialize task event payload");
        }
    }

    private String createTaskCreatedPayload(Task task) {
        try {
            return objectMapper.writeValueAsString(task);
        } catch (JsonProcessingException e) {
            throw new PlanningDomainException("Failed to serialize task created payload");
        }
    }

    private UUID parseId(String idStr) {
        return (idStr != null && !idStr.isBlank()) ? UUID.fromString(idStr) : null;
    }

    private UUID parseSprintId(String sprintId) {
        return parseId(sprintId);
    }

    private Cursor parseCursor(String after) {
        if (after == null || after.isBlank()) {
            return new Cursor(Instant.MAX, UUID.randomUUID());
        }
        try {
            String decoded = new String(Base64.getDecoder().decode(after));
            String[] parts = decoded.split("\\|");
            if (parts.length != 2) {
                return new Cursor(Instant.MAX, UUID.randomUUID());
            }
            return new Cursor(Instant.parse(parts[0]), UUID.fromString(parts[1]));
        } catch (Exception e) {
            return new Cursor(Instant.MAX, UUID.randomUUID());
        }
    }

    private TaskView toView(Task task) {
        return new TaskView(
                task.id(),
                task.sprintId(),
                task.epicId(),
                task.boardColumnId(),
                task.title(),
                task.description(),
                task.status().name(),
                task.priority().name(),
                task.createdAt(),
                task.tenantId(),
                task.version()
        );
    }

    private SprintView toSprintView(Sprint sprint) {
        return new SprintView(
                sprint.id(),
                sprint.name(),
                sprint.status().name(),
                sprint.startDate(),
                sprint.endDate(),
                sprint.tenantId(),
                sprint.version()
        );
    }

    private BoardView toBoardView(Board board) {
        return new BoardView(board.id(), board.projectId(), board.name(), board.type().name(), board.createdAt(), board.tenantId(), board.version());
    }

    private BoardColumnView toBoardColumnView(BoardColumn column) {
        return new BoardColumnView(column.id(), column.boardId(), column.name(), column.statusMapping(), column.orderIndex(), column.createdAt(), column.tenantId(), column.version());
    }

    private EpicView toEpicView(Epic epic) {
        return new EpicView(epic.id(), epic.projectId(), epic.name(), epic.summary(), epic.color(), epic.status().name(), epic.startDate(), epic.targetDate(), epic.createdAt(), epic.tenantId(), epic.version());
    }
}
