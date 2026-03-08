plugins {
    id("org.springframework.boot") version "3.2.5" apply false
    id("io.spring.dependency-management") version "1.1.4" apply false
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

val serviceProjects = subprojects.filter { it.path.startsWith(":services:") }
val domainServiceProjects = serviceProjects.filter { it.path != ":services:gateway-bff" }

configure(serviceProjects) {
    apply(plugin = "java")
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    dependencies {
        "implementation"("org.springframework.boot:spring-boot-starter-actuator")
        "implementation"("org.springframework.boot:spring-boot-starter-validation")
        "implementation"("io.micrometer:micrometer-registry-prometheus")
        "implementation"("io.micrometer:micrometer-tracing-bridge-otel")
        "implementation"("io.opentelemetry:opentelemetry-exporter-otlp")

        "compileOnly"("org.projectlombok:lombok:1.18.32")
        "annotationProcessor"("org.projectlombok:lombok:1.18.32")

        "testImplementation"("org.springframework.boot:spring-boot-starter-test")
    }
}

configure(domainServiceProjects) {
    dependencies {
        "implementation"("org.springframework.boot:spring-boot-starter-web")
        "implementation"("org.springframework.boot:spring-boot-starter-data-jpa")
        "implementation"("org.springframework.boot:spring-boot-starter-data-mongodb")
        "implementation"("org.springframework.boot:spring-boot-starter-data-redis")
        "implementation"("org.springframework.boot:spring-boot-starter-amqp")
        "implementation"("org.springframework.boot:spring-boot-starter-security")
        "implementation"("org.springframework.boot:spring-boot-starter-oauth2-client")
        "implementation"("org.springframework.kafka:spring-kafka")
        "implementation"("org.flywaydb:flyway-core")
        "implementation"(project(":platform:tenancy-core"))
        "runtimeOnly"("org.postgresql:postgresql")

        "implementation"("io.jsonwebtoken:jjwt-api:0.12.5")
        "runtimeOnly"("io.jsonwebtoken:jjwt-impl:0.12.5")
        "runtimeOnly"("io.jsonwebtoken:jjwt-jackson:0.12.5")

        "implementation"(platform("software.amazon.awssdk:bom:2.25.35"))
        "implementation"("software.amazon.awssdk:sqs")
        "implementation"("software.amazon.awssdk:sns")
        "implementation"("software.amazon.awssdk:dynamodb")
        "implementation"("software.amazon.awssdk:s3")
        "implementation"("software.amazon.awssdk:cognitoidentityprovider")

        "implementation"("io.github.resilience4j:resilience4j-ratelimiter:2.2.0")
    }
}

project(":services:gateway-bff") {
    dependencies {
        "implementation"(platform("org.springframework.cloud:spring-cloud-dependencies:2023.0.3"))
        "implementation"("org.springframework.boot:spring-boot-starter-webflux")
        "implementation"("org.springframework.cloud:spring-cloud-starter-gateway")
        "implementation"("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
        "implementation"("io.jsonwebtoken:jjwt-api:0.12.5")
        "runtimeOnly"("io.jsonwebtoken:jjwt-impl:0.12.5")
        "runtimeOnly"("io.jsonwebtoken:jjwt-jackson:0.12.5")
        "testImplementation"("org.springframework.security:spring-security-test")
    }
}

project(":services:identity-service") {
    dependencies {
        "implementation"("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    }
}

project(":platform:tenancy-core") {
    apply(plugin = "java-library")

    dependencies {
        "api"("org.springframework.boot:spring-boot-autoconfigure:3.2.5")
        "api"("org.springframework.boot:spring-boot-starter-web:3.2.5")
        "implementation"("io.jsonwebtoken:jjwt-api:0.12.5")
        "runtimeOnly"("io.jsonwebtoken:jjwt-impl:0.12.5")
        "runtimeOnly"("io.jsonwebtoken:jjwt-jackson:0.12.5")
        "testImplementation"("org.springframework.boot:spring-boot-starter-test:3.2.5")
    }
}
