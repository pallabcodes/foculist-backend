package com.yourorg.platform.foculist.common.resilience;

import io.github.resilience4j.retry.event.RetryEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LoggingRetryListener {

    public void onRetryEvent(RetryEvent event) {
        switch (event.getEventType()) {
            case RETRY:
                log.warn("⚠️ Retry attempt #{} for '{}'. Last failure: {}", 
                        event.getNumberOfRetryAttempts(), event.getName(), event.getLastThrowable().getMessage());
                break;
            case SUCCESS:
                log.info("✅ Retry successful for '{}' after {} attempts.", 
                        event.getName(), event.getNumberOfRetryAttempts());
                break;
            case ERROR:
                log.error("❌ Retry failed for '{}' after {} attempts. Exhausted.", 
                        event.getName(), event.getNumberOfRetryAttempts());
                break;
            default:
                break;
        }
    }
}
