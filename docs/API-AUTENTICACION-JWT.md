# API REST – Autenticación con JWT

Uso de los endpoints seguros y autenticación JWT en NexoERP.

---

## Endpoints públicos (sin token)

- `POST /api/auth/login` — Obtener token JWT.
- `GET /actuator/health` — Estado del servicio.
- `GET /actuator/info` — Información básica de la aplicación.

---

## Login y obtención del token

**Petición:**

```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "tu_contraseña"
}
```

**Respuesta correcta (200):**

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "username": "admin"
}
```

**Respuesta error (401):**

```json
{
  "error": "Credenciales inválidas",
  "status": 401
}
```

El token incluye: usuario, roles y `tenantId` (empresa del usuario). Las llamadas a la API usan ese tenant de forma automática.

---

## Uso del token en la API

En todas las peticiones a `/api/**` (excepto `/api/auth/**`) enviar la cabecera:

```http
Authorization: Bearer <token>
```

Ejemplo con curl:

```bash
curl -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  http://localhost:8081/api/productos
```

---

## Configuración JWT (producción)

En producción definir:

- **`APP_SECURITY_JWT_SECRET`:** Clave Base64 (mín. 256 bits). Generar con:

  ```powershell
  [Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes((New-Guid).Guid + (New-Guid).Guid))
  ```

- **`APP_SECURITY_JWT_EXPIRATION`:** (opcional) Validez del token en milisegundos. Por defecto: 3600000 (1 hora).

No usar el valor por defecto del `application.properties` en producción.

---

## Seguridad de endpoints

| Ruta | Quién puede acceder |
|------|----------------------|
| `/api/auth/**` | Cualquiera (solo login) |
| `/api/**` | Cualquier usuario autenticado (JWT o sesión) |
| `/web/**` | Usuario autenticado por sesión (form login) |
| `/web/admin/**` | Solo rol SAAS_ADMIN |
| `/actuator/health`, `/actuator/info` | Público (para balanceadores) |
| `/actuator/**` (resto) | Solo SAAS_ADMIN |

Las respuestas de error de la API son JSON con formato `{"error": "mensaje", "status": código}`.
