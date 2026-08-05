## Context

Existe la capacidad `employee-vehicles` (CRUD 1:N de vehículos por empleado, gestionado por `ADMIN` desde `EmployeeFormModal`, con panel genérico `VehiclesPanel`). Existe un portal de empleado con páginas self-service (`MyRequestsPage`, `MyFixedAssignmentsPage`, `MyWeekPage`…). Existe un sistema de notificaciones por eventos: `NotificationEventType` + `NotificationDispatcher` (resuelve destinatarios; a admins vía `employeeRepository.findByRoleAndActiveTrue(Role.ADMIN)`), con entrega por **email** (outbox con reintento) y **push** (módulo `push` / `WebPushSender`, capacidad `push-notifications`). Existe un patrón de **aprobar/rechazar con motivo** en `request` (`RejectRequestModal`, `RejectionReasonRequiredException`, `RejectionReasonCode`) que se reutiliza para la validación.

El diseño de pantallas de esta sección se elaboró con apoyo del agente experto UX, aterrizado a componentes reales del proyecto.

## Goals / Non-Goals

**Goals (Fase 1):**
- El empleado gestiona sus propios vehículos (alta/edición/borrado) desde el portal.
- Alta/edición por el empleado → estado `PENDING`; el empleado ve el estado y el motivo de rechazo.
- Avisar a los administradores (email + push) al enviar/modificar un vehículo.

**Goals (Fase 2, solo diseño aquí):**
- Bandeja de validación del admin: aprobar / rechazar (con motivo) + contador de pendientes.
- Avisar al empleado (email + push) de la decisión.

**Non-Goals:** acciones en lote, historial de estados, uso del estado en accesos/plano, consolidación del `licensePlate` suelto, unicidad global de matrícula.

## Decisions

### Modelo y estados
- **Un solo modelo de vehículo con `status`.** Se extiende `employee_vehicles` con `status ∈ {PENDING, APPROVED, REJECTED}`, `rejection_reason` (NULL) y auditoría mínima `reviewed_at`/`reviewed_by` (NULL). Migración **V37** (`ALTER TABLE`), que marca los vehículos existentes como `APPROVED` (ya validados de facto).
- **Origen del alta determina el estado inicial:** alta/edición **por el empleado** (self-service) → `PENDING`; alta **por el `ADMIN`** (CRUD existente) → `APPROVED`. Decisión: alta por admin = validada.
- **Cualquier edición del empleado re-valida:** un `PUT` del empleado sobre un vehículo `APPROVED` o `REJECTED` lo devuelve a `PENDING` (cambiar matrícula/datos invalida la aprobación previa). Simplicidad: no hay campos "menores" exentos (refinable si molesta en uso real).
- **Unicidad de matrícula:** se mantiene **única por empleado** (como en `employee-vehicles`). La unicidad global entre empleados queda como Open Question.

### Backend
- **Endpoints self-service acotados al principal** (`/api/v1/me/vehicles`, GET/POST/PUT/DELETE): el `employeeId` se deriva del usuario autenticado (nunca de la ruta), reutilizando el `EmployeeVehicleService` con una capa que fuerza `status=PENDING` y la pertenencia. Rol: cualquier empleado autenticado sobre **sus** vehículos.
- **Notificación a admins:** nuevo `NotificationEventType.EMPLOYEE_VEHICLE_SUBMITTED`; `NotificationDispatcher` añade un método que emite a cada admin activo (email + push) con payload mínimo (empleado, matrícula). Se dispara en alta y en edición del self-service.
- **Fase 2 (diseño):** endpoints `GET /api/v1/employee-vehicles/pending` (paginado, filtrable por estado), `POST …/{id}/approve`, `POST …/{id}/reject` (motivo obligatorio, reutiliza `RejectionReasonCode` + free-text). Eventos `EMPLOYEE_VEHICLE_APPROVED` / `EMPLOYEE_VEHICLE_REJECTED` al empleado. Concurrencia: aprobar/rechazar un vehículo ya resuelto → `409`.

### Frontend — Pantalla "Mis vehículos" (empleado, Fase 1)
- **Patrón `PageHeader`, NO `PageFrame`** (coherencia con `MyRequestsPage`/`MyFixedAssignmentsPage`, que usan `<section className="…-page">` + `PageHeader` con scroll de documento).
- **Tarjetas, no tabla** (`mv-cards`/`mv-card`, calco de `mfa-card`): el empleado tiene 1–5 vehículos y cada uno lleva **estado + posible motivo de rechazo**, que en una fila de tabla densa quedan mal y funcionan peor en móvil.
- **Estado** con las clases **ya existentes** `status-badge status-pending|status-approved|status-rejected` (ámbar/verde/rojo, contraste AA) → cero CSS nuevo para estados; textos en `vehicles.status.*`.
- **Motivo de rechazo inline** (no tooltip): `InfoBanner variant="red" icon="alert-triangle"` dentro de la tarjeta solo si `REJECTED` (`role="status"`, accesible; el empleado necesita el motivo para corregir).
- **Alta/edición:** reutiliza el modal de vehículo de `VehiclesPanel` (`Modal` icon `car`, `narrow`, grid `vehicles-form-grid`, 4 `Input`; solo matrícula obligatoria). Se añade dentro un `InfoBanner variant="amber" icon="clock"`: *"Al guardar, el vehículo quedará pendiente de validación por un administrador. Podrás usarlo cuando lo aprueben."* Al editar un `APPROVED`: aviso *"Este vehículo está aprobado. Si lo modificas, volverá a quedar pendiente de validación."*
  - Recomendación de refactor: **extraer `VehicleFormModal`** a componente propio para que lo consuman el CRUD admin (`VehiclesPanel`) y la página del empleado sin duplicar.
- **Borrado:** `ConfirmDialog` tono `red` (reutiliza `deleteBody` con `{{plate}}`). Estados vacío/carga/error con `TableEmpty`/`TableSkeleton`/`TableError`. 409 de matrícula → error inline en el modal.

### Frontend — Pantalla de validación (admin, Fase 2, diseño)
- **`PageFrame`** clonando `PendingRequestsPage`: `toolbar` con `search-box` (empleado o matrícula) + `chip-filters` `role="tablist"` (Pendientes/Aprobados/Rechazados/Todos) con `cf-dot` por estado y `cf-count` en Pendientes; `footer` con paginación (`PAGE_SIZE=20`).
- **Tabla** `table` + `table-scroll table-cards-mobile`: Empleado (`EmployeeCell` con `Avatar`), Vehículo (marca·modelo·color), Matrícula (`.mono`), Fecha de solicitud (`SortableTh field="createdAt"`, FIFO ASC por defecto), Estado (`status-badge`), Acciones.
- **Aprobar** → `ConfirmDialog` tono `green` (no hay recurso que asignar, así que no se usa `ApproveRequestModal`): *"Aprobar el vehículo {{plate}} de {{name}}. Se notificará al empleado."*
- **Rechazar** → `RejectVehicleModal` (clon de `RejectRequestModal`): `Dialog narrow` + `select` de motivo (catálogo `vehicles.review.reasonCodes`) + `textarea` (obligatorio si `OTHER`, mín. como `REJECTION_FREE_TEXT_MIN`) + `InfoBanner` de aviso de email.
- **Contador de pendientes** `VehicleReviewBadge` (clon de `PendingRequestsBadge`, `badge-red`).
- **Edge cases:** ya resuelto por otro admin → 409 → toast + refetch; vehículo borrado por el empleado → 404 → toast + refetch.

### Reutilización (inventario)
- **Tal cual:** `PageHeader` (empleado), `PageFrame` (admin), `ConfirmDialog`, `TableStates`, `InfoBanner`, `Input`/`Button`/`Avatar`/`Spinner`/`ExportMenu`/`SortableTh`/`useTableSort`; clases `status-badge status-{pending,approved,rejected}`, `.mono`, `.chip-filters/.chip-filter/.cf-dot/.cf-count`, `.search-box`, `.badge-red`, `.table-scroll.table-cards-mobile`, `.vehicles-add`, `.employee-cell*`.
- **Como plantilla:** `RejectRequestModal` → `RejectVehicleModal`; `PendingRequestsPage` → `VehicleReviewPage`; `PendingRequestsBadge` → `VehicleReviewBadge`; `mfa-card*` → `mv-card*`.
- **Nuevo mínimo:** `MyVehiclesPage`, `VehicleReviewPage` (F2), `RejectVehicleModal` (F2), `VehicleReviewBadge` (F2), CSS `.mv-card*`, i18n `vehicles.*`, hooks self-service + aprobar/rechazar.

## Risks / Trade-offs

- **`status` en el modelo de vehículo (contrato).** El tipo `Vehicle` del front y la respuesta del backend ganan `status`/`rejectionReason`; el panel admin (`VehiclesPanel`) deberá mostrar el estado también. Es el cambio de contrato más impactante; se documenta como delta MODIFIED de `employee-vehicles`.
- **Editar aprobado → pendiente.** Un empleado que corrige un dato de un vehículo aprobado pierde temporalmente la validez hasta re-aprobación. Aceptable por seguridad; refinable.
- **Push en el copy.** El copy promete "email + push". La infraestructura push existe (`push-notifications` / `WebPushSender`), pero el empleado/admin debe tener suscripción push activa; si no, el aviso push es best-effort (el email es el canal garantizado por outbox).
- **Volumen de la cola (F2).** Con muchas altas simultáneas la bandeja necesita buscador + paginación desde el día 1 (contemplado); acciones en lote quedan fuera de alcance.
- **Concurrencia de validación.** Dos admins validando el mismo vehículo: la segunda acción devuelve 409; la UI refresca.

## Open Questions (negocio)

- ¿Alta por admin nace `APPROVED`? Propuesta: **sí**.
- ¿Cualquier edición del empleado revalida (`→ PENDING`), o algún campo (p. ej. color) es "menor"? Propuesta: **cualquier edición revalida**.
- ¿Matrícula única por empleado (actual) o **global** entre empleados (evitar el mismo coche en dos fichas)? Propuesta por defecto: **por empleado**; decidir.
- ¿La aprobación admite nota opcional (subir de `ConfirmDialog` a `Dialog`) o es un simple confirmar? Propuesta: **confirmar simple**.
- ¿El portal del empleado se mantiene en `PageHeader` (coherente con "Mis…") o se migra todo a `PageFrame`? Propuesta: **mantener `PageHeader`**.

## Addendum Fase 2 (implementada) — decisiones finales

Refinamientos acordados sobre el diseño inicial de Fase 2:

- **Motivo de rechazo en texto libre** (sin catálogo de códigos): `EmployeeVehicleRejectRequest` con `@NotBlank`.
- **Nuevo estado `IN_PROGRESS` ("en trámite")**: el admin lo usa mientras gestiona (p.ej. alta con la mutua). Se puede aprobar/rechazar desde `PENDING` o `IN_PROGRESS`.
- **Borrado con `PENDING_DELETION`**: el empleado que borra un vehículo `IN_PROGRESS`/`APPROVED` no lo elimina; queda pendiente de borrado y el admin **confirma** o **restaura** (a su estado previo, guardado en `previous_status`, migración V38). Borrar un `PENDING`/`REJECTED` sí es directo.
- **Histórico de cambios** (migración V39 `employee_vehicle_history` + `EmployeeVehicleHistoryRecorder`): alta, edición (con snapshot previo), cambios de estado (con motivo) y solicitud de borrado. Consultable por el admin desde la bandeja y desde el tab de vehículos del formulario de empleado. Se borra en cascada con el vehículo (vive mientras el vehículo exista).
- **Estado visible en el tab admin**: `VehiclesPanel` gana la prop `showStatus` (columna Estado + acceso al histórico); los vehículos de visitante no la activan.
- **Notificaciones**: al empleado en trámite/aprobado/rechazado; al admin en la solicitud de borrado. Mismo notificador dedicado best-effort (email + push) de la Fase 1; integración en el outbox reintentable sigue pendiente (tarea 3.3).
- **Contador de pendientes de acción** = `PENDING` + `PENDING_DELETION` (badge en la navegación admin).
