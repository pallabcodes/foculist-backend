package com.yourorg.platform.foculist.identity.clean.infrastructure.auth;

import com.yourorg.platform.foculist.identity.clean.domain.port.IdentityProviderPort;
import com.yourorg.platform.foculist.identity.clean.infrastructure.auth.aws.AwsCognitoIdentityProviderAdapter;
import com.yourorg.platform.foculist.identity.clean.infrastructure.auth.aws.LocalRateLimitedIdentityProviderAdapter;
import com.yourorg.platform.foculist.identity.clean.infrastructure.auth.local.KeycloakIdentityProviderAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

import java.net.URI;

@Configuration
public class IdentityProviderConfiguration {

    @Value("${app.aws.cognito.userPoolId:fallback-pool}")
    private String userPoolId;

    @Value("${app.aws.cognito.clientId:fallback-client}")
    private String clientId;

    @Value("${app.aws.cognito.endpointOverride:}")
    private String endpointOverride;

    @Value("${keycloak.server-url:http://localhost:9090}")
    private String keycloakUrl;
    
    @Value("${keycloak.realm:foculist}")
    private String keycloakRealm;
    
    @Value("${keycloak.client-id:foculist-local-client}")
    private String keycloakClientId;
    
    @Value("${keycloak.client-secret:foculist-dev-secret-that-is-long}")
    private String keycloakClientSecret;

    @Bean
    public CognitoIdentityProviderClient cognitoClient() {
        var builder = CognitoIdentityProviderClient.builder()
                .region(Region.US_EAST_1);

        if (endpointOverride != null && !endpointOverride.isBlank()) {
            builder.endpointOverride(URI.create(endpointOverride))
                   .credentialsProvider(StaticCredentialsProvider.create(
                           AwsBasicCredentials.create("test", "test")));
        }
        
        return builder.build();
    }

    /**
     * Staging/Production Profile (Tier 1): Raw, unthrottled access to AWS Cognito.
     */
    @Bean
    @Profile({"!local & !aws-local", "aws"})
    public IdentityProviderPort productionIdentityProvider(CognitoIdentityProviderClient cognitoClient) {
        return new AwsCognitoIdentityProviderAdapter(cognitoClient, userPoolId, clientId);
    }

    /**
     * Local Development Profile (Tier 4 Open Source Mod / Dev AWS Stub):
     * Dynamically swaps between Keycloak and LocalStack AWS Cognito based on identity.provider.
     */
    @Bean
    @Profile("local")
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "identity.provider", havingValue = "keycloak", matchIfMissing = true)
    public IdentityProviderPort localKeycloakIdentityProvider() {
        return new KeycloakIdentityProviderAdapter(keycloakUrl, keycloakRealm, keycloakClientId, keycloakClientSecret);
    }

    @Bean
    @Profile("local")
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "identity.provider", havingValue = "cognito")
    public IdentityProviderPort localCognitoIdentityProvider(CognitoIdentityProviderClient cognitoClient) {
        return new AwsCognitoIdentityProviderAdapter(cognitoClient, userPoolId, clientId);
    }

    /**
     * Guarded Local Profile (Tier 5 Guarded Native AWS): If a developer MUST use AWS locally, 
     * this forcefully wraps it in the Resonance4j rate limiter to instantly blow up the app 
     * if they exceed 5 AWS hits per day.
     */
    @Bean
    @Profile("aws-local")
    public IdentityProviderPort guardedAwsLocalIdentityProvider(CognitoIdentityProviderClient cognitoClient) {
        AwsCognitoIdentityProviderAdapter rawAdapter = new AwsCognitoIdentityProviderAdapter(cognitoClient, userPoolId, clientId);
        return new LocalRateLimitedIdentityProviderAdapter(rawAdapter);
    }
}
