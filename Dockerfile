# Multi-stage Dockerfile for Bharat Spatial Engine
# Stage 1: Build
FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -B

COPY src/ src/
RUN ./mvnw clean package -DskipTests -B

# Stage 2: Minimal Runtime
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN groupadd -r spatial && useradd -r -g spatial spatial

COPY --from=builder --chown=spatial:spatial /workspace/target/bharat-spatial-engine-*.jar app.jar

USER spatial:spatial
EXPOSE 8080

ENV JAVA_OPTS="-XX:+UseZGC -XX:+ZGenerational -XX:+UseStringDeduplication -Dspring.threads.virtual.enabled=true"

HEALTHCHECK --interval=15s --timeout=5s --start-period=20s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
