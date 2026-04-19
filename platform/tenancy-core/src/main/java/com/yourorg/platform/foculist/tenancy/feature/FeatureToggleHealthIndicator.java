package com.yourorg.platform.foculist.tenancy.feature;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class FeatureToggleHealthIndicator implements HealthIndicator {

    private final FeatureToggleService featureToggleService;

    public FeatureToggleHealthIndicator(FeatureToggleService featureToggleService) {
        this.featureToggleService = featureToggleService;
    }

    @Override
    public Health health() {
        // Basic connectivity check: is the Unleash client initialized?
        // In a real environment, we would also check the last ping to the Unleash API
        try {
            boolean active = featureToggleService.isEnabled("health-check-toggle", true);
            return Health.up()
                    .withDetail("provider", "Unleash")
                    .withDetail("features_active", active)
                    .build();
        } catch (Exception e) {
            return Health.down(e)
                    .withDetail("provider", "Unleash")
                    .build();
        }
    }
}
