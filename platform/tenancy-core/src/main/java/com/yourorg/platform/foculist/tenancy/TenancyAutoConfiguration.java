package com.yourorg.platform.foculist.tenancy;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import com.yourorg.platform.foculist.tenancy.security.CustomPermissionEvaluator;

import org.springframework.web.client.RestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;

@AutoConfiguration
@EnableConfigurationProperties(TenantContextProperties.class)
public class TenancyAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RestTemplate tenancyRestTemplate(org.springframework.beans.factory.ObjectProvider<RestTemplateBuilder> builderProvider) {
        RestTemplateBuilder builder = builderProvider.getIfAvailable();
        return builder != null ? builder.build() : new RestTemplate();
    }

    @Bean
    JwtClaimExtractor jwtClaimExtractor(
            @Value("${app.security.jwt.secret:Zm9jdWxpc3QtZGV2LWp3dC1zZWNyZXQta2VlcC1jaGFuZ2U=}") String jwtSecret,
            @Value("${app.identity.url:http://localhost:8081}") String identityUrl,
            RestTemplate tenancyRestTemplate
    ) {
        return new JwtClaimExtractor(jwtSecret, identityUrl, tenancyRestTemplate);
    }

    @Bean
    TenantResolver tenantResolver(TenantContextProperties properties, JwtClaimExtractor jwtClaimExtractor) {
        return new TenantResolver(properties, jwtClaimExtractor);
    }

    @org.springframework.context.annotation.Configuration
    @org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication(type = org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.SERVLET)
    public static class TenancyWebMvcConfiguration {
        @Bean
        public FilterRegistrationBean<TenantContextFilter> tenantContextFilter(
                TenantResolver tenantResolver,
                TenantContextProperties properties,
                ObjectMapper objectMapper
        ) {
            FilterRegistrationBean<TenantContextFilter> registration = new FilterRegistrationBean<>();
            registration.setFilter(new TenantContextFilter(tenantResolver, properties, objectMapper));
            registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
            return registration;
        }

        @Bean
        public FilterRegistrationBean<com.yourorg.platform.foculist.tenancy.web.RequestIdFilter> requestIdFilter() {
            FilterRegistrationBean<com.yourorg.platform.foculist.tenancy.web.RequestIdFilter> registration = new FilterRegistrationBean<>();
            registration.setFilter(new com.yourorg.platform.foculist.tenancy.web.RequestIdFilter());
            registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
            return registration;
        }

        @Bean
        public MethodSecurityExpressionHandler methodSecurityExpressionHandler(CustomPermissionEvaluator evaluator) {
            DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
            expressionHandler.setPermissionEvaluator(evaluator);
            return expressionHandler;
        }
    }
}
