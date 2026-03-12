## Imagen multi-stage para construir y ejecutar NexoERP con Spring Boot y PostgreSQL

### Etapa 1: build con Maven Wrapper
FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copiamos solo archivos de configuración primero para aprovechar la cache
COPY pom.xml ./
COPY .mvn .mvn
COPY mvnw mvnw.cmd ./

RUN chmod +x mvnw

# Descarga dependencias (sin código todavía) para cachear
RUN ./mvnw -q -DskipTests dependency:go-offline

# Ahora copiamos el código fuente completo y construimos el JAR
COPY src src

RUN ./mvnw -q -DskipTests package

### Etapa 2: imagen ligera para producción
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Copiamos el JAR generado desde la etapa de build
COPY --from=build /app/target/nexoerp-*.jar app.jar

# Puerto por defecto de la app (coincide con server.port)
EXPOSE 8082

# Variables de entorno típicas (pueden ser sobreescritas por Render)
ENV SPRING_PROFILES_ACTIVE=postgres

# Comando de arranque
ENTRYPOINT ["java","-jar","/app/app.jar"]

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

