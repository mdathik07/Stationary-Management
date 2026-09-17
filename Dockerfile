# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q package -DskipTests

# ---- Run stage ----
FROM eclipse-temurin:17-jre
WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

RUN mkdir -p /logs

COPY --from=build /app/target/*.jar app.jar

RUN useradd -m -u 1000 appuser && \
    chown -R appuser:appuser /app /logs

USER appuser

EXPOSE 8081

HEALTHCHECK --interval=30s \
            --timeout=10s \
            --start-period=40s \
            --retries=3 \
    CMD curl -f http://localhost:8081/actuator/health || exit 1

ENV JAVA_OPTS="-Xmx512m -Xms256m"

ENTRYPOINT exec java $JAVA_OPTS -jar app.jar
