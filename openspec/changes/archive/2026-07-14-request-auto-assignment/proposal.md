## Why

Hoy toda solicitud (plaza o puesto) nace `PENDING` y exige que el ADMIN la resuelva una a una, incluso cuando la organización quiere agilizar la operativa y dejar que el sistema asigne recursos automáticamente. El propietario ha decidido introducir un **ajuste global** que permita conmutar entre el flujo manual actual y un flujo automático que asigne plazas por **categoría del empleado** y **planta de la plaza**, y auto-apruebe las solicitudes, reservando al ADMIN un rechazo posterior que libere el recurso.

## What Changes

- **NUEVO** parámetro **global** de sistema `approvalMode` con valores `MANUAL` | `AUTOMATIC`, configurable por el ADMIN (alcance global, un único ajuste para toda la instalación). Se introduce la capability `system-settings` con almacenamiento persistente (fila única) y endpoints admin `GET`/`PUT`.
- Modo `MANUAL`: **sin cambios** — la solicitud nace `PENDING` y el ADMIN aprueba/rechaza (comportamiento actual intacto).
- Modo `AUTOMATIC`:
  - Solicitud de **PLAZA** → **asignación automática** de una plaza LIBRE elegida por la **categoría del empleado** (dependencia `employee-category`) y la **planta** de la plaza (dependencia `parking-space-floors`; planta = `número / 1000`). La solicitud nace `APPROVED` con la plaza asignada.
  - Solicitud de **PUESTO** → el empleado ELIGE el puesto; la solicitud se **auto-aprueba** por defecto (sin asignación automática por planta).
  - El ADMIN puede **RECHAZAR después** cualquier solicitud auto-aprobada, lo que **libera** el recurso asignado (reutiliza el flujo de rechazo existente + auditoría). **BREAKING** parcial: en modo `AUTOMATIC` una solicitud puede transitar `APPROVED → REJECTED` por acción del ADMIN (antes solo se rechazaba desde `PENDING`).
- Se preservan intactas las garantías de concurrencia (índices únicos filtrados de la BD → 409) al auto-asignar/aprobar.
- **Dependencia**: este change requiere `parking-space-floors` (concepto de planta en la plaza) y `employee-category` (categoría del empleado) previos.

## Capabilities

### New Capabilities
- `system-settings`: parámetro global de sistema (`approvalMode`) con almacenamiento persistente de fila única y endpoints admin de lectura/escritura.

### Modified Capabilities
- `requests`: la creación de solicitud pasa a ramificar según `approvalMode`; en `AUTOMATIC` la plaza se auto-asigna por categoría/planta y la solicitud nace `APPROVED`, el puesto se auto-aprueba, y el ADMIN puede rechazar una solicitud ya aprobada liberando el recurso.

## Impact

- Backend: `system-settings` (nueva capability, tabla `system_settings` de fila única, endpoint admin), `request/application/RequestService` (ramificación por modo, algoritmo de auto-asignación, auto-aprobación y rechazo post-aprobación), `request/domain/Request` (transición `APPROVED → REJECTED`), integración con `availability` (plazas libres por planta) y auditoría.
- Frontend: pantalla admin de configuración del parámetro global; UX de la solicitud del empleado adaptada al modo automático (feedback de asignación inmediata / auto-aprobación).
- Migraciones: nueva `V16__system_settings.sql`; los índices únicos filtrados existentes de `requests` (V8) se mantienen sin cambios.
- Dependencias funcionales previas: `parking-space-floors`, `employee-category`.
