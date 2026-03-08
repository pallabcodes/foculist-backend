package com.yourorg.platform.foculist.sync.clean.infrastructure.websocket;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

/**
 * WebSocket handshake interceptor that extracts authentication context
 * from the HTTP upgrade request.
 * <p>
 * Supports two methods of token delivery:
 * <ol>
 *   <li>{@code Authorization: Bearer <token>} header</li>
 *   <li>{@code ?token=<jwt>} query parameter (for SockJS which can't set headers)</li>
 * </ol>
 * <p>
 * Extracts {@code tenantId} from the {@code X-Tenant-ID} header or
 * {@code ?tenantId=<id>} query parameter.
 * <p>
 * Stores extracted attributes on the WebSocket session for use by
 * {@link SyncWebSocketHandler}.
 */
@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private static final Logger log = LoggerFactory.getLogger(JwtHandshakeInterceptor.class);

    public static final String ATTR_TENANT_ID = "ws.tenantId";
    public static final String ATTR_USER_ID = "ws.userId";
    public static final String ATTR_TOKEN = "ws.token";

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {

        // Extract tenant ID from header or query param
        String tenantId = request.getHeaders().getFirst("X-Tenant-ID");
        if (tenantId == null && request instanceof ServletServerHttpRequest servletRequest) {
            tenantId = servletRequest.getServletRequest().getParameter("tenantId");
        }
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = "public"; // Default tenant fallback
        }
        attributes.put(ATTR_TENANT_ID, tenantId);

        // Extract JWT from Authorization header or query param
        String authHeader = request.getHeaders().getFirst("Authorization");
        String token = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        } else if (request instanceof ServletServerHttpRequest servletRequest) {
            token = servletRequest.getServletRequest().getParameter("token");
        }

        if (token != null) {
            attributes.put(ATTR_TOKEN, token);
            // Extract userId from JWT subject (simple base64 decode of payload)
            String userId = extractUserIdFromJwt(token);
            if (userId != null) {
                attributes.put(ATTR_USER_ID, userId);
            }
        }

        log.debug("WebSocket handshake: tenant={}, hasToken={}", tenantId, token != null);
        return true; // Allow connection; downstream handler enforces auth if needed
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                WebSocketHandler wsHandler, Exception exception) {
        // No-op
    }

    /**
     * Minimal JWT userId extraction — parses the payload segment without full validation.
     * Full validation happens at the service layer if needed.
     */
    private String extractUserIdFromJwt(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) return null;
            String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
            // Simple parse: look for "userId":"<value>"
            int idx = payload.indexOf("\"userId\"");
            if (idx < 0) return null;
            int colonIdx = payload.indexOf(':', idx);
            int startQuote = payload.indexOf('"', colonIdx + 1);
            int endQuote = payload.indexOf('"', startQuote + 1);
            if (startQuote < 0 || endQuote < 0) return null;
            return payload.substring(startQuote + 1, endQuote);
        } catch (Exception e) {
            log.debug("Failed to extract userId from JWT: {}", e.getMessage());
            return null;
        }
    }
}
