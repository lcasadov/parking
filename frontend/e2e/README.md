# Tests e2e (Playwright) — Puerta 3

Suite e2e contra el **stack real integrado** (sin mocks): SPA React (Vite) →
dev-proxy `/parking-api` → backend Spring Boot (perfil `des`) → SQL Server
(Docker). Ejercita el login local con el admin del seed de desarrollo (V5).

## Prerrequisitos (arrancar ANTES de lanzar la suite)

1. **SQL Server** (desde la raíz del repo):

   ```bash
   docker compose up -d sqlserver
   ```

   > ⚠️ El servicio `db-init` del compose referencia la imagen
   > `mcr.microsoft.com/mssql-tools18`, que no existe en el registry (bug #16).
   > Mientras se corrige, crea la BD con el sqlcmd del propio contenedor:
   >
   > ```bash
   > docker exec parking-sqlserver /opt/mssql-tools18/bin/sqlcmd \
   >   -S localhost -U sa -P 'Parking!Local2024' -C -b \
   >   -Q "IF DB_ID(N'parking') IS NULL CREATE DATABASE parking;"
   > ```

2. **Backend** (JDK 21; Flyway aplica V1–V5, incluido el seed del admin de DES):

   ```bash
   cd backend
   mvn spring-boot:run -Dspring-boot.run.profiles=des
   ```

   Espera a ver `Started ParkingApplication`. API en
   `http://localhost:8080/parking-api`.

3. **Frontend**: no hace falta arrancarlo a mano — `playwright.config.ts`
   orquesta `npm run dev` vía `webServer` (y reutiliza un dev server ya
   levantado en `:5173` si existe).

## Ejecutar

```bash
cd frontend
npm run test:e2e
```

Primera vez: `npx playwright install chromium`.

## Qué cubre

| Test | Escenario |
|---|---|
| `should_login_and_redirect_to_admin_when_valid_credentials` | Login `admin` / seed V5 → redirect a `/admin` + cookie `parking_SESSION` HttpOnly |
| `should_show_inline_error_when_invalid_credentials` | Password erróneo → error inline, sigue en `/login`, sin modal de sesión expirada |
| `should_return_current_user_when_auth_me` | Tras login UI, `GET /auth/me` con las cookies del navegador → 200 `{login, role, passwordMustChange}` |
| `should_invalidate_session_when_logout` | Logout desde el header → `/login`; la cookie antigua ya devuelve 401 en `/auth/me` |

## Notas

- Credenciales del seed (solo DES, ver `backend/.../db/seed/dev/V5__seed_dev_admin.sql`):
  `admin` / `Admin#Parking2026`.
- Sin sleeps: solo auto-waits de Playwright (espíritu Sonar S2925).
- Screenshots y trazas se guardan solo en fallo (`test-results/`).
- **CI**: esta suite NO está cableada en `.github/workflows/ci.yml` — los
  runners no garantizan el stack completo (SQL Server + backend). Ejecución
  local bajo demanda.
