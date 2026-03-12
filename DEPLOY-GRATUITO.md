# Despliegue NexoERP en la nube (gratuito, ~30K datos)

Guía para desplegar NexoERP gratis hasta que el sistema crezca. Pensado para unos ~30 mil registros/datos.

---

## Opción recomendada: Render.com

**Render** ofrece plan gratuito suficiente para empezar (PostgreSQL incluido) y crece contigo.

### URLs y descargas

| Qué | URL | Descarga |
|-----|-----|----------|
| **Render (hosting)** | https://render.com | Crear cuenta en la web |
| **Git** (si no lo tienes) | https://git-scm.com/downloads | Instalar para Windows/Mac/Linux |
| **GitHub** | https://github.com | Crear cuenta + repositorio |

---

## Pasos para desplegar en Render

### 1. Subir el proyecto a GitHub

```bash
# En la carpeta del proyecto (donde está el pom.xml)
git init
git add .
git commit -m "NexoERP listo para despliegue"
git branch -M main
git remote add origin https://github.com/TU_USUARIO/nexoerp.git
git push -u origin main
```

> Si no tienes Git instalado: https://git-scm.com/download/win (Windows) o descarga desde la web.

### 2. Crear cuenta en Render

1. Entra a **https://render.com**
2. Sign up con GitHub (recomendado, así conectas el repo directo)
3. Conecta tu cuenta de GitHub si te lo pide

### 3. Crear base de datos PostgreSQL (gratis)

1. En el Dashboard de Render: **New** → **PostgreSQL**
2. Nombre: `nexoerp-db`
3. Region: `Oregon (US West)` u otra cercana
4. Plan: **Free**
5. **Create Database**
6. Espera unos minutos. Luego copia:
   - **Internal Database URL** (algo como `postgresql://user:pass@dpg-xxx/dbname`)

### 4. Crear Web Service (tu app)

1. **New** → **Web Service**
2. Conecta el repositorio **nexoerp** (de tu GitHub)
3. Configura:
   - **Name**: `nexoerp`
   - **Region**: la misma que la BD
   - **Branch**: `main`
   - **Runtime**: `Docker` (usa tu Dockerfile)
   - **Instance Type**: **Free**

4. En **Environment Variables** agrega:

   | Variable | Valor |
   |----------|-------|
   | `SPRING_PROFILES_ACTIVE` | `postgres` |
   | `DATABASE_URL` | *(pegar Internal Database URL de Render)* |
   | `DATABASE_USERNAME` | *(usuario que te da Render en la URL)* |
   | `DATABASE_PASSWORD` | *(contraseña que te da Render en la URL)* |
   | `APP_SECURITY_JWT_SECRET` | *(genera una clave Base64 larga, ver abajo)* |

5. **Create Web Service**

### 5. Generar clave JWT segura

En PowerShell o CMD:

```powershell
powershell -Command "[Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes((New-Guid).Guid + (New-Guid).Guid))"
```

Copia el resultado y pégalo en `APP_SECURITY_JWT_SECRET`.

### 6. Conectar la BD con el Web Service

1. En el panel de tu **PostgreSQL**, entra a la pestaña **Info**
2. Copia **Internal Database URL**
3. En el **Web Service** → **Environment** → Edita `DATABASE_URL` y pega esa URL completa

> La Internal URL ya incluye usuario y contraseña; si la usas, no necesitas `DATABASE_USERNAME` y `DATABASE_PASSWORD` por separado. Render inyecta bien la URL.

### 7. Primer despliegue

1. Guarda los cambios en el Web Service
2. Render construye y despliega automáticamente
3. Tu app estará en: `https://nexoerp.onrender.com` (o el nombre que elegiste)

---

## Límites del plan gratuito Render

- **Web Service**: se duerme tras 15 min sin tráfico; el primer request después tarda ~30 seg en despertar
- **PostgreSQL Free**: 1 GB, 90 días (luego migrar a plan de pago o exportar datos)
- **Build**: 400 minutos/mes gratis

Para ~30 mil registros (clientes, productos, ventas) el Free suele bastar. Si creces, pasa al plan de pago ($7/mes aprox).

---

## Alternativa: Fly.io

Si prefieres Fly.io (también gratis, no se duerme tanto):

| Qué | URL |
|-----|-----|
| **Fly.io** | https://fly.io |
| **Flyctl (CLI)** | https://fly.io/docs/hands-on/install-flyctl/ |

```bash
# Instalar flyctl (Windows con PowerShell)
powershell -Command "iwr https://fly.io/install.ps1 -useb | iex"

# Login
flyctl auth login

# Desde la carpeta del proyecto
flyctl launch
# Responde: nombre app, región (lax = Los Ángeles), NO crear Postgres ahora

# Crear Postgres
flyctl postgres create --name nexoerp-db --region lax

# Vincular BD a la app
flyctl postgres attach <nombre-del-postgres>

# Configurar secretos
flyctl secrets set SPRING_PROFILES_ACTIVE=postgres
flyctl secrets set APP_SECURITY_JWT_SECRET="tu-clave-base64-aqui"

# Desplegar
flyctl deploy
```

---

## Resumen rápido

1. **Git** + **GitHub**: sube tu código
2. **Render** o **Fly.io**: crea cuenta
3. **PostgreSQL** gestionado (Render o Fly)
4. **Web Service** con Dockerfile
5. Variables: `SPRING_PROFILES_ACTIVE`, `DATABASE_URL`, `APP_SECURITY_JWT_SECRET`
6. Despliega y usa la URL que te den

Cuando el sistema crezca, migras a un plan de pago o a AWS/Azure con los mismos perfiles de configuración.
