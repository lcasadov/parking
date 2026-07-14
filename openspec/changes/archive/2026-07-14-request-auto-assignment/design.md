## Context

El flujo actual de solicitudes es **hexagonal** y siempre manual: `RequestService.create()` da de alta una solicitud `PENDING` sin recurso, y el ADMIN la resuelve con `approve(parkingSpaceId, ...)` / `reject(...)`. La máquina de estados del dominio (`Request`) solo permite resolver desde `PENDING` (invariante comprobada por el caso de uso vía `isPending()`). La concurrencia entre administradores está resuelta en la BD por **índices únicos filtrados** de SQL Server (`UX_requests_space_date_approved` sobre `(parking_space_id, requested_date) WHERE status='APPROVED'`), que convierten la segunda aprobación de la misma plaza/fecha en una `DataIntegrityViolation → 409`.

Este change introduce un **modo automático** conmutado por un ajuste **global**. Depende de dos changes previos:
- `parking-space-floors`: añade el concepto de **planta** a la plaza. La planta se deriva del número de plaza como `planta = número / 1000` (división entera): p. ej. `5012 → planta 5`, `1007 → planta 1`.
- `employee-category`: añade la **categoría** al empleado (CEO, Consejo, Director N1, Director N2, Gerente, Mando intermedio, Empleado).

**Stakeholders**: propietario (define el modo global), ADMIN (configura el parámetro y conserva el rechazo posterior), empleado (recibe asignación/aprobación inmediata).

## Goals / Non-Goals

**Goals:**
- Un único parámetro **global** `approvalMode` (`MANUAL` | `AUTOMATIC`) persistente y editable por el ADMIN.
- En `AUTOMATIC`: auto-asignar plaza libre por categoría/planta y auto-aprobar; auto-aprobar el puesto elegido por el empleado.
- Permitir al ADMIN rechazar una solicitud auto-aprobada, liberando el recurso, con registro en auditoría.
- No romper las garantías de concurrencia existentes (índices únicos filtrados → 409).

**Non-Goals:**
- No se asigna puesto automáticamente por planta (el empleado siempre elige el puesto).
- No se introduce configuración por-departamento ni por-empleado: el alcance es estrictamente global (un único ajuste).
- No se modifica la ventana de solicitud (hoy..hoy+14) ni la unicidad `PENDING` por empleado/tipo/fecha.
- No se rediseña el motor de disponibilidad (`AvailabilityService`); se reutiliza tal cual.

## Decisions

### D1 — Almacenamiento del parámetro global: tabla `system_settings` de fila única

Se crea una tabla `dbo.system_settings` con una **única fila** (patrón singleton), garantizada por una PK constante y un `CHECK`:

```sql
CREATE TABLE dbo.system_settings (
    id            TINYINT      NOT NULL CONSTRAINT PK_system_settings PRIMARY KEY,
    approval_mode VARCHAR(10)  NOT NULL CONSTRAINT DF_system_settings_mode DEFAULT 'MANUAL',
    updated_by_id BIGINT       NULL,
    updated_at    DATETIME2(3) NOT NULL CONSTRAINT DF_system_settings_updated DEFAULT SYSUTCDATETIME(),
    CONSTRAINT CK_system_settings_singleton CHECK (id = 1),
    CONSTRAINT CK_system_settings_mode CHECK (approval_mode IN ('MANUAL','AUTOMATIC')),
    CONSTRAINT FK_system_settings_updated_by FOREIGN KEY (updated_by_id) REFERENCES dbo.employees(id)
);
-- Seed de la fila única con el modo por defecto MANUAL (retrocompatible)
INSERT INTO dbo.system_settings (id, approval_mode) VALUES (1, 'MANUAL');
```

- **Endpoints admin**: `GET /admin/settings` (lee el modo) y `PUT /admin/settings` (`{ approvalMode }`), ambos restringidos a `ADMIN` (403 para `EMPLOYEE`).
- **Alternativa descartada** — propiedad en `application.yml`: no editable en caliente por el ADMIN sin redeploy; una fila en BD permite cambiar el modo en tiempo de ejecución y auditar quién lo cambió.
- **Alternativa descartada** — tabla clave/valor genérica: sobre-ingeniería para un único ajuste; el esquema tipado documenta mejor el dominio.
- El servicio expone `SystemSettingsService.approvalMode()` (cacheable/consulta directa a la fila 1) que `RequestService` consulta al crear.

### D2 — Ramificación en `RequestService.create()` según el modo

`create()` lee `approvalMode` al inicio. En `MANUAL` el flujo es idéntico al actual. En `AUTOMATIC` bifurca por tipo de recurso:

```
create(login, req):
    mode = systemSettings.approvalMode()
    validar ventana + unicidad PENDING (igual que hoy)
    if mode == MANUAL:
        guardar Request.create(...)  # PENDING, comportamiento actual
        publicar RequestCreatedEvent
        return
    # mode == AUTOMATIC
    if req.type == DESK:
        # el empleado ELIGE el puesto (resourceId presente en la creación por-recurso)
        request = Request.createForResource(emp, DESK, req.deskId, date, now)
        request.approve(req.deskId, actorId=SYSTEM/emp, note="auto", now)  # nace APPROVED
        verificar disponibilidad + guardar (índice único filtrado protege concurrencia)
        publicar RequestApprovedEvent
    else:  # PARKING
        space = autoAssignParkingSpace(emp.category, date)   # ver D3
        if space == null: -> resultado sin plaza (ver D4)
        request = Request.createForResource(emp, PARKING, space.id, date, now)
        request.approve(space.id, actorId, note="auto", now)  # nace APPROVED
        guardar (índice único filtrado protege concurrencia -> 409 si colisión)
        publicar RequestApprovedEvent
```

- **Nota**: para la plaza, la solicitud se persiste directamente en `APPROVED` con la plaza asignada (no pasa por un `PENDING` intermedio). Esto es consistente con `createForResource` + `approve` en la misma transacción.

### D3 — Algoritmo de auto-asignación de PLAZA por categoría y planta

Dos grupos de preferencia de planta (planta = `número_plaza / 1000`):

- **Grupo ALTO** (categorías **hasta Director N2**: CEO, Consejo, Director N1, Director N2) → prefiere las **plantas más altas**: probar 5 → 4 → 3 → 2 → 1.
- **Grupo BASE** (resto: Gerente, Mando intermedio, Empleado) → prefiere las **plantas más bajas**: probar 1 → 2 → 3 → 4 → 5.

Si en la planta preferida no hay plaza libre, se cae a la siguiente según el orden del grupo. Se recorre el **espacio completo de plantas** (fallback total), de modo que si existe **cualquier** plaza libre para la fecha, se asigna; el grupo solo determina el **orden de preferencia**, no restringe el acceso.

```
autoAssignParkingSpace(category, date):
    floorsInPreferenceOrder =
        isHighCategory(category)          # CEO..Director N2
            ? [maxFloor .. minFloor]      # descendente: 5,4,3,2,1
            : [minFloor .. maxFloor]      # ascendente:  1,2,3,4,5
    for floor in floorsInPreferenceOrder:
        candidates = availabilityService.freeParkingSpacesOnFloor(floor, date)  # plazas LIBRES esa fecha
        if candidates no vacío:
            return candidates.first()     # orden estable (p. ej. por label/número asc)
    return null                            # ninguna plaza libre en ninguna planta

isHighCategory(category):
    return category in { CEO, CONSEJO, DIRECTOR_N1, DIRECTOR_N2 }
```

- La consulta de **plazas libres por planta** reutiliza la regla de disponibilidad consolidada (`AvailabilityService`: activa, sin asignación fija vigente / liberada, sin solicitud `APPROVED`, sin reserva de visitante), filtrando por planta = `número / 1000`.
- `maxFloor`/`minFloor` se derivan del rango real de plantas existentes (no se hardcodea 5); el ejemplo 5→1 es ilustrativo del dato actual.
- Orden estable dentro de una planta (p. ej. por `label`/número ascendente) para que la asignación sea determinista y testeable.

### D4 — Resultado cuando no hay ninguna plaza libre (modo AUTOMATIC)

Si `autoAssignParkingSpace` devuelve `null` (no hay **ninguna** plaza libre en ninguna planta para la fecha), la solicitud **no puede auto-asignarse**. Decisión: **responder error `409 NO_AVAILABILITY`** y **no crear la solicitud** (no queda un `PENDING` colgado).

- **Rationale**: en modo automático la promesa es "asignación inmediata"; dejar un `PENDING` silencioso confundiría al empleado (que no espera intervención del ADMIN en este modo) y crearía una cola invisible. Un 409 explícito comunica que no hay recurso y el empleado puede reintentar otra fecha.
- **Alternativa descartada** — crear `PENDING` como fallback: mezcla ambos modos y reintroduce trabajo manual del ADMIN que el modo automático pretende eliminar; se descarta para mantener la semántica limpia del ajuste global.
- Para PUESTO no aplica este caso: el empleado elige un puesto concreto; si ese puesto no está disponible se responde `409` por la vía de disponibilidad ya existente.

### D5 — Rechazo posterior de una solicitud auto-aprobada (libera el recurso)

En modo `AUTOMATIC` las solicitudes nacen `APPROVED`. El ADMIN debe poder rechazarlas después por cualquier motivo. Se amplía la máquina de estados para permitir `APPROVED → REJECTED` por acción del ADMIN:

- `RequestService.reject(id, body, adminLogin)` deja de exigir `PENDING`: acepta rechazar desde `PENDING` **o** desde `APPROVED`. El dominio `Request.reject(...)` limpia `resourceId` (o el flujo de rechazo lo trata como liberación) de modo que el recurso queda **libre** para esa fecha.
- Al pasar de `APPROVED` a `REJECTED`, la fila deja de cumplir el filtro `WHERE status='APPROVED'` del índice único `UX_requests_space_date_approved`, por lo que la plaza/puesto vuelve a estar **disponible** automáticamente (la disponibilidad se recalcula sobre solicitudes `APPROVED`). No hace falta borrado físico: basta el cambio de estado.
- Se reutiliza el **evento de rechazo existente** (`RequestRejectedEvent`) para la notificación, y se registra la acción en **auditoría** (motivo del catálogo / texto libre).
- La cancelación por el empleado (`cancel()`) permanece restringida a `PENDING` (sin cambios): el empleado no cancela una plaza ya asignada; eso es competencia del ADMIN vía rechazo/liberación.

### D6 — Interacción con la concurrencia (índices únicos filtrados intactos)

La auto-asignación + auto-aprobación ocurren en **una única transacción** que persiste la fila directamente en `APPROVED`. La red de concurrencia no cambia:

- Dos empleados solicitando simultáneamente y cuyo algoritmo elige la **misma** plaza/fecha: la segunda inserción `APPROVED` viola `UX_requests_space_date_approved` → `DataIntegrityViolation` → **409**. El primer solicitante se queda con la plaza; el segundo recibe 409 (o, opcionalmente en una iteración futura, reintento con la siguiente candidata — fuera de alcance aquí).
- La comprobación previa de disponibilidad (`AvailabilityService.isSpaceTakenForDate`) da un mensaje claro en el caso no concurrente, pero el índice filtrado sigue siendo la **red dura** bajo carrera. No se relaja ni elimina ningún índice de la V8.

## Risks / Trade-offs

- **[Carrera en auto-asignación → 409 al empleado]** → El índice único filtrado garantiza consistencia; el 409 es correcto aunque menos amable que un reintento automático. Mitigación mínima: mensaje claro; reintento automático con siguiente candidata queda como mejora futura (out of scope).
- **[`APPROVED → REJECTED` amplía la máquina de estados]** → Riesgo de permitir rechazar solicitudes ya resueltas indebidamente. Mitigación: la transición extra se restringe al ADMIN y solo desde `APPROVED`/`PENDING`; los estados terminales `REJECTED`/`CANCELLED` siguen sin admitir transición.
- **[Fila única de settings mal inicializada]** → Migración siembra la fila `id=1` con `MANUAL`; el `CHECK (id=1)` impide filas adicionales. El servicio falla ruidosamente si la fila no existe (no asume defaults silenciosos).
- **[Dependencia de `parking-space-floors`/`employee-category` no aplicadas]** → El algoritmo requiere planta y categoría; si esos changes no están, este change no puede implementarse. Mitigación: declarado como dependencia explícita en el proposal; las tareas asumen su presencia.
- **[Cambio de modo en caliente]** → Cambiar `MANUAL↔AUTOMATIC` no re-procesa solicitudes ya existentes; solo afecta a las nuevas. Aceptable y documentado en la UX admin.

## Migration Plan

1. Aplicar `parking-space-floors` y `employee-category` (dependencias).
2. Migración `V16__system_settings.sql`: crea la tabla de fila única y siembra `id=1, approval_mode='MANUAL'` (comportamiento retrocompatible: sin cambio de conducta hasta que el ADMIN conmute a `AUTOMATIC`).
3. Backend: capability `system-settings` + ramificación en `RequestService` + ampliación de `reject` + algoritmo de auto-asignación.
4. Frontend: pantalla admin del parámetro + UX de solicitud en modo automático.
5. **Rollback**: poner `approval_mode='MANUAL'` restaura el comportamiento actual sin desplegar código; revertir la migración elimina la tabla si se retira la feature por completo.

## Open Questions

- ¿El `resolved_by_id`/actor de una solicitud auto-aprobada debe ser el propio empleado, un usuario "SYSTEM" sintético, o `NULL` con una marca `auto`? (Propuesta: marcar `auto` y dejar `resolved_by_id = NULL`; a confirmar con auditoría.)
- ¿Se desea, en una iteración futura, reintento automático con la siguiente plaza candidata ante 409 de concurrencia? (Fuera de alcance de este change.)
