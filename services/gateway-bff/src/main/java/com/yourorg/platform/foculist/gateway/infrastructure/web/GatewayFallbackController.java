package com.yourorg.platform.foculist.gateway.infrastructure.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import java.util.Map;

@RestController
public class GatewayFallbackController {

    @GetMapping("/fallback")
    public Mono<ResponseEntity<Map<String, String>>> defaultFallback() {
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                    "error", "Service Unavailable",
                    "message", "The requested service is temporarily unavailable due to high load or maintenance.",
                    "status", "503"
                )));
    }

    @GetMapping("/fallback/identity")
    public Mono<ResponseEntity<Map<String, String>>> identityFallback() {
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                    "error", "Authentication Service Unavailable",
                    "message", "We are unable to verify your identity at this time. Please try again in a few minutes.",
                    "status", "503"
                )));
    }
}
