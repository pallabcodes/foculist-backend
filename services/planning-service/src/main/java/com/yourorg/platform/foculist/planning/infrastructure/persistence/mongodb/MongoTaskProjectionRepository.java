package com.yourorg.platform.foculist.planning.infrastructure.persistence.mongodb;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MongoTaskProjectionRepository extends MongoRepository<TaskProjectionDocument, String> {
    Optional<TaskProjectionDocument> findByTaskIdAndTenantId(UUID taskId, String tenantId);
    void deleteByTaskIdAndTenantId(UUID taskId, String tenantId);
}
