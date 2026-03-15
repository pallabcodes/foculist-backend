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
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Transactional
    public TaskView createTask(String tenantId, CreateTaskCommand command) {
        Task task = Task.create(
                tenantId,
                parseSprintId(command.sprintId()),
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

    private UUID parseSprintId(String sprintId) {
        return (sprintId != null && !sprintId.isBlank()) ? UUID.fromString(sprintId) : null;
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
}
