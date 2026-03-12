package com.yourorg.platform.foculist.sync;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(title = "Sync Service API", version = "v1", description = "Real-time state synchronization for Foculist"))
public class SyncServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(SyncServiceApplication.class, args);
    }
}
