plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    java
}

val moduleName = project.path.substringAfterLast(":")

description = "Foculist service: $moduleName"

dependencies {
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("${moduleName}.jar")
}
