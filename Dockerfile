# -----------------------------------------------------------------------------
# STAGE 1: Build the Application
# -----------------------------------------------------------------------------
FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app

# 1. Copy pom.xml and download dependencies (Cached Layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 2. Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests

# -----------------------------------------------------------------------------
# STAGE 2: Run the Application
# -----------------------------------------------------------------------------
# NOTE: Using standard JRE (Debian-based) to ensure native library support for AI features
FROM eclipse-temurin:21-jre

WORKDIR /app

# 1. Setup User, Directories, and Permissions in a SINGLE LAYER
# We chain commands with '&&' and '\' to keep the image small
RUN addgroup --system spring && \
    adduser --system --ingroup spring spring && \
    chown -R spring:spring /app

# 2. Copy the JAR file and set ownership instantly
# Using --chown here removes the need for a separate "RUN chown" layer later
COPY --from=build --chown=spring:spring /app/target/*.jar app.jar

# 3. Switch to the non-root user
USER spring

# Expose port 8080
EXPOSE 8080

# Environment Configuration
ENV HOME=/app
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75 -XX:+UseSerialGC"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]