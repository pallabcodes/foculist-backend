package com.yourorg.platform.foculist.sync.clean.infrastructure.websocket;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Tenant-scoped Reactive WebSocket session registry.
 */
@Component
public class SyncSessionRegistry {

    private static final Logger log = LoggerFactory.getLogger(SyncSessionRegistry.class);

    /** tenantId → live sessions */
    private final Map<String, Set<WebSocketSession>> tenantSessions = new ConcurrentHashMap<>();

    public void register(String tenantId, WebSocketSession session) {
        tenantSessions
                .computeIfAbsent(tenantId, k -> new CopyOnWriteArraySet<>())
                .add(session);
        log.info("Reactive Session {} registered for tenant {} (total: {})",
                session.getId(), tenantId, tenantSessions.get(tenantId).size());
    }

    public void unregister(String tenantId, WebSocketSession session) {
        Set<WebSocketSession> sessions = tenantSessions.get(tenantId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                tenantSessions.remove(tenantId);
            }
            log.info("Reactive Session {} unregistered from tenant {} (remaining: {})",
                    session.getId(), tenantId, sessions != null ? sessions.size() : 0);
        }
    }

    /**
     * Broadcast a text message to all other sessions in the tenant.
     * Returns a Mono that completes when all broadcast signals are sent.
     */
    @SuppressWarnings("null")
    public Mono<Void> broadcastToTenant(String tenantId, String payload, String excludeSessionId) {
        Set<WebSocketSession> sessions = tenantSessions.get(tenantId);
        if (sessions == null || sessions.isEmpty()) {
            return Mono.empty();
        }

        return Flux.fromIterable(sessions)
                .filter(s -> !s.getId().equals(excludeSessionId))
                .flatMap(s -> s.send(Mono.just(s.textMessage(payload)))
                        .doOnError(e -> log.warn("Failed to send message to session {}: {}", s.getId(), e.getMessage())))
                .then();
    }

    public int sessionCount(String tenantId) {
        Set<WebSocketSession> sessions = tenantSessions.get(tenantId);
        return sessions == null ? 0 : sessions.size();
    }
}
