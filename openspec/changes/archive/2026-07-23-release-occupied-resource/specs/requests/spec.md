# requests

## ADDED Requirements

### Requirement: Cancelación administrativa de una solicitud aprobada
**El sistema DEBE (MUST) permitir a un `ADMIN` cancelar la solicitud `APPROVED` de fecha futura de cualquier empleado mediante `POST /requests/{id}/admin-cancel` con un motivo obligatorio, transicionando la solicitud a `CANCELLED`, LIBERANDO el recurso asociado para esa fecha y registrando la acción en auditoría. La operación DEBE (MUST) rechazarse (sin efecto) si la solicitud no está `APPROVED`, si su fecha es pasada, o si el actor no es `ADMIN`.**

#### Scenario: Admin cancela una solicitud aprobada futura y libera el recurso
- **GIVEN** un `ADMIN` autenticado y una solicitud `APPROVED` de otro empleado con recurso asignado y fecha futura
- **WHEN** envía `POST /requests/{id}/admin-cancel` con un `reason` no vacío
- **THEN** el sistema responde 200 con la solicitud en estado `CANCELLED`
- **AND** el recurso queda liberado y disponible para esa fecha
- **AND** registra la acción en auditoría con el motivo y el admin actor

#### Scenario: Motivo obligatorio
- **GIVEN** un `ADMIN` autenticado y una solicitud `APPROVED` futura
- **WHEN** envía `POST /requests/{id}/admin-cancel` sin `reason` (vacío o ausente)
- **THEN** el sistema responde 400 con `fields` indicando `reason`
- **AND** no cancela la solicitud

#### Scenario: Solo se cancela una APPROVED futura
- **GIVEN** un `ADMIN` autenticado y una solicitud que no está `APPROVED` (p. ej. `PENDING`, `CANCELLED` o `REJECTED`) o cuya fecha es pasada
- **WHEN** envía `POST /requests/{id}/admin-cancel`
- **THEN** el sistema responde 409 (estado no cancelable) y no modifica la solicitud

#### Scenario: RBAC — un EMPLOYEE no puede usar admin-cancel
- **GIVEN** un `EMPLOYEE` autenticado
- **WHEN** envía `POST /requests/{id}/admin-cancel`
- **THEN** el sistema responde 403 y no modifica la solicitud
