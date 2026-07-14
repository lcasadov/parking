## 1. Requisitos previos

- [x] 1.1 Confirmar que la máquina de estados de `Request` ya admite `PENDING → CANCELLED` (empleado) y `APPROVED → REJECTED` (ADMIN, change `request-auto-assignment`) y que la disponibilidad se recalcula sobre filas `status = 'APPROVED'` (índice único filtrado `UX_requests_space_date_approved` vigente)
- [x] 1.2 Confirmar el reloj/zona usado por la ventana de creación (`ClockPort` + `ZoneOffset.UTC`) para derivar "hoy" de forma consistente en la guarda de fecha

## 2. Backend — dominio `Request` (TDD)

- [x] 2.1 (Red) Test de dominio: `canBeCancelledBy(today)` devuelve `true` para `PENDING` (cualquier fecha), `true` para `APPROVED` con `requested_date >= today`, `false` para `APPROVED` con `requested_date < today`, y `false` para `REJECTED`/`CANCELLED`
- [x] 2.2 (Red) Test de dominio en el borde de fecha: `APPROVED` con `requested_date` = ayer (no cancelable), = hoy (cancelable), = mañana (cancelable)
- [x] 2.3 (Green) Añadir a `Request` el predicado `canBeCancelledBy(LocalDate today)` y permitir que `cancel()` transicione a `CANCELLED` también desde `APPROVED`; conservar `resourceId` como traza del recurso liberado; `REJECTED`/`CANCELLED` siguen terminales
- [x] 2.4 (Refactor) Verificar que `PENDING → CANCELLED` mantiene su semántica actual (sin restricción de fecha) y no rompe tests existentes de cancelación

## 3. Backend — caso de uso `RequestService.cancel()` (TDD)

- [x] 3.1 (Red) Test de `cancel()`: cancelar una `PENDING` propia (cualquier fecha) → `CANCELLED` (comportamiento actual intacto)
- [x] 3.2 (Red) Test de `cancel()`: cancelar una `APPROVED` propia con `requested_date` futura → `CANCELLED`; se registra auditoría de liberación
- [x] 3.3 (Red) Test de `cancel()`: cancelar una `APPROVED` propia con `requested_date` pasada → `RequestStateException`/409; sin cambios en la solicitud
- [x] 3.4 (Red) Test de `cancel()`: cancelar una `REJECTED`/`CANCELLED` → 409; cancelar una solicitud ajena → `AccessDeniedException`/403 (BOLA intacta)
- [x] 3.5 (Green) Sustituir `requirePending(request)` por `requireCancellable(request, today)` en `RequestService.cancel()` (deriva `today` de `clock.now()`); registrar auditoría de liberación cuando el estado previo es `APPROVED`
- [x] 3.6 (Red→Green) Test de disponibilidad: tras cancelar una `APPROVED`, la plaza/puesto reaparece como disponible para `requested_date` (`GET /availability?date=F` / `isSpaceTakenForDate` devuelve `false`); la fila deja de cumplir el filtro `WHERE status='APPROVED'`

## 4. Backend — contrato / documentación

- [x] 4.1 Actualizar la documentación OpenAPI del endpoint `POST /requests/{id}/cancel` (`RequestController` + `docs/openapi.yaml`): 200 al cancelar una `APPROVED` futura; 409 cuando es `APPROVED` pasada o estado terminal; mantener 403 (ajena) y 404 (inexistente)

## 5. Frontend — "Mis solicitudes" (TDD)

- [x] 5.1 (Red) Test de `MyRequestsPage`: el botón **Cancelar** aparece en filas `PENDING` y en filas `APPROVED` con `requested_date >= hoy`; no aparece en `APPROVED` pasadas, `REJECTED` ni `CANCELLED`
- [x] 5.2 (Green) Ajustar la condición de visibilidad del botón Cancelar (hoy solo `status === 'PENDING'`) a `PENDING` o (`APPROVED` y `requested_date >= hoy`), derivando "hoy" en la zona/formato de fecha de la app
- [x] 5.3 (Green) Verificar que el `CancelRequestModal` y la invalidación de la query de "Mis solicitudes" funcionan igual al cancelar una `APPROVED` (refresco del estado a `CANCELLED` y del recurso liberado)

## 6. Verificación y cierre

- [x] 6.1 Cobertura ≥80% líneas / ≥75% branches en backend y frontend; 0 violations nuevas de Sonar; complejidad cognitiva < 15 en `requireCancellable`/`canBeCancelledBy`
- [x] 6.2 Tests E2E (Playwright): un empleado con una solicitud `APPROVED` de fecha futura la cancela desde "Mis solicitudes" y el recurso reaparece en disponibilidad para esa fecha
- [x] 6.3 `npm run lint && npm test && npm run build` y `mvn clean verify` en verde; `openspec validate cancel-approved-request` sin errores
