package com.yourorg.platform.foculist.identity.clean.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {
    private final OAuth2SuccessHandler oAuth2SuccessHandler;

    @Value("${app.security.jwt.secret:Zm9jdWxpc3QtZGV2LWp3dC1zZWNyZXQta2VlcC1jaGFuZ2U=}")
    private String jwtSecret;

    @Value("${identity.provider:keycloak}")
    private String identityProvider;

    @Value("${JWT_ISSUER_URI:http://localhost:9080/realms/foculist}")
    private String issuerUri;

    @Value("${identity.cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    @Value("${identity.cors.allowed-methods:GET,POST,PUT,PATCH,DELETE,OPTIONS}")
    private String allowedMethods;

    @Value("${identity.cors.allowed-headers:*}")
    private String allowedHeaders;

    @Value("${identity.cors.allow-credentials:true}")
    private boolean allowCredentials;


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher("/v3/api-docs")).permitAll()
                        .requestMatchers(org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher("/v3/api-docs/**")).permitAll()
                        .requestMatchers(org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher("/swagger-ui/**")).permitAll()
                        .requestMatchers(org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher("/swagger-ui.html")).permitAll()
                        .requestMatchers(org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher("/v1/auth/**")).permitAll()
                        .requestMatchers(org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher("/error")).permitAll()
                        .requestMatchers(org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher("/actuator/**")).permitAll()
                        .requestMatchers(org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher("/login/**")).permitAll()
                        .requestMatchers(org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher("/oauth2/**")).permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2.successHandler(oAuth2SuccessHandler))
                .oauth2ResourceServer(resourceServer -> resourceServer.jwt(Customizer.withDefaults()));
        return http.build();
    }

    @Bean
    public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
        org.springframework.web.cors.CorsConfiguration configuration = new org.springframework.web.cors.CorsConfiguration();
        
        java.util.List<String> origins = java.util.Arrays.stream(allowedOrigins.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toList();
        java.util.List<String> methods = java.util.Arrays.stream(allowedMethods.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toList();
        java.util.List<String> headers = java.util.Arrays.stream(allowedHeaders.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toList();

        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(methods);
        configuration.setAllowedHeaders(headers);
        configuration.setAllowCredentials(allowCredentials);

        org.springframework.web.cors.UrlBasedCorsConfigurationSource source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        if ("keycloak".equalsIgnoreCase(identityProvider) || "cognito".equalsIgnoreCase(identityProvider)) {
            // Strictly enforce OIDC validation for real identity providers. No dev bypass fallback allowed.
            return NimbusJwtDecoder.withIssuerLocation(issuerUri).build();
        }
        
        // If explicitly not using an identity provider, use symmetric for standalone testing (if configured)
        return NimbusJwtDecoder.withSecretKey(signingKey())
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private SecretKey signingKey() {
        if (!StringUtils.hasText(jwtSecret)) {
            throw new IllegalStateException("app.security.jwt.secret must be configured");
        }
        byte[] keyBytes = decode(jwtSecret.trim());
        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    private byte[] decode(String rawSecret) {
        try {
            return Base64.getDecoder().decode(rawSecret);
        } catch (IllegalArgumentException ignored) {
            return rawSecret.getBytes(StandardCharsets.UTF_8);
        }
    }
}
