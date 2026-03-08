package com.yourorg.platform.foculist.planning.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourorg.platform.foculist.planning.domain.model.OutboxEvent;
import com.yourorg.platform.foculist.planning.domain.model.PlanningDomainException;
import com.yourorg.platform.foculist.planning.domain.model.Sprint;
import com.yourorg.platform.foculist.planning.domain.model.Task;
import com.yourorg.platform.foculist.planning.domain.model.TaskPriority;
import com.yourorg.platform.foculist.planning.domain.model.TaskStatus;
import com.yourorg.platform.foculist.planning.domain.port.OutboxEventRepositoryPort;
import com.yourorg.platform.foculist.planning.domain.port.SprintRepositoryPort;
import com.yourorg.platform.foculist.planning.domain.port.TaskRepositoryPort;
import com.yourorg.platform.foculist.planning.domain.model.EventStore;
import com.yourorg.platform.foculist.planning.domain.port.EventStoreRepositoryPort;
import com.yourorg.platform.foculist.planning.domain.event.TaskEvent;
import com.yourorg.platform.foculist.planning.domain.event.TaskCreatedEvent;
import com.yourorg.platform.foculist.planning.domain.event.TaskUpdatedEvent;
import com.yourorg.platform.foculist.planning.domain.event.TaskStatusChangedEvent;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlanningApplicationService {
    public static final String TASK_CREATED_EVENT_TYPE = "planning.task.created.v1";
    private final SprintRepositoryPort sprintRepository;
    private final TaskRepositoryPort taskRepository;
    private final OutboxEventRepositoryPort outboxEventRepository;
    private final EventStoreRepositoryPort eventStoreRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public PlanningApplicationService(
            SprintRepositoryPort sprintRepository,
            TaskRepositoryPort taskRepository,
            OutboxEventRepositoryPort outboxEventRepository,
            EventStoreRepositoryPort eventStoreRepository,
            ObjectMapper objectMapper
    ) {
        this(sprintRepository, taskRepository, outboxEventRepository, eventStoreRepository, objectMapper, Clock.systemUTC());
    }

    PlanningApplicationService(
            SprintRepositoryPort sprintRepository,
            TaskRepositoryPort taskRepository,
            OutboxEventRepositoryPort outboxEventRepository,
            EventStoreRepositoryPort eventStoreRepository,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.sprintRepository = sprintRepository;
        this.taskRepository = taskRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.eventStoreRepository = eventStoreRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<SprintView> listSprints(String tenantId, int page, int size) {
        return sprintRepository.findByTenantId(tenantId, page, size).stream()
                .map(this::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> workflowStatuses() {
        return Arrays.stream(TaskStatus.values()).map(Enum::name).toList();
    }

    @Transactional
    public TaskView createTask(String tenantId, CreateTaskCommand command) {
        UUID sprintId = parseSprintId(command.sprintId());
        if (sprintId != null && sprintRepository.findByIdAndTenantId(sprintId, tenantId).isEmpty()) {
            throw new PlanningDomainException("Sprint does not exist for tenant: " + sprintId);
        }

        Task task = Task.create(
                tenantId,
                sprintId,
                command.title(),
                command.description(),
                TaskStatus.from(command.status()),
                TaskPriority.from(command.priority()),
                Instant.now(clock)
        );

        Task saved = taskRepository.save(task);

        TaskCreatedEvent domainEvent = new TaskCreatedEvent(
                saved.id(), saved.tenantId(), saved.sprintId(), saved.title(),
                saved.description(), saved.status().name(), saved.version(), Instant.now(clock)
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
        Task task = taskRepository.findByIdAndTenantId(taskId, tenantId)
                .orElseThrow(() -> new PlanningDomainException("Task not found"));
        return eventStoreRepository.findByAggregateId(taskId, "Task");
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
                Instant.now(clock)
        );
        Task saved = taskRepository.save(updated);

        TaskUpdatedEvent domainEvent = new TaskUpdatedEvent(
                saved.id(), saved.tenantId(), saved.title(), saved.description(),
                saved.version(), Instant.now(clock)
        );
        saveTaskEvent(domainEvent);

        return toView(saved);
    }

    @Transactional
    public TaskView updateTaskStatus(String tenantId, UUID taskId, String status) {
        Task task = taskRepository.findByIdAndTenantId(taskId, tenantId)
                .orElseThrow(() -> new PlanningDomainException("Task not found"));
        TaskStatus oldStatus = task.status();
        Task updated = task.updateStatus(TaskStatus.from(status), Instant.now(clock));
        Task saved = taskRepository.save(updated);

        TaskStatusChangedEvent domainEvent = new TaskStatusChangedEvent(
                saved.id(), saved.tenantId(), oldStatus.name(), saved.status().name(),
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
    }

    @Transactional
    public SprintView createSprint(String tenantId, CreateSprintCommand command) {
        Sprint sprint = new Sprint(
                UUID.randomUUID(),
                tenantId,
                command.name(),
                com.yourorg.platform.foculist.planning.domain.model.SprintStatus.PLANNED,
                Instant.parse(command.startDate() + "T00:00:00Z"),
                Instant.parse(command.endDate() + "T23:59:59Z"),
                Instant.now(clock),
                Instant.now(clock),
                0L
        );
        return toView(sprintRepository.save(sprint));
    }

    @Transactional
    public SprintView updateSprint(String tenantId, UUID sprintId, CreateSprintCommand command) {
        Sprint sprint = sprintRepository.findByIdAndTenantId(sprintId, tenantId)
                .orElseThrow(() -> new PlanningDomainException("Sprint not found"));
        Sprint updated = sprint.update(
                command.name(),
                Instant.parse(command.startDate() + "T00:00:00Z"),
                Instant.parse(command.endDate() + "T23:59:59Z"),
                Instant.now(clock)
        );
        return toView(sprintRepository.save(updated));
    }

    @Transactional
    public void deleteSprint(String tenantId, UUID sprintId) {
        Sprint sprint = sprintRepository.findByIdAndTenantId(sprintId, tenantId)
                .orElseThrow(() -> new PlanningDomainException("Sprint not found"));
        sprintRepository.delete(sprint);
    }

    private UUID parseSprintId(String rawSprintId) {
        if (rawSprintId == null || rawSprintId.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(rawSprintId.trim());
        } catch (IllegalArgumentException ex) {
            throw new PlanningDomainException("Invalid sprintId format");
        }
    }

    private void saveTaskEvent(TaskEvent domainEvent) {
        try {
            String payload = objectMapper.writeValueAsString(domainEvent);
            String eventType = domainEvent.getClass().getSimpleName();
            EventStore store = new EventStore(
                    UUID.randomUUID(),
                    domainEvent.tenantId(),
                    domainEvent.taskId(),
                    "Task",
                    eventType,
                    payload,
                    domainEvent.version(),
                    domainEvent.occurredAt()
            );
            eventStoreRepository.save(store);
        } catch (JsonProcessingException e) {
            throw new PlanningDomainException("Failed to serialize task event payload");
        }
    }

    private String createTaskCreatedPayload(Task task) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("eventType", TASK_CREATED_EVENT_TYPE);
            payload.put("taskId", task.id().toString());
            payload.put("tenantId", task.tenantId());
            payload.put("sprintId", task.sprintId() == null ? null : task.sprintId().toString());
            payload.put("title", task.title());
            payload.put("status", task.status().name());
            payload.put("priority", task.priority().name());
            payload.put("occurredAt", task.createdAt().toString());
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new PlanningDomainException("Failed to serialize task created event payload");
        }
    }

    private SprintView toView(Sprint sprint) {
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

    private Cursor parseCursor(String raw) {
        if (raw == null || raw.isBlank()) {
            return Cursor.empty();
        }
        String[] parts = raw.split("\\|");
        if (parts.length != 2) {
            throw new PlanningDomainException("Invalid cursor format");
        }
        try {
            Instant createdAt = Instant.parse(parts[0]);
            UUID id = UUID.fromString(parts[1]);
            return new Cursor(createdAt, id);
        } catch (Exception ex) {
            throw new PlanningDomainException("Invalid cursor format");
        }
    }

    private record Cursor(Instant createdAt, UUID id) {
        static Cursor empty() {
            return new Cursor(null, null);
        }
    }
}
