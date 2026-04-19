# --- Build Stage ---
FROM gradle:8.7-jdk21 AS build
WORKDIR /home/gradle/src
COPY --chown=gradle:gradle . .
# Using cache to speed up repeated builds
RUN --mount=type=cache,target=/home/gradle/.gradle \
    ./gradlew :services:identity-service:bootJar --no-daemon

# --- Final Production Stage (Distroless) ---
FROM gcr.io/distroless/java21-debian12:latest
WORKDIR /app
COPY --from=build /home/gradle/src/services/identity-service/build/libs/identity-service.jar app.jar

# Standard security best practice: Non-root execution is default in distroless/java
# Environment variables for cloud-native integration
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8081 9090
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
