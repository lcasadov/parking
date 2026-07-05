# Proposal: e2e-employee-request-approval-mobile

## Why
Los flujos más críticos para el usuario —un empleado solicitando plaza y/o puesto,
y un administrador aprobándolos o rechazándolos— funcionan (verificado por API en
vivo) pero **no tienen cobertura E2E**. El CI no ejecuta Playwright, y el único e2e
existente (`auth-login`) tiene una aserción **obsoleta** (`loginAsAdmin` espera la
URL `/admin$` cuando la landing real es `/admin/employees`), por lo que 3 de sus 4
casos fallan. Este change añade la red E2E de los flujos de negocio clave, incluida
la vista de plano en **móvil**, y arregla el e2e roto.

## What Changes
- **Seed de empleado dev** (`db/seed/dev`) para poder autenticar un `EMPLOYEE` en E2E (hoy solo hay `admin`).
- **Fix** del helper obsoleto en `auth-login.spec.ts`: la landing del admin es `/admin/employees`.
- **E2E empleado**: solicitar plaza y puesto para la misma fecha; ver sus solicitudes.
- **E2E admin**: ver solicitudes pendientes, aprobar (plaza y puesto) y rechazar.
- **E2E móvil** (emulación de dispositivo Playwright): plano de puestos con la lista "Disponibles para solicitar" y solicitud desde móvil.

## Capabilities
- `requests` (MODIFIED — se añade el requisito de cobertura E2E de los flujos de solicitud/aprobación; sin cambio de comportamiento del sistema)

## Impact
- **Tests**: `frontend/e2e/*.spec.ts` (nuevos specs + fix), `frontend/playwright.config.ts` (proyecto móvil), posible helper de setup (crear/loguear empleado).
- **Seed**: nueva migración en `db/seed/dev` (empleado dev), solo perfil `des`; no afecta a PRE/PRO.
- **Comportamiento del sistema / contrato API**: sin cambios (solo se añade cobertura de test y datos de desarrollo).

## Out of scope
- Cambios de contrato API o de backend de dominio.
- Ejecutar Playwright en el CI (se documenta cómo correrlo; su integración en el pipeline es un change de devops aparte).
- Nuevas pantallas o lógica de negocio.
