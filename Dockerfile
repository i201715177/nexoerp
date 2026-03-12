## Imagen multi-stage para construir y ejecutar NexoERP

### Etapa 1: build con Maven
FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /app

COPY pom.xml ./
COPY .mvn .mvn
COPY mvnw mvnw.cmd ./

RUN chmod +x mvnw

RUN ./mvnw -q -DskipTests dependency:go-offline

COPY src src

RUN ./mvnw -q -DskipTests package

### Etapa 2: imagen ligera para produccion
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

RUN useradd -r -s /bin/false appuser && chown -R appuser:appuser /app
USER appuser

COPY --from=build /app/target/nexoerp-*.jar app.jar

EXPOSE 8082

ENV SPRING_PROFILES_ACTIVE=postgres

ENV JAVA_OPTS="-XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=75.0 \
  -XX:InitialRAMPercentage=50.0 \
  -XX:+UseG1GC \
  -XX:G1HeapRegionSize=4m \
  -XX:+UseStringDeduplication \
  -XX:+OptimizeStringConcat \
  -XX:+ParallelRefProcEnabled \
  -XX:MaxGCPauseMillis=200 \
  -XX:+AlwaysPreTouch \
  -Djava.security.egd=file:/dev/./urandom \
  -Dfile.encoding=UTF-8"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
