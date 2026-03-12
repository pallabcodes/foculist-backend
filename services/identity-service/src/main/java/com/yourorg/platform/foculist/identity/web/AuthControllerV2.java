package com.yourorg.platform.foculist.identity.web;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/v2/auth")
@RequiredArgsConstructor
public class AuthControllerV2 {

    @GetMapping("/info")
    public Mono<Map<String, Object>> getAuthInfo() {
        return Mono.just(Map.of(
            "version", "v2.0.0-beta",
            "message", "This is the enhanced Auth V2 endpoint with biometric-ready schema.",
            "status", "Experimental"
        ));
    }

    @GetMapping("/login")
    public Mono<Map<String, String>> loginv2() {
        return Mono.just(Map.of(
            "message", "Login V2 is currently undergoing security audit. Please use V1 for production.",
            "documentation", "https://docs.foculist.com/api/v2/auth/login"
        ));
    }
}
