# admin-release-by-employee-week

## Why
El flujo "Nueva liberación" (admin y agencia) obliga a elegir un empleado y luego una plaza
de un desplegable genérico, sin ver qué recursos tiene realmente ese empleado ni permitir puesto
de forma clara ni liberar varias fechas de una vez. Además, el rol AGENCIA no puede listar
empleados (el `EmployeeController` es ADMIN-only), por lo que el selector aparece vacío.

## What Changes
- Backend: nuevos endpoints (ADMIN y AGENCIA) para (a) listar empleados seleccionables para
  liberación y (b) obtener la **ocupación de un empleado por semana** (plaza y puesto, origen
  asignación fija o solicitud aprobada, con `requestId` cuando aplique). Se permite a AGENCIA usar
  también `POST /requests/{id}/admin-cancel`.
- Frontend: rediseñar "Nueva liberación" (admin y agencia): seleccionar empleado → ver sus reservas
  por semana → marcar una o varias → liberar el lote con un único motivo; cada reserva se libera por
  su mecanismo (fija → liberación administrativa; solicitud → admin-cancel).

## Impact
- Affected specs: `releases` (nuevos endpoints de selección y ocupación semanal), `requests`
  (admin-cancel accesible también a AGENCIA).
- Affected code backend: `release`/`availability` (endpoints + RBAC), `request` (RBAC admin-cancel).
- Affected code frontend: pantalla/modal de liberación administrativa (admin + agencia).

## Out of scope
- Dar a AGENCIA acceso al resto del panel admin (solo este flujo).
- Un endpoint de liberación en lote transaccional (el frontend libera cada reserva seleccionada
  por su endpoint con el mismo motivo).
