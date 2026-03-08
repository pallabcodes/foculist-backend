plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    java
}

val moduleName = project.path.substringAfterLast(":")

description = "Foculist service: $moduleName"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    testImplementation("org.springframework.security:spring-security-test")
    testRuntimeOnly("com.h2database:h2")
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("${moduleName}.jar")
}
