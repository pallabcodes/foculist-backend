package com.yourorg.platform.foculist.sync.clean.infrastructure.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket configuration for real-time sync.
 * <p>
 * Registers the {@link SyncWebSocketHandler} at {@code /ws/sync} with
 * SockJS fallback for browsers that don't support native WebSockets.
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final SyncWebSocketHandler syncHandler;
    private final JwtHandshakeInterceptor jwtInterceptor;

    public WebSocketConfig(SyncWebSocketHandler syncHandler,
                           JwtHandshakeInterceptor jwtInterceptor) {
        this.syncHandler = syncHandler;
        this.jwtInterceptor = jwtInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(syncHandler, "/ws/sync")
                .addInterceptors(jwtInterceptor)
                .setAllowedOriginPatterns("*")
                .withSockJS();

        // Raw WebSocket (no SockJS) for native clients
        registry.addHandler(syncHandler, "/ws/sync")
                .addInterceptors(jwtInterceptor)
                .setAllowedOriginPatterns("*");
    }
}
