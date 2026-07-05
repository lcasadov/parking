# Tests e2e (Playwright) — Puerta 3

Suite e2e contra el **stack real integrado** (sin mocks): SPA React (Vite) →
dev-proxy `/parking-api` → backend Spring Boot (perfil `des`) → SQL Server
(Docker). Cubre login local, los flujos de **solicitud (empleado)** y
**aprobación/rechazo (admin)**, y el **plano de puestos en móvil**, con los seeds
de desarrollo del admin (V5) y del empleado (V17).

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
   levantado si existe).

### Puertos ocupados (override)

Si `8080` (backend) o `5173` (Vite) ya están en uso en tu máquina, arranca el
stack en otros puertos y apunta la suite con variables de entorno:

```bash
# backend en 8081
SERVER_PORT=8081 mvn spring-boot:run -Dspring-boot.run.profiles=des -Dspring-boot.run.arguments=--server.port=8081
# frontend proxeando a 8081 (Vite puede auto-incrementar el puerto, p. ej. 5175)
VITE_PROXY_TARGET=http://localhost:8081 npm run dev
# lanzar la suite contra el puerto real del dev server
E2E_BASE_URL=http://localhost:5175 npx playwright test
```

## Ejecutar

```bash
cd frontend
npx playwright test                    # escritorio + móvil
npx playwright test --project=mobile   # solo móvil (Pixel 5)
npx playwright test --project=chromium # solo escritorio
```

Primera vez: `npx playwright install chromium`. La suite corre **en serie**
(`workers: 1`): los specs comparten el empleado dev y la BD (estado mutable), así
que el paralelismo provocaría carreras al crear/cancelar solicitudes.

## Qué cubre

| Proyecto | Test | Escenario |
|---|---|---|
| chromium | `should_login_and_redirect_to_admin_when_valid_credentials` | Login `admin` (V5) → landing real `/admin/employees` + cookie HttpOnly |
| chromium | `should_show_inline_error_when_invalid_credentials` | Password erróneo → error inline, sin modal de sesión expirada |
| chromium | `should_return_current_user_when_auth_me` | `GET /auth/me` con cookies del navegador → 200 |
| chromium | `should_invalidate_session_when_logout` | Logout → `/login`; cookie antigua devuelve 401 |
| chromium | `should_create_parking_and_desk_requests_when_employee_submits_both_for_same_date` | Empleado (V17) solicita **plaza + puesto** para la misma fecha → 2 pendientes |
| chromium | `should_approve_and_reject_pending_requests_as_admin` | Admin **aprueba** (asigna plaza) y **rechaza** → salen de la bandeja |
| mobile   | `should_request_a_desk_from_the_mobile_list_when_employee_taps_solicitar` | En viewport Pixel 5, solicitud desde la lista "Disponibles para solicitar" del plano |

## Notas

- Credenciales de los seeds (solo DES): `admin` / `Admin#Parking2026` (V5),
  `empleado` / `Empleado#Dev2026` (V17).
- Idempotencia: los specs limpian por API las solicitudes pendientes del empleado
  antes de cada test (BD dev compartida) y usan fechas dedicadas dentro de la
  ventana hoy..+14d.
- Sin sleeps: solo auto-waits de Playwright (espíritu Sonar S2925).
- Screenshots y trazas se guardan solo en fallo (`test-results/`).
- **CI**: esta suite NO está cableada en `.github/workflows/ci.yml` — los
  runners no garantizan el stack completo (SQL Server + backend). Ejecución
  local bajo demanda.
