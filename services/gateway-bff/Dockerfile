# Build stage
FROM gradle:8.7-jdk21 AS build
WORKDIR /home/gradle/src
COPY --chown=gradle:gradle . .
RUN ./gradlew :services:gateway-bff:bootJar --no-daemon

# Run stage
FROM eclipse-temurin:21-jre
COPY --from=build /home/gradle/src/services/gateway-bff/build/libs/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
