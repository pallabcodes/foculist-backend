package com.yourorg.platform.foculist.identity.clean.adapter.out.persistence;

import com.yourorg.platform.foculist.identity.clean.domain.model.Invite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InviteRepository extends JpaRepository<Invite, UUID> {
    Optional<Invite> findByToken(String token);
    Optional<Invite> findByEmailAndOrgId(String email, UUID orgId);
}
