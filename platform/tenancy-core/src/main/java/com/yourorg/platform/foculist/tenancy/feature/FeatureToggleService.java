package com.yourorg.platform.foculist.tenancy.feature;

import com.yourorg.platform.foculist.tenancy.TenantContext;
import io.getunleash.DefaultUnleash;
import io.getunleash.Unleash;
import io.getunleash.UnleashContext;
import io.getunleash.util.UnleashConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
public class FeatureToggleService {

    private Unleash unleash;

    @Value("${unleash.url:http://localhost:4242/api}")
    private String unleashUrl;

    @Value("${UNLEASH_TOKEN:default:development.unleash-insecure-api-token}")
    private String unleashToken;

    @PostConstruct
    public void init() {
        UnleashConfig config = UnleashConfig.builder()
                .appName("foculist-platform")
                .instanceId("local")
                .unleashAPI(unleashUrl)
                .customHttpHeader("Authorization", unleashToken)
                .build();

        this.unleash = new DefaultUnleash(config);
    }

    /**
     * Evaluates a feature flag in the context of the CURRENT tenant.
     */
    public boolean isEnabled(String toggleName) {
        String tenantId = TenantContext.get();
        if (tenantId == null) {
            tenantId = "public";
        }
        
        UnleashContext context = UnleashContext.builder()
                .userId(tenantId) // Mapping tenant to userId for standard Unleash strategies
                .addProperty("tenantId", tenantId)
                .build();

        return unleash.isEnabled(toggleName, context);
    }

    public boolean isEnabled(String toggleName, boolean defaultValue) {
        if (unleash == null) return defaultValue;
        return isEnabled(toggleName);
    }
}
