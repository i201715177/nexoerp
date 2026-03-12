# Despliegue - Sistema Farmacia

Requisito: **Java 17** para compilar y ejecutar (Spring Boot 3.2).

---

## 1. Build del JAR

```bash
cd E:\sistemaPara venderselosalasFarmaciasyboticas
.\mvnw.cmd clean package -DskipTests
```

El JAR queda en: `target\sistema-farmacia-0.0.1-SNAPSHOT.jar`

---

## 2. Ejecutar el JAR (desarrollo / pruebas)

```bash
java -jar target/sistema-farmacia-0.0.1-SNAPSHOT.jar
```

Con perfil de produccion (por ejemplo con Oracle):

```bash
java -jar target/sistema-farmacia-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod,oracle
```

Con perfil Docker (H2 en disco + Redis):

```bash
java -jar target/sistema-farmacia-0.0.1-SNAPSHOT.jar --spring.profiles.active=docker
```

---

## 3. Despliegue con Docker

### Build y ejecución con Docker Compose

```bash
docker-compose up -d --build
```

- App: http://localhost:8080  
- Nginx: http://localhost:80 (proxy a la app)  
- Prometheus: http://localhost:9090  
- Grafana: http://localhost:3000  
- Redis: puerto 6379  

### Solo la aplicación en un contenedor

```bash
docker build -t sistema-farmacia:latest .
docker run -p 8080:8080 -e SPRING_PROFILES_ACTIVE=docker sistema-farmacia:latest
```

---

## 4. Perfiles disponibles

| Perfil   | Uso                                      |
|----------|------------------------------------------|
| `dev`    | Por defecto. H2 en memoria, logs DEBUG.  |
| `docker` | H2 en disco, Redis, sin consola H2.      |
| `prod`   | Validación de esquema, logs reducidos.  |
| `oracle` | Conexión a Oracle (configurar en `application-oracle.properties`). |

---

## 5. Producción con Oracle

1. Crear `src/main/resources/application-oracle.properties` (o configurar variables de entorno):

```properties
spring.datasource.url=jdbc:oracle:thin:@//localhost:1521/XEPDB1
spring.datasource.username=FARMACIA
spring.datasource.password=tu_password
spring.datasource.driver-class-name=oracle.jdbc.OracleDriver
spring.jpa.hibernate.ddl-auto=none
spring.jpa.database-platform=org.hibernate.dialect.OracleDialect
```

2. Ejecutar el script SQL de Oracle para crear esquema y datos.

3. Arrancar con:

```bash
java -jar sistema-farmacia-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod,oracle
```

---

## 6. Ejecución local en IDE (NetBeans / otros)

- **Requisito:** JDK 17 para ejecutar (evitar Java 25 por incompatibilidades).
- En NetBeans: **Project Properties → Run → Java Platform** → elegir **JDK 17**.
- Alternativa: usar el script `run-con-java17.bat` (editar la ruta de JDK 17 dentro del script).

---

## 7. Puertos por defecto

| Servicio   | Puerto |
|-----------|--------|
| App       | 8080   |
| Nginx     | 80     |
| Redis     | 6379   |
| Prometheus| 9090   |
| Grafana   | 3000   |

---

## 8. Salud y documentación API

- Health: http://localhost:8080/actuator/health  
- Swagger UI: http://localhost:8080/swagger-ui.html  
- Métricas Prometheus: http://localhost:8080/actuator/prometheus  
