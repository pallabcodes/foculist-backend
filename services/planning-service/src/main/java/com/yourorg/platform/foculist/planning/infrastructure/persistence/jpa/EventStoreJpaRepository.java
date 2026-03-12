package com.yourorg.platform.foculist.planning.infrastructure.persistence.jpa;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventStoreJpaRepository extends JpaRepository<EventStoreJpaEntity, UUID> {
    List<EventStoreJpaEntity> findByAggregateIdAndAggregateTypeAndTenantIdOrderByVersionAsc(UUID aggregateId, String aggregateType, String tenantId);
    
    List<EventStoreJpaEntity> findByAggregateIdAndAggregateTypeAndTenantIdAndVersionGreaterThanOrderByVersionAsc(
            UUID aggregateId, String aggregateType, String tenantId, long version);
}
