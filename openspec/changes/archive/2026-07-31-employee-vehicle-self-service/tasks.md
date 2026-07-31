# Fase 1 — Self-service del empleado + estado + aviso al admin (alcance de implementación)

## 1. Backend — modelo y estado

- [x] 1.1 Migración `db/migration/V37__employee_vehicle_status.sql` (SQL Server, con `GO`): `ALTER TABLE employee_vehicles` añadiendo `status` NVARCHAR(20) NOT NULL DEFAULT `'PENDING'`, `rejection_reason` NVARCHAR(500) NULL, `reviewed_at` DATETIME2(3) NULL, `reviewed_by` BIGINT NULL (FK opcional a `employees(id)`); `UPDATE` de los vehículos existentes a `APPROVED`.
- [x] 1.2 Enum de dominio `VehicleStatus { PENDING, APPROVED, REJECTED }` + mapeo en la entidad `EmployeeVehicle` (`status`, `rejectionReason`, auditoría).
- [x] 1.3 Exponer `status`/`rejectionReason` en `EmployeeVehicleResponse`; el alta por `ADMIN` fija `APPROVED`.

## 2. Backend — self-service acotado al empleado

- [x] 2.1 Endpoints `/api/v1/me/vehicles` (GET lista propia, POST alta, PUT edición, DELETE borrado): `employeeId` derivado del principal autenticado (nunca de la ruta).
- [x] 2.2 Servicio self-service: alta/edición fuerzan `status=PENDING` (limpiando `rejectionReason`); pertenencia por empleado (404 si ajeno); normalización/unicidad de matrícula por empleado (409) reutilizando `EmployeeVehicleService`.
- [x] 2.3 Springdoc (@Operation/@ApiResponses) de los endpoints `/me/vehicles`.

## 3. Backend — notificación a administradores

- [x] 3.1 Notificador dedicado `EmployeeVehicleAdminNotifier` (email + push a cada `ADMIN` activo). Nota de implementación: en vez de añadir un `NotificationEventType` al outbox (payload acoplado a `RequestResponse` + switches exhaustivos), se usa un notificador self-contained sobre los puertos de bajo nivel (`EmailSenderPort` + `WebPushSender`/suscripciones), con el mismo gateado (global + preferencia del empleado). Es best-effort/inmediato (sin reintento por outbox).
- [x] 3.2 `MyVehicleService` invoca `EmployeeVehicleAdminNotifier.vehicleSubmitted(...)` en alta y edición del self-service (no en borrado); reutiliza `findByRoleAndActiveTrue(Role.ADMIN)`.
- [ ] 3.3 (Endurecimiento posterior) Integrar el aviso en el outbox reintentable de email (requiere generalizar el payload de `NotificationCommand` más allá de `RequestResponse`).

## 4. Frontend — sección "Mis vehículos" (portal)

- [x] 4.1 Tipos/api/hooks self-service (`/me/vehicles`) reutilizando la firma de `VehiclesHooks`; el modelo `Vehicle` gana `status`/`rejectionReason`.
- [x] 4.2 `MyVehiclesPage` (patrón `PageHeader` como `MyRequestsPage`; tarjetas `mv-card` calco de `mfa-card`; `status-badge status-{pending,approved,rejected}` reutilizado; motivo de rechazo con `InfoBanner` rojo). Ruta + entrada de navegación en el portal del empleado.
- [x] 4.3 Extraer `VehicleFormModal` a componente propio y reutilizarlo (admin `VehiclesPanel` + empleado); añadir el `InfoBanner` de aviso de validación (alta y, al editar un aprobado, aviso de re-validación). Borrado con `ConfirmDialog`.
- [x] 4.4 i18n (es/en): `vehicles.mine.*` (título, eyebrow, descripción, aviso de validación, toast, estados vacío/carga/error) + `vehicles.status.{PENDING,APPROVED,REJECTED}` + adaptación del 409 ("ya tienes un vehículo con esa matrícula").

## 5. Tests y Quality Gate (Fase 1)

- [x] 5.1 Backend: self-service (alta propia → `PENDING` 201, edición → vuelve a `PENDING` 200, borrado 204, vehículo ajeno 404, matrícula vacía 400, duplicada 409); alta por admin → `APPROVED`; migración marca existentes `APPROVED`.
- [x] 5.2 Backend: se emite `EMPLOYEE_VEHICLE_SUBMITTED` a los admins en alta y edición (y no en borrado).
- [x] 5.3 Frontend: render de "Mis vehículos" con estados y motivo de rechazo; alta/edición → aviso de validación; borrado con confirmación; invalidación de la lista.
- [x] 5.4 Verificación: backend — módulos afectados verdes (`MyVehicleControllerTest` 8, `MyVehicleServiceTest` 4, `EmployeeVehicleServiceTest` 13, `EmployeeVehicleControllerTest` 13) + compila; front — `tsc` + `npm run build` OK, `eslint` de los ficheros de la feature 0, tests de vehículos 20/20. Endpoints `/me/vehicles` documentados vía springdoc y montados en vivo (401 sin auth). Pendiente (no bloqueante): `mvn verify` completo (arrastra 2 fallos pre-existentes ajenos) y `npm test` completo (rojos pre-existentes de refactors de UI de la sesión).

---

# Fase 2 — Pantalla de validación del admin (solo diseñada en este change; implementación en un change de continuación)

## 6. Backend — estados, validación e histórico (Fase 2)

- [x] 6.1 Estados ampliados `IN_PROGRESS` + `PENDING_DELETION` (migración V38 `previous_status`); transiciones en la entidad (`markInProgress`/`requestDeletion`/`restore`/`isReviewable`). Borrado self-service: `PENDING`/`REJECTED` → real; `IN_PROGRESS`/`APPROVED` → `PENDING_DELETION` + aviso al admin.
- [x] 6.2 `EmployeeVehicleReviewController`/`Service`: `GET /employee-vehicles` (paginado + datos del empleado, filtrable por estado), `GET /pending-count`, `GET /{id}/history`, `POST /{id}/in-progress|approve|reject|confirm-deletion|restore`; reservado a `ADMIN`; concurrencia → 409 (`VehicleReviewConflictException`), 404. Motivo de rechazo texto libre (`EmployeeVehicleRejectRequest`, `@NotBlank`).
- [x] 6.3 Histórico: migración V39 `employee_vehicle_history` + entidad/repo + `EmployeeVehicleHistoryRecorder` (alta, edición con snapshot previo, cambios de estado con motivo, solicitud de borrado); cableado en self-service y review.
- [x] 6.4 Avisos al empleado (en trámite / aprobado / rechazado con motivo) y al admin (solicitud de borrado) vía `EmployeeVehicleAdminNotifier` (email + push, best-effort).

## 7. Frontend — bandeja de validación + histórico (Fase 2)

- [x] 7.1 `VehicleReviewPage` (`PageFrame` + `chip-filters` por estado + búsqueda + tabla con datos del empleado + paginación); acciones por estado (en trámite / aprobar / rechazar / restaurar / confirmar borrado / histórico).
- [x] 7.2 `RejectVehicleModal` (texto libre), `VehicleHistoryModal` (timeline), `VehicleReviewBadge` (contador `PENDING`+`PENDING_DELETION`); ruta `/admin/vehicles` + nav admin; tipos/api/hooks.
- [x] 7.3 Columna Estado + acceso al histórico en el tab admin (`VehiclesPanel` con prop `showStatus`; visitantes no). i18n `vehicles.review.*` + estados `IN_PROGRESS`/`PENDING_DELETION`; edge cases 409/404 → toast.

## 8. Tests y Quality Gate (Fase 2)

- [x] 8.1 Backend: `EmployeeVehicleReviewServiceTest` (aprobar/rechazar/en trámite/confirmar borrado/restaurar + conflictos 409 + contador + listado con datos del empleado), `EmployeeVehicleReviewControllerTest` (RBAC, 400 motivo ausente, 409, 404, 204), y borrado self-service real vs pendiente (`MyVehicleServiceTest`, `EmployeeVehicleServiceTest`).
- [x] 8.2 Frontend: `VehicleReviewPage.test` (render, aprobar, rechazar con motivo obligatorio, acciones de pendiente de borrado); regresión de los tests de vehículos existentes.
- [x] 8.3 Verificación: backend módulos de vehículos 62/62 + compila; front `tsc` + `npm run build` OK, `eslint` de la feature 0, tests de vehículos verdes. Endpoints `/employee-vehicles` documentados vía springdoc y montados en vivo (401 sin auth).
