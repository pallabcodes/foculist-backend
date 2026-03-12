package com.yourorg.platform.foculist.sync.clean.infrastructure.websocket;

import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

/**
 * Reactive WebSocket configuration for real-time sync.
 */
@Configuration
public class WebSocketConfig {

    private final SyncWebSocketHandler syncHandler;

    public WebSocketConfig(SyncWebSocketHandler syncHandler) {
        this.syncHandler = syncHandler;
    }

    @Bean
    public HandlerMapping webSocketHandlerMapping() {
        Map<String, SyncWebSocketHandler> map = Map.of("/ws/sync", syncHandler);

        SimpleUrlHandlerMapping handlerMapping = new SimpleUrlHandlerMapping();
        handlerMapping.setOrder(1);
        handlerMapping.setUrlMap(map);
        return handlerMapping;
    }

    @Bean
    public WebSocketHandlerAdapter handlerAdapter() {
        return new WebSocketHandlerAdapter();
    }
}
