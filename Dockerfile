# Build stage
FROM eclipse-temurin:11-jdk-jammy as builder
WORKDIR /app

# Add non-root user
RUN addgroup --system --gid 1001 appgroup && \
    adduser --system --uid 1001 --group appgroup

# Copy maven files first for better caching
COPY --chown=appgroup:appgroup mvnw .
COPY --chown=appgroup:appgroup .mvn .mvn
COPY --chown=appgroup:appgroup pom.xml .

# Fix line endings and download dependencies
RUN apt-get update && \
    apt-get install -y --no-install-recommends dos2unix && \
    dos2unix mvnw && \
    chmod +x mvnw && \
    ./mvnw dependency:go-offline && \
    apt-get remove -y dos2unix && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Copy source and build with layers
COPY --chown=appgroup:appgroup src src
RUN ./mvnw package -DskipTests && \
    java -Djarmode=layertools -jar target/*.jar extract

# Runtime stage
FROM eclipse-temurin:11-jre-jammy as runtime
WORKDIR /app

# Add non-root user
RUN addgroup --system --gid 1001 appgroup && \
    adduser --system --uid 1001 --group appgroup && \
    mkdir -p /app/logs && \
    chown -R appgroup:appgroup /app

# Copy layers in order of least to most likely to change
COPY --from=builder --chown=appgroup:appgroup /app/dependencies/ ./
COPY --from=builder --chown=appgroup:appgroup /app/spring-boot-loader/ ./
COPY --from=builder --chown=appgroup:appgroup /app/snapshot-dependencies/ ./
COPY --from=builder --chown=appgroup:appgroup /app/application/ ./

# Configure container
USER appgroup
EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=3s \
  CMD curl -f http://localhost:8081/actuator/health || exit 1

# JVM configuration for containers
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0 -XX:+UseContainerSupport"

# Start application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.JarLauncher"]
