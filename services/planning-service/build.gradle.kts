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
dependencies {
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.5.0")
    implementation(project(":platform:tenancy-core"))
}
