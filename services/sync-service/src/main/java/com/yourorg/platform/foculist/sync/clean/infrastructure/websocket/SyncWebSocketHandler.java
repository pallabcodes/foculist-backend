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
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

/**
 * Reactive WebSocket handler for real-time sync operations.
 */
@Component
public class SyncWebSocketHandler implements WebSocketHandler {

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
    public Mono<Void> handle(WebSocketSession session) {
        String tenantId = getTenantId(session);
        String userId = getUserId(session);
        
        sessionRegistry.register(tenantId, session);
        
        return session.receive()
                .flatMap(message -> handleMessage(session, message))
                .doFinally(signalType -> sessionRegistry.unregister(tenantId, session))
                .then();
    }

    private Mono<Void> handleMessage(WebSocketSession session, WebSocketMessage message) {
        try {
            JsonNode json = objectMapper.readTree(message.getPayloadAsText());
            String type = json.has("type") ? json.get("type").asText() : "unknown";

            return switch (type) {
                case "push" -> handlePush(session, json);
                case "pull" -> handlePull(session, json);
                case "ping" -> handlePing(session);
                default -> sendError(session, "Unknown message type: " + type);
            };
        } catch (Exception e) {
            log.error("Error handling message from session {}: {}", session.getId(), e.getMessage());
            return sendError(session, "Invalid JSON: " + e.getMessage());
        }
    }

    private Mono<Void> handlePush(WebSocketSession session, JsonNode json) throws Exception {
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

        // Wrapping service call in Mono.fromCallable as it's likely blocking JPA currently
        return Mono.fromCallable(() -> syncService.push(tenantId, command))
                .flatMap(response -> {
                    try {
                        String ack = objectMapper.writeValueAsString(Map.of(
                                "type", "push_ack",
                                "envelopeId", response.envelopeId().toString(),
                                "pendingChanges", response.pendingChanges(),
                                "receivedAt", response.receivedAt().toString()
                        ));
                        
                        String broadcast = objectMapper.writeValueAsString(Map.of(
                                "type", "remote_change",
                                "deviceId", deviceId,
                                "payload", payload,
                                "timestamp", Instant.now().toString()
                        ));

                        return Mono.when(
                                session.send(Mono.just(session.textMessage(ack))),
                                sessionRegistry.broadcastToTenant(tenantId, broadcast, session.getId())
                        );
                    } catch (Exception e) {
                        return Mono.error(e);
                    }
                });
    }

    private Mono<Void> handlePull(WebSocketSession session, JsonNode json) throws Exception {
        String tenantId = getTenantId(session);
        String deviceId = json.has("deviceId") ? json.get("deviceId").asText() : session.getId();
        String lastSync = json.has("lastSync") ? json.get("lastSync").asText() : null;

        SyncPullCommand command = new SyncPullCommand(deviceId, lastSync);
        
        return Mono.fromCallable(() -> syncService.pull(tenantId, command))
                .flatMap(response -> {
                    try {
                        String responseJson = objectMapper.writeValueAsString(Map.of(
                                "type", "pull_response",
                                "changes", response.changes(),
                                "changeCount", response.changeCount(),
                                "serverTime", response.serverTime().toString(),
                                "newCursor", response.nextSyncCursor().toString()
                        ));
                        return session.send(Mono.just(session.textMessage(responseJson)));
                    } catch (Exception e) {
                        return Mono.error(e);
                    }
                });
    }

    private Mono<Void> handlePing(WebSocketSession session) throws Exception {
        String pong = objectMapper.writeValueAsString(Map.of(
                "type", "pong",
                "timestamp", Instant.now().toString()
        ));
        return session.send(Mono.just(session.textMessage(pong)));
    }

    private Mono<Void> sendError(WebSocketSession session, String errorMessage) {
        try {
            String error = objectMapper.writeValueAsString(Map.of(
                    "type", "error",
                    "message", errorMessage,
                    "timestamp", Instant.now().toString()
            ));
            return session.send(Mono.just(session.textMessage(error)));
        } catch (Exception e) {
            return Mono.error(e);
        }
    }

    private String getTenantId(WebSocketSession session) {
        Object tenantId = session.getAttributes().get("tenantId");
        return tenantId != null ? tenantId.toString() : "public";
    }

    private String getUserId(WebSocketSession session) {
        Object userId = session.getAttributes().get("userId");
        return userId != null ? userId.toString() : "anonymous";
    }
}
