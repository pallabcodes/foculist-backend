# Build stage
FROM gradle:8.7-jdk21 AS build
WORKDIR /home/gradle/src
COPY --chown=gradle:gradle . .
RUN ./gradlew :services:project-service:bootJar --no-daemon

# Run stage
FROM eclipse-temurin:21-jre
COPY --from=build /home/gradle/src/services/project-service/build/libs/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
