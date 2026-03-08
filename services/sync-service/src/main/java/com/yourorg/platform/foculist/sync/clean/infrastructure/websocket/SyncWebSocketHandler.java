package com.yourorg.platform.foculist.sync.clean.infrastructure.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourorg.platform.foculist.sync.clean.application.command.SyncPullCommand;
import com.yourorg.platform.foculist.sync.clean.application.command.SyncPushCommand;
import com.yourorg.platform.foculist.sync.clean.application.service.SyncApplicationService;
import com.yourorg.platform.foculist.sync.clean.application.view.SyncPullResponseView;
import com.yourorg.platform.foculist.sync.clean.application.view.SyncPushResponseView;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * WebSocket handler for real-time sync operations.
 * <p>
 * Handles three message types:
 * <ul>
 *   <li><b>push</b> — Persist a change event and broadcast to all other tenant sessions</li>
 *   <li><b>pull</b> — Fetch changes since a cursor and respond to the caller</li>
 *   <li><b>ping</b> — Respond with a pong for keep-alive</li>
 * </ul>
 * <p>
 * Message format (JSON):
 * <pre>
 * { "type": "push", "deviceId": "...", "pendingChanges": 1, "payload": {...} }
 * { "type": "pull", "deviceId": "...", "lastSync": "2024-01-01T00:00:00Z" }
 * { "type": "ping" }
 * </pre>
 */
@Component
public class SyncWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(SyncWebSocketHandler.class);

    private final SyncApplicationService syncService;
    private final SyncSessionRegistry sessionRegistry;
    private final ObjectMapper objectMapper;

    public SyncWebSocketHandler(SyncApplicationService syncService,
                                 SyncSessionRegistry sessionRegistry,
                                 ObjectMapper objectMapper) {
        this.syncService = syncService;
        this.sessionRegistry = sessionRegistry;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String tenantId = getTenantId(session);
        String userId = getUserId(session);
        sessionRegistry.register(tenantId, session);
        log.info("Sync WebSocket connected: session={}, tenant={}, user={}, peers={}",
                session.getId(), tenantId, userId, sessionRegistry.sessionCount(tenantId));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String tenantId = getTenantId(session);
        sessionRegistry.unregister(tenantId, session);
        log.info("Sync WebSocket disconnected: session={}, tenant={}, status={}",
                session.getId(), tenantId, status);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            JsonNode json = objectMapper.readTree(message.getPayload());
            String type = json.has("type") ? json.get("type").asText() : "unknown";

            switch (type) {
                case "push" -> handlePush(session, json);
                case "pull" -> handlePull(session, json);
                case "ping" -> handlePing(session);
                default -> sendError(session, "Unknown message type: " + type);
            }
        } catch (Exception e) {
            log.error("Error handling message from session {}: {}", session.getId(), e.getMessage(), e);
            sendError(session, "Invalid message: " + e.getMessage());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        String tenantId = getTenantId(session);
        log.error("Sync WebSocket transport error: session={}, tenant={}", session.getId(), tenantId, exception);
        sessionRegistry.unregister(tenantId, session);
    }

    // ─── Message Handlers ───────────────────────────────────────────────

    private void handlePush(WebSocketSession session, JsonNode json) throws Exception {
        String tenantId = getTenantId(session);
        String deviceId = json.has("deviceId") ? json.get("deviceId").asText() : session.getId();
        int pendingChanges = json.has("pendingChanges") ? json.get("pendingChanges").asInt() : 1;
        String payloadVersion = json.has("payloadVersion") ? json.get("payloadVersion").asText() : "v1";
        String clientSyncTime = json.has("clientSyncTime") ? json.get("clientSyncTime").asText() : null;

        @SuppressWarnings("unchecked")
        Map<String, Object> payload = json.has("payload")
                ? objectMapper.convertValue(json.get("payload"), Map.class)
                : Map.of();

        SyncPushCommand command = new SyncPushCommand(
                deviceId, pendingChanges, payloadVersion, payload, clientSyncTime
        );

        SyncPushResponseView response = syncService.push(tenantId, command);

        // Send acknowledgment to sender
        String ack = objectMapper.writeValueAsString(Map.of(
                "type", "push_ack",
                "envelopeId", response.envelopeId().toString(),
                "pendingChanges", response.pendingChanges(),
                "receivedAt", response.receivedAt().toString()
        ));
        session.sendMessage(new TextMessage(ack));

        // Broadcast delta to other tenant sessions
        String broadcast = objectMapper.writeValueAsString(Map.of(
                "type", "remote_change",
                "deviceId", deviceId,
                "payload", payload,
                "timestamp", Instant.now().toString()
        ));
        sessionRegistry.broadcastToTenant(tenantId, broadcast, session.getId());
    }

    private void handlePull(WebSocketSession session, JsonNode json) throws Exception {
        String tenantId = getTenantId(session);
        String deviceId = json.has("deviceId") ? json.get("deviceId").asText() : session.getId();
        String lastSync = json.has("lastSync") ? json.get("lastSync").asText() : null;

        SyncPullCommand command = new SyncPullCommand(deviceId, lastSync);
        SyncPullResponseView response = syncService.pull(tenantId, command);

        String responseJson = objectMapper.writeValueAsString(Map.of(
                "type", "pull_response",
                "changes", response.changes(),
                "changeCount", response.changeCount(),
                "serverTime", response.serverTime().toString(),
                "newCursor", response.nextSyncCursor().toString()
        ));
        session.sendMessage(new TextMessage(responseJson));
    }

    private void handlePing(WebSocketSession session) throws Exception {
        String pong = objectMapper.writeValueAsString(Map.of(
                "type", "pong",
                "timestamp", Instant.now().toString()
        ));
        session.sendMessage(new TextMessage(pong));
    }

    // ─── Helpers ────────────────────────────────────────────────────────

    private void sendError(WebSocketSession session, String errorMessage) {
        try {
            String error = objectMapper.writeValueAsString(Map.of(
                    "type", "error",
                    "message", errorMessage,
                    "timestamp", Instant.now().toString()
            ));
            session.sendMessage(new TextMessage(error));
        } catch (Exception e) {
            log.error("Failed to send error to session {}: {}", session.getId(), e.getMessage());
        }
    }

    private String getTenantId(WebSocketSession session) {
        Object tenantId = session.getAttributes().get(JwtHandshakeInterceptor.ATTR_TENANT_ID);
        return tenantId != null ? tenantId.toString() : "public";
    }

    private String getUserId(WebSocketSession session) {
        Object userId = session.getAttributes().get(JwtHandshakeInterceptor.ATTR_USER_ID);
        return userId != null ? userId.toString() : "anonymous";
    }
}
