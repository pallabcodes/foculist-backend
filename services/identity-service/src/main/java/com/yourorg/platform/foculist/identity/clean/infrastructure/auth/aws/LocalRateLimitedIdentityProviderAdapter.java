package com.yourorg.platform.foculist.identity.clean.infrastructure.auth.aws;

import com.yourorg.platform.foculist.identity.clean.domain.port.IdentityProviderPort;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.function.Supplier;

public class LocalRateLimitedIdentityProviderAdapter implements IdentityProviderPort {

    private final IdentityProviderPort delegate;
    private final RateLimiter rateLimiter;

    public LocalRateLimitedIdentityProviderAdapter(IdentityProviderPort delegate) {
        this.delegate = delegate;
        
        // Strict local limit: 5 requests per day
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofDays(1))
                .limitForPeriod(5)
                .timeoutDuration(Duration.ZERO) 
                .build();

        RateLimiterRegistry registry = RateLimiterRegistry.of(config);
        this.rateLimiter = registry.rateLimiter("aws-cognito-local-protection");
    }

    private <T> T enforceLimit(Supplier<T> operation) {
        try {
            return RateLimiter.decorateSupplier(rateLimiter, operation).get();
        } catch (io.github.resilience4j.ratelimiter.RequestNotPermitted ex) {
            throw new RuntimeException("LOCAL_DEVELOPMENT_RATE_LIMIT_EXCEEDED: You have exceeded the 5 AWS Cognito requests per day limit allowed during local development. Use LocalStack to bypass.");
        }
    }

    private void enforceLimit(Runnable operation) {
        try {
            RateLimiter.decorateRunnable(rateLimiter, operation).run();
        } catch (io.github.resilience4j.ratelimiter.RequestNotPermitted ex) {
            throw new RuntimeException("LOCAL_DEVELOPMENT_RATE_LIMIT_EXCEEDED: You have exceeded the 5 AWS Cognito requests per day limit allowed during local development. Use LocalStack to bypass.");
        }
    }

    @Override
    public String registerUser(String email, String password, Map<String, String> attributes) {
        return enforceLimit(() -> delegate.registerUser(email, password, attributes));
    }

    @Override
    public Map<String, String> authenticate(String email, String password) {
        return enforceLimit(() -> delegate.authenticate(email, password));
    }

    @Override
    public void confirmUser(String email, String confirmationCode) {
        enforceLimit(() -> delegate.confirmUser(email, confirmationCode));
    }

    @Override
    public Map<String, String> refreshToken(String refreshToken) {
        return enforceLimit(() -> delegate.refreshToken(refreshToken));
    }

    @Override
    public void forgotPassword(String email) {
        enforceLimit(() -> delegate.forgotPassword(email));
    }

    @Override
    public void confirmForgotPassword(String email, String confirmationCode, String newPassword) {
        enforceLimit(() -> delegate.confirmForgotPassword(email, confirmationCode, newPassword));
    }
}
