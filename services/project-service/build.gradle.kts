plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    java
}

val moduleName = project.path.substringAfterLast(":")

description = "Foculist service: $moduleName"

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("${moduleName}.jar")
}
