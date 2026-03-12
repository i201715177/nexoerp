# Despliegue de NexoERP en producción

Pasos necesarios para dejar el sistema listo y desplegado en producción.

---

## 1. Requisitos previos

- **JDK 17** (para build local)
- **Maven** o el wrapper del proyecto (`mvnw` / `mvnw.cmd`)
- **Git** (para subir código)
- Cuenta en un proveedor de nube (Render, Fly.io, AWS, etc.) o servidor propio con Docker

---

## 2. Build del proyecto

### Opción A: Scripts incluidos

```bash
# Windows (desde la raíz del proyecto)
scripts\build.bat

# Linux/macOS
chmod +x scripts/build.sh
./scripts/build.sh
```

### Opción B: Maven directo

```bash
./mvnw clean package -DskipTests
```

El JAR resultante: `target/nexoerp-1.0.0.jar`

### Ejecutar en local (prueba)

```bash
java -jar target/nexoerp-1.0.0.jar
# Por defecto escucha en http://localhost:8081
```

---

## 3. Variables de entorno en producción

Configurar siempre en el entorno de despliegue:

| Variable | Descripción | Ejemplo |
|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | Perfil (postgres, prod, etc.) | `postgres` |
| `DATABASE_URL` | URL JDBC de la base de datos | `jdbc:postgresql://host:5432/nexoerp` |
| `DATABASE_USERNAME` | Usuario BD | `nexoerp` |
| `DATABASE_PASSWORD` | Contraseña BD | *(secreto)* |
| `APP_SECURITY_JWT_SECRET` | Clave Base64 para JWT (mín. 256 bits) | *(generar, ver abajo)* |
| `PORT` | Puerto del servidor (en muchos clouds lo asigna el proveedor) | `8080` |

### Generar clave JWT segura (Base64)

**PowerShell:**
```powershell
[Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes((New-Guid).Guid + (New-Guid).Guid))
```

**Linux/macOS:**
```bash
openssl rand -base64 48
```

Usar el resultado en `APP_SECURITY_JWT_SECRET`. No compartir ni subir a repositorios.

---

## 4. Base de datos

- **Desarrollo:** H2 en archivo (por defecto), no requiere configuración.
- **Producción:** Usar PostgreSQL (recomendado) o Oracle.

Activar perfil según BD:

- PostgreSQL: `SPRING_PROFILES_ACTIVE=postgres` y configurar `DATABASE_*`.
- Ver `application-postgres.properties` para propiedades soportadas.

---

## 5. Despliegue con Docker

### Build de la imagen

```bash
# Windows
scripts\deploy-docker.bat

# Linux/macOS
chmod +x scripts/deploy-docker.sh
./scripts/deploy-docker.sh
```

Para construir y levantar el stack (app + Redis + Nginx + Prometheus + Grafana):

```bash
./scripts/deploy-docker.sh up
# o: scripts\deploy-docker.bat up
```

### Docker Compose (stack completo)

Desde la raíz del proyecto:

```bash
docker compose up -d
```

Puertos por defecto:

- App: 8080
- Nginx: 80, 443
- Prometheus: 9090
- Grafana: 3000

Ajustar variables en `docker-compose.yml` o en un `.env` (no versionado).

---

## 6. Despliegue en Render.com (gratuito)

Ver guía detallada en la raíz del proyecto: **`DEPLOY-GRATUITO.md`**.

Resumen:

1. Subir código a GitHub.
2. Crear cuenta en Render y conectar el repositorio.
3. Crear base de datos PostgreSQL (Free).
4. Crear Web Service con Runtime **Docker**, apuntando al Dockerfile del proyecto.
5. Configurar variables: `SPRING_PROFILES_ACTIVE=postgres`, `DATABASE_URL`, `APP_SECURITY_JWT_SECRET`.
6. Desplegar; la URL será del tipo `https://nexoerp.onrender.com`.

---

## 7. Seguridad y endpoints

- **Web:** autenticación por formulario (sesión). Rutas bajo `/web/**` requieren login.
- **API REST:** autenticación por **JWT**.
  - Login: `POST /api/auth/login` con body `{"username":"...", "password":"..."}`.
  - Respuesta: `{"token":"...", "username":"..."}`.
  - Llamadas a `/api/**`: cabecera `Authorization: Bearer <token>`.
- El token incluye `tenantId` para multi-tenant; las peticiones API usan ese contexto.
- En producción: usar HTTPS, clave JWT fuerte y BD con contraseña segura.

Detalle completo: **`docs/API-AUTENTICACION-JWT.md`**.

---

## 8. Logs y monitoreo

- **Logs:** por defecto en `logs/nexoerp.log` (y rotación por día/tamaño según `logback-spring.xml`).
- **Actuator:** endpoints expuestos: `health`, `info`, `prometheus` (métricas).
  - `GET /actuator/health` — estado del servicio (p. ej. para load balancers).
  - En producción restringir `/actuator/**` a red interna o admin (ya configurado para rol SAAS_ADMIN donde aplica).
- **Prometheus/Grafana:** opcionales; configuración de ejemplo en `monitoring/` y en `docker-compose.yml`.

---

## 9. Checklist pre-producción

- [ ] `APP_SECURITY_JWT_SECRET` generado y configurado (no por defecto).
- [ ] Base de datos de producción creada y variables `DATABASE_*` configuradas.
- [ ] Perfil `postgres` (o el que use) activado.
- [ ] HTTPS activo (inversor proxy o proveedor de cloud).
- [ ] Usuario admin (SAAS_ADMIN) creado y contraseña cambiada.
- [ ] Logs y métricas revisables (ruta de logs y/o Prometheus/Grafana si se usan).

Con esto el sistema queda listo para revisión y uso en producción.
