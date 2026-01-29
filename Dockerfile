# Multi-stage build for SmartNotebook JavaFX Application

# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy Maven files
COPY pom.xml .
COPY src ./src

# Build application (skip tests for faster build)
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copy JAR from build stage
COPY --from=build /app/target/SmartNotebook.jar .

# Copy configuration
COPY config.properties .

# Copy resources
COPY database.sql .

# Expose port (if needed for future web features)
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s \
  CMD java -version || exit 1

# Run application
CMD ["java", "-jar", "SmartNotebook.jar"]
