package com.yourorg.platform.foculist.identity.clean.adapter.out.persistence;

import com.yourorg.platform.foculist.identity.clean.domain.model.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {
    List<UserSession> findByUserId(UUID userId);
    Optional<UserSession> findByJti(String jti);
    void deleteByJti(String jti);
}
