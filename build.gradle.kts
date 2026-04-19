plugins {
    id("org.springframework.boot") version "3.2.5" apply false
    id("io.spring.dependency-management") version "1.1.4" apply false
    id("com.google.protobuf") version "0.9.4" apply false
    java
}

allprojects {
    group = "com.yourorg.platform.foculist"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}

// Common configuration for all microservices
val microservices = listOf(
    ":services:gateway-bff",
    ":services:identity-service",
    ":services:planning-service",
    ":services:project-service",
    ":services:sync-service",
    ":services:calendar-service",
    ":services:meeting-service",
    ":services:resource-service"
)

configure(microservices.map { project(it) }) {
    apply(plugin = "java")
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")

    dependencies {
        "implementation"("org.springframework.boot:spring-boot-starter")
        "implementation"("org.springframework.boot:spring-boot-starter-actuator")
        "implementation"("org.springframework.boot:spring-boot-starter-validation")
        "implementation"("io.micrometer:micrometer-registry-prometheus")
        "implementation"("io.micrometer:micrometer-tracing-bridge-otel")
        "implementation"("io.opentelemetry:opentelemetry-exporter-otlp")
        "implementation"("net.logstash.logback:logstash-logback-encoder:7.4")

        "compileOnly"("org.projectlombok:lombok:1.18.32")
        "annotationProcessor"("org.projectlombok:lombok:1.18.32")

        "testImplementation"("org.springframework.boot:spring-boot-starter-test")

        "implementation"(platform("software.amazon.awssdk:bom:2.25.35"))
        "implementation"("io.awspring.cloud:spring-cloud-aws-starter-sqs:3.1.1")
        "implementation"("io.awspring.cloud:spring-cloud-aws-starter-secrets-manager:3.1.1")
        "implementation"("software.amazon.awssdk:sqs")
        "implementation"("software.amazon.awssdk:sns")
        "implementation"("software.amazon.awssdk:dynamodb")
        "implementation"("software.amazon.awssdk:s3")
        "implementation"("software.amazon.awssdk:cognitoidentityprovider")
    }
}

// WebMVC Services (Standard)
val mvcServices = listOf(
    ":services:identity-service",
    ":services:planning-service",
    ":services:project-service",
    ":services:calendar-service",
    ":services:meeting-service",
    ":services:resource-service"
)

configure(mvcServices.map { project(it) }) {
    dependencies {
        "implementation"("org.springframework.boot:spring-boot-starter-web")
        "implementation"("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.5.0")
        
        "implementation"("org.springframework.boot:spring-boot-starter-data-jpa")
        "implementation"("org.springframework.boot:spring-boot-starter-data-mongodb")
        "implementation"("org.springframework.boot:spring-boot-starter-data-redis")
        "implementation"("org.springframework.boot:spring-boot-starter-amqp")
        "implementation"("org.springframework.boot:spring-boot-starter-security")
        "implementation"("org.springframework.boot:spring-boot-starter-oauth2-client")
        "implementation"("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
        "implementation"("org.springframework.kafka:spring-kafka")
        "implementation"("org.flywaydb:flyway-core")
        "implementation"(project(":platform:tenancy-core"))
        "implementation"("org.postgresql:postgresql")

        "implementation"("io.jsonwebtoken:jjwt-api:0.12.5")
        "runtimeOnly"("io.jsonwebtoken:jjwt-impl:0.12.5")
        "runtimeOnly"("io.jsonwebtoken:jjwt-jackson:0.12.5")

        "implementation"("io.hypersistence:hypersistence-utils-hibernate-63:3.7.3")

        "implementation"("io.getunleash:unleash-client-java:9.2.0")
        "implementation"("io.github.resilience4j:resilience4j-ratelimiter:2.2.0")
    }
}

// WebFlux Services (Reactive)
val fluxServices = listOf(
    ":services:gateway-bff",
    ":services:sync-service"
)

configure(fluxServices.map { project(it) }) {
    dependencies {
        "implementation"("org.springframework.boot:spring-boot-starter-webflux")
        "implementation"("org.springframework.boot:spring-boot-starter-security")
        "implementation"("org.springdoc:springdoc-openapi-starter-webflux-ui:2.5.0")
        "implementation"(project(":platform:tenancy-core"))
        "implementation"("org.postgresql:postgresql")
    }
}

project(":services:gateway-bff") {
    apply(plugin = "com.google.protobuf")
    apply(from = "$rootDir/protobuf.gradle")
    dependencies {
        "implementation"(platform("org.springframework.cloud:spring-cloud-dependencies:2023.0.3"))
        "implementation"("org.springframework.cloud:spring-cloud-starter-gateway")
        "implementation"("org.springframework.cloud:spring-cloud-starter-circuitbreaker-reactor-resilience4j")
        "implementation"("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
        "implementation"("io.jsonwebtoken:jjwt-api:0.12.5")
        "runtimeOnly"("io.jsonwebtoken:jjwt-impl:0.12.5")
        "runtimeOnly"("io.jsonwebtoken:jjwt-jackson:0.12.5")
        "implementation"("net.devh:grpc-client-spring-boot-starter:3.1.0.RELEASE")
        "implementation"("io.grpc:grpc-netty:1.63.0")
        "implementation"("io.grpc:grpc-netty-shaded:1.63.0")
        "implementation"("io.grpc:grpc-protobuf:1.63.0")
        "implementation"("io.grpc:grpc-stub:1.63.0")
        "implementation"("javax.annotation:javax.annotation-api:1.3.2")
        "testImplementation"("org.springframework.security:spring-security-test")
    }
}

project(":services:identity-service") {
    apply(plugin = "com.google.protobuf")
    apply(from = "$rootDir/protobuf.gradle")
    dependencies {
        "implementation"("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
        "implementation"("net.devh:grpc-server-spring-boot-starter:3.1.0.RELEASE")
        "implementation"("io.grpc:grpc-netty-shaded:1.63.0")
        "implementation"("io.grpc:grpc-protobuf:1.63.0")
        "implementation"("io.grpc:grpc-stub:1.63.0")
        "implementation"("javax.annotation:javax.annotation-api:1.3.2")
    }
}

project(":platform:tenancy-core") {
    apply(plugin = "java-library")

    dependencies {
        "api"("org.springframework.boot:spring-boot-autoconfigure:3.2.5")
        "api"("org.springframework.boot:spring-boot-starter-actuator:3.2.5")
        "compileOnly"("org.springframework.boot:spring-boot-starter-web:3.2.5")
        "compileOnly"("org.springframework.boot:spring-boot-starter-security:3.2.5")
        "compileOnly"("org.springframework.boot:spring-boot-starter-oauth2-resource-server:3.2.5")
        "compileOnly"("org.springframework.boot:spring-boot-starter-aop:3.2.5")
        "api"("io.micrometer:micrometer-tracing-bridge-otel:1.2.5")
        "api"("io.opentelemetry:opentelemetry-api:1.37.0")
        "implementation"("io.jsonwebtoken:jjwt-api:0.12.5")
        "runtimeOnly"("io.jsonwebtoken:jjwt-impl:0.12.5")
        "runtimeOnly"("io.jsonwebtoken:jjwt-jackson:0.12.5")
        "implementation"("org.springframework.boot:spring-boot-starter-json:3.2.5")
        "api"("io.getunleash:unleash-client-java:9.2.0")
        "api"("io.github.resilience4j:resilience4j-ratelimiter:2.2.0")
        "api"("io.github.resilience4j:resilience4j-retry:2.2.0")
        "api"("jakarta.annotation:jakarta.annotation-api:2.1.1")
        "compileOnly"("org.projectlombok:lombok:1.18.32")
        "annotationProcessor"("org.projectlombok:lombok:1.18.32")
        "testImplementation"("org.springframework.boot:spring-boot-starter-test:3.2.5")
        "testImplementation"("jakarta.servlet:jakarta.servlet-api:6.0.0")
    }
}

