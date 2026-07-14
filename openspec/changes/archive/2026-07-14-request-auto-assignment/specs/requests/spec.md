## MODIFIED Requirements

### Requirement: Creación de solicitud con ventana y unicidad
**El sistema DEBE (MUST) crear una solicitud para un empleado y una fecha dentro de la ventana hoy..hoy+14 días, garantizando una única solicitud `PENDING` por empleado, tipo de recurso y fecha, y DEBE ramificar el estado inicial según el parámetro global `approvalMode`: en modo `MANUAL` la solicitud nace `PENDING` (con `resource_id = NULL`); en modo `AUTOMATIC` la solicitud nace `APPROVED` con recurso asignado (plaza auto-asignada o puesto elegido).**

#### Scenario: Creación dentro de la ventana en modo MANUAL
- **GIVEN** el parámetro global `approvalMode = MANUAL` y un `Employee` autenticado con rol `EMPLOYEE` sin solicitud `PENDING` para `requested_date`
- **WHEN** envía `POST /requests` con `requested_date` entre hoy y hoy+14 días
- **THEN** el sistema responde 201 con la solicitud en estado `PENDING` y `resource_id = NULL`
- **AND** registra `created_at` con la marca temporal actual

#### Scenario: Fecha fuera de la ventana
- **GIVEN** un `Employee` autenticado con rol `EMPLOYEE`
- **WHEN** envía `POST /requests` con `requested_date` anterior a hoy o posterior a hoy+14 días
- **THEN** el sistema responde 400 con `error = OUTSIDE_REQUEST_WINDOW` y `fields` indicando `requested_date`
- **AND** no crea ninguna solicitud

#### Scenario: Solicitud PENDING duplicada para la misma fecha
- **GIVEN** un `Employee` con una solicitud `PENDING` para `requested_date` y el mismo tipo de recurso
- **WHEN** envía `POST /requests` con la misma `requested_date` y tipo
- **THEN** el sistema responde 409 con `error = REQUEST_ALREADY_PENDING`
- **AND** no crea una segunda solicitud

### Requirement: Rechazo con catálogo de motivos
**El sistema DEBE (MUST) permitir al administrador rechazar una solicitud indicando un `rejection_reason_code` del catálogo, exigiendo `rejection_reason` (≥5 caracteres) cuando el código es `OTHER`. El rechazo DEBE (MUST) admitirse tanto desde `PENDING` como desde `APPROVED` (solicitud auto-aprobada en modo `AUTOMATIC`); al rechazar una solicitud `APPROVED` el recurso asignado DEBE quedar liberado y disponible de nuevo para esa fecha.**

#### Scenario: Rechazo de solicitud PENDING con motivo del catálogo
- **GIVEN** un `Employee` con rol `ADMIN` y una solicitud en estado `PENDING`
- **WHEN** envía `POST /requests/{id}/reject` con `rejectionReasonCode = NO_AVAILABILITY`
- **THEN** el sistema responde 200 con la solicitud en estado `REJECTED`, `resolved_by_id` y `resolved_at` informados
- **AND** dispara la notificación de rechazo al empleado

#### Scenario: Rechazo OTHER sin texto libre
- **GIVEN** un `Employee` con rol `ADMIN` y una solicitud rechazable
- **WHEN** envía `POST /requests/{id}/reject` con `rejectionReasonCode = OTHER` y `rejectionReason` ausente o de menos de 5 caracteres
- **THEN** el sistema responde 400 con `error` de validación y `fields` indicando `rejectionReason`
- **AND** la solicitud no cambia de estado

#### Scenario: Rechazo posterior de una solicitud auto-aprobada libera el recurso
- **GIVEN** un `Employee` con rol `ADMIN` y una solicitud auto-aprobada en estado `APPROVED` con una plaza asignada para `requested_date`
- **WHEN** envía `POST /requests/{id}/reject` con un `rejectionReasonCode` válido
- **THEN** el sistema responde 200 con la solicitud en estado `REJECTED`, `resolved_by_id` y `resolved_at` informados
- **AND** la plaza deja de figurar como `APPROVED` y vuelve a estar disponible para `requested_date`
- **AND** registra la acción en auditoría y dispara la notificación de rechazo

## ADDED Requirements

### Requirement: Auto-asignación de plaza por categoría y planta en modo AUTOMATIC
**Cuando `approvalMode = AUTOMATIC` y un empleado solicita una PLAZA, el sistema DEBE (MUST) asignar automáticamente una plaza LIBRE para la fecha eligiéndola por la categoría del empleado y la planta de la plaza (planta = número de plaza / 1000), y la solicitud DEBE nacer en estado `APPROVED` con la plaza asignada. Las categorías altas hasta Director nivel 2 (CEO, Consejo, Director N1, Director N2) DEBEN preferir las plantas más altas disponibles (probando de la más alta a la más baja); el resto de categorías (Gerente, Mando intermedio, Empleado) DEBEN preferir las plantas más bajas (de la más baja a la más alta), cayendo a la siguiente planta según el orden de preferencia cuando la preferida no tiene plazas libres.**

#### Scenario: Categoría alta recibe la planta más alta disponible
- **GIVEN** `approvalMode = AUTOMATIC`, un `Employee` de categoría `DIRECTOR_N2` y plazas libres en las plantas 5 y 1 para `requested_date`
- **WHEN** envía `POST /requests` de tipo `PARKING`
- **THEN** el sistema responde 201 con la solicitud en estado `APPROVED` y una plaza de la planta 5 asignada

#### Scenario: Categoría base recibe la planta más baja disponible
- **GIVEN** `approvalMode = AUTOMATIC`, un `Employee` de categoría `EMPLEADO` y plazas libres en las plantas 5 y 1 para `requested_date`
- **WHEN** envía `POST /requests` de tipo `PARKING`
- **THEN** el sistema responde 201 con la solicitud en estado `APPROVED` y una plaza de la planta 1 asignada

#### Scenario: Fallback a la siguiente planta según preferencia
- **GIVEN** `approvalMode = AUTOMATIC`, un `Employee` de categoría `DIRECTOR_N1` y sin plazas libres en la planta 5 pero con plazas libres en la planta 4 para `requested_date`
- **WHEN** envía `POST /requests` de tipo `PARKING`
- **THEN** el sistema responde 201 con la solicitud en estado `APPROVED` y una plaza de la planta 4 asignada

#### Scenario: Sin ninguna plaza libre no se puede auto-asignar
- **GIVEN** `approvalMode = AUTOMATIC`, un `Employee` de cualquier categoría y ninguna plaza libre en ninguna planta para `requested_date`
- **WHEN** envía `POST /requests` de tipo `PARKING`
- **THEN** el sistema responde 409 con `error = NO_AVAILABILITY`
- **AND** no crea ninguna solicitud

#### Scenario: Concurrencia sobre la misma plaza auto-asignada
- **GIVEN** `approvalMode = AUTOMATIC` y dos empleados cuyo algoritmo selecciona simultáneamente la misma plaza para la misma `requested_date`
- **WHEN** la segunda solicitud se confirma tras la primera
- **THEN** el sistema responde 409 a la segunda (colisión detectada por el índice único filtrado `APPROVED`)
- **AND** solo la primera solicitud queda `APPROVED` con esa plaza

### Requirement: Auto-aprobación de solicitud de puesto en modo AUTOMATIC
**Cuando `approvalMode = AUTOMATIC` y un empleado solicita un PUESTO de oficina, el sistema DEBE (MUST) tomar el puesto ELEGIDO por el empleado (no hay asignación automática de puesto por planta) y crear la solicitud directamente en estado `APPROVED` con ese puesto, siempre que el puesto esté disponible para la fecha.**

#### Scenario: El puesto elegido se auto-aprueba
- **GIVEN** `approvalMode = AUTOMATIC`, un `Employee` autenticado y un puesto disponible para `requested_date`
- **WHEN** envía `POST /requests` de tipo `DESK` eligiendo ese puesto
- **THEN** el sistema responde 201 con la solicitud en estado `APPROVED` y el puesto elegido asignado

#### Scenario: El puesto elegido no está disponible
- **GIVEN** `approvalMode = AUTOMATIC`, un `Employee` autenticado y un puesto ya ocupado (`APPROVED` o reservado) para `requested_date`
- **WHEN** envía `POST /requests` de tipo `DESK` eligiendo ese puesto
- **THEN** el sistema responde 409 (sin disponibilidad)
- **AND** no crea ninguna solicitud
