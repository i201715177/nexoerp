## Etapa 1: build
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn -q -DskipTests package

## Etapa 2: runtime (JRE ligero)
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

COPY --from=build /app/target/nexoerp-1.0.0.jar app.jar

RUN useradd -r -s /bin/false appuser && chown -R appuser:appuser /app
USER appuser

EXPOSE 8080

ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC"
ENV PORT=8080

HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh","-c","java ${JAVA_OPTS} -jar app.jar"]

