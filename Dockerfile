FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# Copy Maven files
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Download dependencies (cacheable)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src ./src

# Build application
RUN ./mvnw clean package -DskipTests

# ====================================================================
# Final image (lighter)
# ====================================================================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Install curl for healthcheck
RUN apk add --no-cache curl

# Copy compiled JAR
COPY --from=builder /app/target/*.jar app.jar

# Create directory for OSM data (will be mounted as volume)
RUN mkdir -p /app/osm-data

# Application port
EXPOSE 8080

# Default environment variables
ENV GRAPHHOPPER_OSM_PATH=/app/osm-data/colombia-latest.osm.pbf \
    GRAPHHOPPER_GRAPH_LOCATION=/app/osm-data/graph-cache

# Startup command
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
