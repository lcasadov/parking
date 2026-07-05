# Tasks: e2e-employee-request-approval-mobile

> Cobertura E2E de flujos críticos + fix del e2e obsoleto + seed de empleado dev. Sin cambio de contrato ni backend de dominio.

## 1. Seed de empleado dev
- [x] 1.1 `backend/src/main/resources/db/seed/dev/V17__seed_dev_employee.sql`: un `EMPLOYEE` idempotente con login/contraseña conocidos (BCrypt), `password_must_change=0`, activo. Solo perfil `des`.
- [x] 1.2 Verificar que el seed no entra en las flyway locations de test (solo `db/seed/dev`, cargado por `application-des.yml`).

## 2. Fix del e2e obsoleto
- [x] 2.1 `frontend/e2e/auth-login.spec.ts`: `loginAsAdmin` asevera la landing real `/admin/employees` (no `/admin$`); los 4 casos en verde.

## 3. E2E empleado (solicitud plaza + puesto)
- [x] 3.1 Helper de login de empleado (credenciales del seed V17).
- [x] 3.2 Spec: el empleado solicita **plaza** y **puesto** para la misma fecha y ve ambas como pendientes en "Mis solicitudes".
- [x] 3.3 Aserciones order-independent (fecha dedicada; filtrar por el propio empleado/fecha, no por índice de fila).

## 4. E2E admin (aprobación + rechazo)
- [x] 4.1 Spec: el admin ve las solicitudes pendientes, **aprueba** (asignando plaza/puesto) y **rechaza** otra; verifica estados `APPROVED`/`REJECTED`.

## 5. E2E móvil (plano)
- [x] 5.1 `playwright.config.ts`: proyecto móvil con `devices['Pixel 5']` (o iPhone) junto al Desktop Chrome.
- [x] 5.2 Spec móvil: en viewport móvil, el empleado abre el plano, usa la lista "Disponibles para solicitar" y solicita un puesto; verifica feedback de éxito.

## 6. Documentación y arranque
- [x] 6.1 Actualizar `frontend/e2e/README.md` con los prerrequisitos (backend + BD + frontend) y cómo correr los proyectos (escritorio y móvil).

## 7. Quality Gate
- [x] 7.1 Ejecutar la suite E2E (escritorio + móvil) contra el stack levantado: todos los specs verdes. Los tests unitarios/lint/build siguen verdes.
