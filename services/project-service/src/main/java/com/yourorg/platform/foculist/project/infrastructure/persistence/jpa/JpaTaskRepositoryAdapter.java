package com.yourorg.platform.foculist.project.infrastructure.persistence.jpa;

import com.yourorg.platform.foculist.project.domain.model.Task;
import com.yourorg.platform.foculist.project.domain.port.TaskRepositoryPort;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaTaskRepositoryAdapter implements TaskRepositoryPort {
    private final TaskJpaRepository repository;

    @Override
    public Task save(Task task) {
        TaskJpaEntity entity = toEntity(task);
        TaskJpaEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Task> findByIdAndTenantId(UUID id, String tenantId) {
        return repository.findByIdAndTenantId(id, tenantId).map(this::toDomain);
    }

    @Override
    public List<Task> findByProjectIdAndTenantId(UUID projectId, String tenantId, int page, int size) {
        return repository.findByProjectIdAndTenantId(projectId, tenantId, PageRequest.of(page, size))
                .stream().map(this::toDomain).toList();
    }

    @Override
    public void delete(UUID id, String tenantId) {
        repository.findByIdAndTenantId(id, tenantId).ifPresent(repository::delete);
    }

    private TaskJpaEntity toEntity(Task domain) {
        TaskJpaEntity entity = new TaskJpaEntity();
        entity.setId(domain.getId());
        entity.setTenantId(domain.getTenantId());
        entity.setProjectId(domain.getProjectId());
        entity.setTitle(domain.getTitle());
        entity.setDescription(domain.getDescription());
        entity.setStatus(domain.getStatus());
        entity.setPriority(domain.getPriority());
        entity.setType(domain.getType());
        entity.setStoryPoints(domain.getStoryPoints());
        entity.setAssigneeId(domain.getAssigneeId());
        entity.setReporterId(domain.getReporterId());
        entity.setDueDate(domain.getDueDate());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setVersion(domain.getVersion());
        return entity;
    }

    private Task toDomain(TaskJpaEntity entity) {
        return Task.builder()
                .id(entity.getId())
                .tenantId(entity.getTenantId())
                .projectId(entity.getProjectId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .status(entity.getStatus())
                .priority(entity.getPriority())
                .type(entity.getType())
                .storyPoints(entity.getStoryPoints())
                .assigneeId(entity.getAssigneeId())
                .reporterId(entity.getReporterId())
                .dueDate(entity.getDueDate())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .version(entity.getVersion())
                .build();
    }
}
