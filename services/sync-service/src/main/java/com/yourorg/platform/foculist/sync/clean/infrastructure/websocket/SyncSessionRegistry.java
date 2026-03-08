package com.yourorg.platform.foculist.sync.clean.infrastructure.websocket;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * Tenant-scoped WebSocket session registry.
 * <p>
 * Maintains a thread-safe mapping of {@code tenantId → Set<WebSocketSession>}
 * for real-time broadcast fan-out. When a change arrives from one client,
 * it is broadcast to all other sessions belonging to the same tenant.
 */
@Component
public class SyncSessionRegistry {

    private static final Logger log = LoggerFactory.getLogger(SyncSessionRegistry.class);

    /** tenantId → live sessions */
    private final Map<String, Set<WebSocketSession>> tenantSessions = new ConcurrentHashMap<>();

    /**
     * Register a session under the given tenant.
     */
    public void register(String tenantId, WebSocketSession session) {
        tenantSessions
                .computeIfAbsent(tenantId, k -> new CopyOnWriteArraySet<>())
                .add(session);
        log.info("Session {} registered for tenant {} (total: {})",
                session.getId(), tenantId, tenantSessions.get(tenantId).size());
    }

    /**
     * Unregister a session. Removes the tenant key if no sessions remain.
     */
    public void unregister(String tenantId, WebSocketSession session) {
        Set<WebSocketSession> sessions = tenantSessions.get(tenantId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                tenantSessions.remove(tenantId);
            }
            log.info("Session {} unregistered from tenant {} (remaining: {})",
                    session.getId(), tenantId, sessions.size());
        }
    }

    /**
     * Broadcast a text message to all sessions in the tenant,
     * optionally excluding the sender's session ID.
     */
    public void broadcastToTenant(String tenantId, String payload, String excludeSessionId) {
        Set<WebSocketSession> sessions = tenantSessions.get(tenantId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        TextMessage message = new TextMessage(payload);
        for (WebSocketSession session : sessions) {
            if (session.isOpen() && !session.getId().equals(excludeSessionId)) {
                try {
                    session.sendMessage(message);
                } catch (IOException e) {
                    log.warn("Failed to send message to session {}: {}", session.getId(), e.getMessage());
                }
            }
        }
    }

    /** Returns the count of active sessions for a tenant. */
    public int sessionCount(String tenantId) {
        Set<WebSocketSession> sessions = tenantSessions.get(tenantId);
        return sessions == null ? 0 : sessions.size();
    }
}
