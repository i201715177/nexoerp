FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="NexoERP"
LABEL description="Sistema de gestion farmaceutica multi-tenant"

RUN addgroup -S nexoerp && adduser -S nexoerp -G nexoerp

WORKDIR /app

COPY target/nexoerp-1.0.0.jar app.jar

RUN mkdir -p /app/logs /app/backups && \
    chown -R nexoerp:nexoerp /app

USER nexoerp

EXPOSE 8082

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8082/actuator/health || exit 1

ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:+UseContainerSupport -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
