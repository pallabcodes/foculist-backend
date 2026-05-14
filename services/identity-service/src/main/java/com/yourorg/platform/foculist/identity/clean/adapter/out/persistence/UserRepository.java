package com.yourorg.platform.foculist.identity.clean.adapter.out.persistence;

import com.yourorg.platform.foculist.identity.clean.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Optional<User> findByIdAndTenantId(UUID id, String tenantId);
}
