package com.yourorg.platform.foculist.identity.clean.domain.repository;

import com.yourorg.platform.foculist.identity.clean.domain.model.TokenBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TokenBlacklistRepository extends JpaRepository<TokenBlacklist, String> {
}
