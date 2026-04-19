# --- Build Stage ---
FROM gradle:8.7-jdk21 AS build
WORKDIR /home/gradle/src
COPY --chown=gradle:gradle . .
RUN ./gradlew :services:sync-service:bootJar --no-daemon

# --- Final Production Stage (with Profiling) ---
FROM eclipse-temurin:21-jre
WORKDIR /app

# Install async-profiler for local performance analysis
RUN apt-get update && apt-get install -y wget curl \
    && wget https://github.com/async-profiler/async-profiler/releases/download/v2.9/async-profiler-2.9-linux-x64.tar.gz \
    && tar -xvzf async-profiler-2.9-linux-x64.tar.gz \
    && rm async-profiler-2.9-linux-x64.tar.gz

COPY --from=build /home/gradle/src/services/sync-service/build/libs/sync-service.jar app.jar

# Expose debugging and actuator ports
EXPOSE 8086 5005
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
