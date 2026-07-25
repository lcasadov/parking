## Context

Hoy la creación de una solicitud (`RequestService`) se bifurca por el **modo de aprobación global** (`system-settings`, `ApprovalMode`):
- **AUTOMÁTICO** (`createAutomatic`): auto-asigna un recurso libre por prioridad de categoría (`autoAssignedSpaceId(category, date)`); si no hay ninguno, lanza `NoAvailabilityException` → `409 NO_AVAILABILITY`.
- **MANUAL** (`createManual`): crea la solicitud `PENDING` **sin** comprobar disponibilidad; el admin la resuelve luego.

La prioridad por categoría ya existe (`HIGH_CATEGORIES`, categorías altas prefieren ciertas plantas). Existen eventos de dominio (`RequestCreatedEvent`, `RequestApprovedEvent`, `RequestCancelledEvent`, `FixedAssignmentRevokedEvent`) que disparan notificaciones `AFTER_COMMIT` vía `email_outbox`. **No** existe un evento para la **liberación de una asignación fija** por fecha. El modo de aprobación es legible por cualquier autenticado (`GET /settings/approval-mode`).

## Goals

- Que el empleado **siempre** pueda registrar interés en un día lleno, con expectativas claras.
- Que la disponibilidad sobrevenida (liberaciones, cancelaciones) **llegue** a la cola sin intervención (automático) o con un aviso accionable (manual).
- Coste de cambio bajo: reutilizar estados, prioridad y notificaciones existentes.

### Non-Goals
- Reservar/bloquear plaza para el que espera; posición numérica; first-come; expiración automática.

## Decisions

- **Reutilizar `PENDING` + flag `waitlisted`** en vez de un nuevo `RequestStatus`. La "cola" es una vista sobre las `PENDING` con `waitlisted = true` de un día+tipo; las transiciones, filtros y RBAC existentes siguen valiendo (menor blast radius que un estado nuevo).
- **Opt-in explícito `waitlist`** en `POST /requests`: preserva el contrato actual (sin opt-in, el `409` en automático se mantiene) y hace que apuntarse sea una decisión consciente del empleado.
- **En MANUAL**, `waitlisted` se computa al crear = "no había disponibilidad para ese día/tipo"; no requiere opt-in (el modo ya crea `PENDING`).
- **Orden de promoción (automático):** categoría descendente y, a igualdad, `createdAt` ascendente (FIFO). Reutiliza la prioridad de auto-asignación existente.
- **Automático auto-asigna; manual avisa al admin.** Coherente con el significado de cada modo (en automático el sistema decide; en manual decide el admin).
- **Promoción dirigida por eventos `AFTER_COMMIT`**, como el resto de notificaciones. Se centraliza en un caso de uso `promoteWaitlist(date, resourceType)` invocado desde cada origen de liberación.
- **Sin posición numérica**: con prioridad por categoría el número sería volátil y engañoso.

## Risks / Trade-offs

- **Concurrencia**: dos liberaciones o promociones simultáneas sobre el mismo día/tipo. Mitigación: promover **de uno en uno por recurso liberado** dentro de una transacción, y apoyarse en el índice único `(resource, date)` de `APPROVED` que ya impide doble ocupación (la auto-aprobación fallida se reintenta con el siguiente de la cola).
- **No-show**: al auto-asignar por categoría, el promovido podría no usar la plaza. Aceptado en MVP: puede liberar (flujo `release` ya existe). Futuro: paso de aceptar/rechazar o expiración.
- **Orígenes de liberación**: hay que cubrir todos los caminos que liberan un recurso para una fecha —cancelación de `APPROVED` por empleado (`cancel`), admin-cancel (`adminCancel`), liberación voluntaria de fija (`releases`), liberación administrativa por fecha. Riesgo de olvidar uno → se enumeran explícitamente y se engancha `promoteWaitlist` en cada uno.

## Migration Plan

- `V27__request_waitlisted.sql`: `ALTER TABLE dbo.requests ADD waitlisted BIT NOT NULL CONSTRAINT DF_requests_waitlisted DEFAULT 0;` — retro-compatible, sin backfill (las solicitudes existentes = no en espera).

## Open Questions

Resueltas con negocio: orden = categoría y luego FIFO (automático); en manual decide el admin; sin posición visible; aplica a plaza y puesto con colas separadas; sin límite de fechas.
