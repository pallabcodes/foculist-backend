package com.yourorg.platform.foculist.identity.clean.application.service;

import com.yourorg.platform.foculist.identity.clean.adapter.out.persistence.UserSessionRepository;
import com.yourorg.platform.foculist.identity.clean.domain.model.TokenBlacklist;
import com.yourorg.platform.foculist.identity.clean.domain.model.UserSession;
import com.yourorg.platform.foculist.identity.clean.domain.repository.TokenBlacklistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserSessionService {

    private final UserSessionRepository userSessionRepository;
    private final TokenBlacklistRepository tokenBlacklistRepository;

    public List<UserSession> getUserSessions(UUID userId) {
        return userSessionRepository.findByUserId(userId);
    }

    @Transactional
    public void revokeSession(String jti, UUID userId) {
        UserSession session = userSessionRepository.findByJti(jti)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

        // Verify ownership
        if (!session.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own this session");
        }

        // 1. Blacklist the token JTI to prevent future API hits at the Gateway
        if (!tokenBlacklistRepository.existsById(jti)) {
            Instant expiry = session.getExpiresAt().atZone(ZoneId.systemDefault()).toInstant();
            TokenBlacklist blacklist = new TokenBlacklist(jti, expiry, Instant.now());
            tokenBlacklistRepository.save(blacklist);
        }

        // 2. Remove session record from active list
        userSessionRepository.deleteByJti(jti);
    }
}
