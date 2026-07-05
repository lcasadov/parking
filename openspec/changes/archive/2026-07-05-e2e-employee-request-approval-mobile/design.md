# Design: e2e-employee-request-approval-mobile

## Context
Los E2E de "Puerta 3" (`frontend/e2e/auth-login.spec.ts`) prueban el login real
contra el backend Spring Boot + SQL Server vía el dev-proxy de Vite. El CI no los
ejecuta (solo lint/build/test unitarios), por lo que su deriva no se detecta:
`loginAsAdmin` asevera `/admin$` pero la app redirige a `/admin/employees`. Además
solo existe el usuario `admin` en el seed dev, así que no se pueden probar flujos de
empleado. El backend ya soporta todo el flujo (creación de empleado, reset, cambio de
contraseña, solicitud `PARKING`/`DESK`, aprobación/rechazo) — verificado por API.

## Goals
- Cubrir end-to-end los flujos de negocio críticos: solicitud de empleado (plaza+puesto) y resolución del admin (aprobar/rechazar).
- Probar la vista de plano en **móvil** (lista "Disponibles para solicitar" + solicitud).
- Dejar los E2E existentes en verde (arreglar la aserción obsoleta).
- No cambiar comportamiento ni contrato: solo tests + datos de desarrollo.

## Decisions
- **Empleado dev por seed** (`db/seed/dev`), no por creación vía API en cada test. *Por qué:* determinista y rápido; el seed ya es el mecanismo de la Puerta 3 (`V5` admin). El empleado nace con contraseña conocida y sin `password_must_change` (como el admin dev) para poder loguearse directo en E2E. Solo perfil `des`.
- **Proyecto Playwright móvil** con `devices['Pixel 5']` (o iPhone). *Por qué:* emular viewport + user agent táctil es la vía estándar de Playwright para probar responsive/móvil; ejercita los breakpoints `<768px` y la lista móvil añadida en R1.
- **Reset de estado entre specs**: cada spec de solicitud crea/limpia sus propias solicitudes o usa fechas distintas para no colisionar por unicidad. *Por qué:* los E2E comparten la misma BD dev; evitar dependencia de orden (misma lección que los ITs).
- **Fix mínimo del helper**: `loginAsAdmin` pasa a asertar `/admin/employees` (landing real). *Por qué:* corrige los 3 casos que fallan sin tocar la app.
- **Documentar el arranque**: los E2E requieren backend + BD + frontend levantados (Playwright `webServer` arranca el frontend y reutiliza el existente; el backend se arranca aparte). *Por qué:* el CI no los corre; se ejecutan localmente con la BD Docker.

## Risks
- **Flakiness por estado compartido de la BD dev**: solicitudes previas afectan disponibilidad. *Mitigación:* fechas dedicadas por spec y/o limpieza; aserciones que filtran por el propio empleado/fecha (no por índice de fila).
- **Dependencia del stack levantado**: si el backend no está arriba, los E2E fallan. *Mitigación:* documentar prerrequisitos en `e2e/README.md`; el orquestador levanta el stack antes de correrlos.
- **Emulación móvil ≠ dispositivo real**: pinch-zoom real no se reproduce fielmente. *Mitigación:* el E2E móvil se centra en la **lista "Solicitar"** (vía de solicitud real en móvil), no en gestos de zoom.

## Migration Plan
- **Seed dev**: nueva migración `db/seed/dev/V17__seed_dev_employee.sql` (idempotente, un `EMPLOYEE` con contraseña conocida), solo perfil `des`; sin efecto en PRE/PRO ni en los ITs (que no cargan `db/seed/dev`).
- Sin migración de esquema ni cambios de contrato.
- `playwright.config.ts`: añadir un proyecto/`devices` móvil junto al Desktop Chrome existente.
