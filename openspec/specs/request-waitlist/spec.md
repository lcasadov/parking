# request-waitlist Specification

## Purpose
TBD - created by archiving change waitlist-requests. Update Purpose after archive.
## Requirements
### Requirement: Solicitar un recurso aunque no haya disponibilidad (lista de espera)
El sistema DEBE (MUST) permitir a un `EMPLOYEE` registrar una solicitud para una fecha y tipo de recurso **sin disponibilidad** cuando el empleado opta explícitamente por la lista de espera (`waitlist: true`). La solicitud nace `PENDING` con `waitlisted = true`. Sin el opt-in, el comportamiento actual se mantiene (en modo AUTOMÁTICO, `409 NO_AVAILABILITY`). En modo MANUAL toda solicitud para un día sin disponibilidad DEBE (MUST) marcarse `waitlisted = true` aunque no se envíe el opt-in.

#### Scenario: Automático sin hueco con opt-in crea solicitud en espera
- **GIVEN** el modo de aprobación es AUTOMÁTICO y no hay ningún recurso libre del tipo T para la fecha F
- **WHEN** el empleado envía `POST /requests` con `{ requestedDate: F, resourceType: T, waitlist: true }`
- **THEN** el sistema crea una solicitud `PENDING` con `waitlisted = true`
- **AND** responde `200/201` (no `409`)

#### Scenario: Automático sin hueco sin opt-in mantiene el 409
- **GIVEN** el modo de aprobación es AUTOMÁTICO y no hay ningún recurso libre del tipo T para la fecha F
- **WHEN** el empleado envía `POST /requests` sin `waitlist` (o `waitlist: false`)
- **THEN** el sistema responde `409 NO_AVAILABILITY`
- **AND** no crea ninguna solicitud

#### Scenario: Automático con hueco ignora el opt-in y asigna al instante
- **GIVEN** el modo de aprobación es AUTOMÁTICO y hay al menos un recurso libre del tipo T para la fecha F
- **WHEN** el empleado envía `POST /requests` con `waitlist: true`
- **THEN** el sistema auto-asigna el recurso y la solicitud nace `APPROVED` con `waitlisted = false`

#### Scenario: Manual sin hueco marca la solicitud como en espera
- **GIVEN** el modo de aprobación es MANUAL y no hay ningún recurso libre del tipo T para la fecha F
- **WHEN** el empleado envía `POST /requests` para F y T
- **THEN** el sistema crea la solicitud `PENDING` con `waitlisted = true`

### Requirement: Promoción de la lista de espera al liberarse un recurso (modo AUTOMÁTICO)
Cuando un recurso del tipo T queda disponible para una fecha F (por cancelación de una `APPROVED` o por liberación de una asignación fija) y el modo es AUTOMÁTICO, el sistema DEBE (MUST) atender la lista de espera de (F, T) asignando el recurso liberado a la solicitud en espera de **mayor categoría** del solicitante; a **igualdad de categoría**, a la de **`createdAt` más antiguo** (FIFO). La solicitud promovida pasa a `APPROVED` con el recurso asignado y se notifica al empleado. Si no hay solicitudes en espera para (F, T), no se hace nada.

#### Scenario: Se libera una plaza y se promociona por categoría
- **GIVEN** el modo es AUTOMÁTICO, y para (F, PARKING) hay dos solicitudes en espera: E1 (categoría alta) y E2 (categoría baja)
- **WHEN** un titular fijo libera su plaza de tipo PARKING para F
- **THEN** el sistema asigna la plaza liberada a E1 (mayor categoría), que pasa a `APPROVED`
- **AND** E2 permanece `PENDING` en espera
- **AND** se notifica a E1 la asignación

#### Scenario: Empate de categoría se resuelve por orden de solicitud (FIFO)
- **GIVEN** el modo es AUTOMÁTICO, y para (F, DESK) hay dos en espera de la MISMA categoría: E1 (creada antes) y E2 (creada después)
- **WHEN** se libera un puesto para F
- **THEN** el sistema asigna el puesto a E1 (creada antes)

#### Scenario: Un solo recurso liberado promociona una sola solicitud
- **GIVEN** el modo es AUTOMÁTICO y hay tres solicitudes en espera para (F, PARKING)
- **WHEN** se libera exactamente una plaza para F
- **THEN** el sistema promociona exactamente una solicitud
- **AND** las otras dos permanecen en espera

### Requirement: Aviso al administrador al liberarse un recurso con cola (modo MANUAL)
Cuando un recurso del tipo T queda disponible para una fecha F y el modo es MANUAL, el sistema NO DEBE (MUST NOT) auto-asignar; en su lugar, si existen solicitudes en espera para (F, T), DEBE (MUST) notificar a los administradores activos que se ha liberado un recurso con N solicitudes en espera, para que resuelvan con el flujo de aprobación de pendientes ya existente.

#### Scenario: Liberación en modo manual avisa al admin
- **GIVEN** el modo es MANUAL y hay N (>0) solicitudes en espera para (F, T)
- **WHEN** se libera un recurso de tipo T para F
- **THEN** el sistema no auto-asigna
- **AND** notifica a los administradores activos que hay un recurso libre para F con N en espera

#### Scenario: Liberación sin cola no genera aviso
- **GIVEN** el modo es MANUAL y no hay solicitudes en espera para (F, T)
- **WHEN** se libera un recurso de tipo T para F
- **THEN** el sistema no envía ningún aviso de lista de espera

### Requirement: Colas de espera independientes por tipo de recurso
El sistema DEBE (MUST) mantener la lista de espera separada por tipo de recurso: la liberación de una plaza (PARKING) solo promociona o avisa a la cola de PARKING de esa fecha, y análogamente para puestos (DESK).

#### Scenario: Liberar una plaza no afecta a la cola de puestos
- **GIVEN** hay solicitudes en espera para (F, PARKING) y para (F, DESK)
- **WHEN** se libera una plaza (PARKING) para F
- **THEN** solo se atiende la cola de (F, PARKING)
- **AND** la cola de (F, DESK) permanece intacta

