package com.yourorg.platform.foculist.project.infrastructure.document;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProjectTemplateMongoRepository extends MongoRepository<ProjectTemplateDocument, String> {
    List<ProjectTemplateDocument> findByTenantIdOrderByUpdatedAtDesc(String tenantId);
    Optional<ProjectTemplateDocument> findByTenantIdAndTemplateKey(String tenantId, String templateKey);
}
