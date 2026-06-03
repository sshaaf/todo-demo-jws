####
# This Dockerfile uses Red Hat UBI (Universal Base Image) for Quarkus
# Multi-stage build for optimal image size and security
####

## Stage 1: Build the application
FROM registry.access.redhat.com/ubi9/openjdk-21:latest AS builder

# Set working directory
WORKDIR /build

# Copy entire project
COPY --chown=default:root . .

# Build the application (creates quarkus-app/ in target/)
RUN mvn package -DskipTests -B

## Stage 2: Runtime image
FROM registry.access.redhat.com/ubi9/openjdk-21-runtime:latest

# Set working directory
WORKDIR /deployments

# Copy the Quarkus application from builder stage
COPY --from=builder --chown=185:0 /build/target/quarkus-app/lib/ ./lib/
COPY --from=builder --chown=185:0 /build/target/quarkus-app/*.jar ./
COPY --from=builder --chown=185:0 /build/target/quarkus-app/app/ ./app/
COPY --from=builder --chown=185:0 /build/target/quarkus-app/quarkus/ ./quarkus/

# Security: Run as non-root user (default in UBI Java runtime images)
USER 185

# Environment variables (overridable via ConfigMap/Secret in OpenShift)
ENV QUARKUS_HTTP_PORT=8080 \
    JAVA_OPTIONS="-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager" \
    JAVA_APP_JAR="/deployments/quarkus-run.jar"

# Expose HTTP port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "quarkus-run.jar"]
