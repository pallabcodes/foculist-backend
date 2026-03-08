package com.yourorg.platform.foculist.project.infrastructure.document;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProjectBrainstormMongoRepository extends MongoRepository<ProjectBrainstormDocument, String> {
    List<ProjectBrainstormDocument> findByTenantIdAndProjectIdOrderByUpdatedAtDesc(String tenantId, String projectId);
}
