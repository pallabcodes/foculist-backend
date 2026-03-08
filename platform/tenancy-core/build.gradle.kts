plugins {
    `java-library`
    id("io.spring.dependency-management")
}

description = "Shared multi-tenancy core for foculist services"

dependencies {
    // Security and JWT support used by RoleSecurityAspect
    api("org.springframework.boot:spring-boot-starter-security:3.2.5")
    api("org.springframework.boot:spring-boot-starter-oauth2-resource-server:3.2.5")
    // AspectJ annotations used for declarative role enforcement
    api("org.springframework.boot:spring-boot-starter-aop:3.2.5")
    // Web used for RestTemplate to check grants
    api("org.springframework.boot:spring-boot-starter-web:3.2.5")
}
