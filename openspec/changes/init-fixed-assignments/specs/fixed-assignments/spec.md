# Capability: fixed-assignments

## Resumen
Asignación fija de una plaza de parking a un empleado para uno o varios días de la
semana (`day_of_week` 1-7). El `ADMIN` gestiona el alta, modificación y revocación;
el `EMPLOYEE` solo consulta las propias. La revocación es lógica (`active=false` +
`revoked_at`/`revoked_by_id`), nunca un borrado físico.

## Fase
🟢🔵 Fase 1 y Fase 2 (con la abstracción `BookableResource`, en Fase 2 se reutiliza para `desks`).

## Reglas de negocio implicadas
(README §"Glosario"/§"Reglas de negocio"; NO hay códigos RN-xx)
- Una asignación fija es un vínculo indefinido entre un empleado y un recurso para uno o varios días de la semana, vigente hasta que el admin la revoca.
- `day_of_week` toma valores 1-7 (1=Lunes … 7=Domingo).
- Una plaza no puede estar asignada a dos empleados el mismo día de la semana (entre filas activas).
- Un empleado no puede tener dos asignaciones fijas el mismo día de la semana (entre filas activas).
- La revocación es lógica: se marca `active=false`, `revoked_at` y `revoked_by_id`; la fila histórica permanece.
- Cambiar o revocar una asignación fija no afecta a días pasados ni a solicitudes ya aprobadas: aplica de cara al futuro.

## Entidades implicadas
- `FixedAssignment` (`parking_space_id`, `employee_id`, `day_of_week`, `active`, `created_by_id`, `created_at`, `revoked_by_id`, `revoked_at`)
- `Employee` (titular, `created_by`, `revoked_by`)
- `ParkingSpace` (recurso asignado)

## Endpoints
- GET /api/v1/fixed-assignments (operationId: `listFixedAssignments`) — ADMIN
- GET /api/v1/fixed-assignments/employee/{employeeId} (operationId: `getEmployeeFixedAssignments`) — ADMIN cualquiera; EMPLOYEE solo las suyas
- PUT /api/v1/fixed-assignments/employee/{employeeId} (operationId: `setEmployeeFixedAssignments`) — ADMIN
- DELETE /api/v1/fixed-assignments/employee/{employeeId} (operationId: `revokeEmployeeFixedAssignment`) — ADMIN

## Permisos
| Rol | Permisos |
|---|---|
| ADMIN | Listar todas las asignaciones fijas activas; ver, crear/modificar (PUT) y revocar la asignación de cualquier empleado |
| EMPLOYEE | Ver únicamente sus propias asignaciones fijas (verificación de pertenencia: `employeeId == session.employeeId`) |

## ADDED Requirements
### Requirement: Establecer la asignación fija de un empleado
**El sistema DEBE permitir a un `ADMIN` fijar, mediante `PUT setEmployeeFixedAssignments`, la plaza y el conjunto de días de la semana asignados a un empleado, validando los días (1-7) y devolviendo el estado resultante.**

#### Scenario: Alta de asignación fija válida
- **GIVEN** un `ADMIN` autenticado, un `Employee` activo y una `ParkingSpace` activa sin asignación previa esos días
- **WHEN** envía `PUT /fixed-assignments/employee/{employeeId}` con `{ parkingSpaceId, daysOfWeek: [1,2,3] }`
- **THEN** el sistema crea una fila `FixedAssignment active=true` por cada día con `created_by_id = session.employeeId` y `created_at = ahora`
- **AND** responde 200 con el array de asignaciones activas del empleado

#### Scenario: Día de la semana fuera de rango
- **GIVEN** un `ADMIN` autenticado
- **WHEN** envía `PUT /fixed-assignments/employee/{employeeId}` con `daysOfWeek` que incluye `8` (o `0`, o una lista vacía)
- **THEN** el sistema responde 400 con `{ error, message, fields, timestamp }` indicando el campo `daysOfWeek` inválido
- **AND** no crea ni modifica ninguna asignación

### Requirement: Garantizar la unicidad de plaza/día y empleado/día
**El sistema DEBE rechazar con 409 cualquier asignación que viole la unicidad —entre filas activas— de una plaza por día de la semana o de un empleado por día de la semana.**

#### Scenario: Plaza ya asignada a otro empleado ese día
- **GIVEN** una `ParkingSpace` con una asignación fija activa al empleado A el día 1 (Lunes)
- **WHEN** un `ADMIN` envía `PUT /fixed-assignments/employee/{B}` con `{ parkingSpaceId, daysOfWeek: [1] }`
- **THEN** el sistema responde 409 con `{ error, message, fields, timestamp }` (conflicto de plaza/día)
- **AND** no crea la asignación para el empleado B

#### Scenario: Empleado que ya tiene un recurso asignado ese día
- **GIVEN** un `Employee` con una asignación fija activa a la plaza X el día 2 (Martes)
- **WHEN** un `ADMIN` envía `PUT /fixed-assignments/employee/{mismoEmpleado}` con `{ parkingSpaceId: Y, daysOfWeek: [2] }` sin revocar la previa
- **THEN** el sistema responde 409 (conflicto de empleado/día), respetando el índice filtrado `UX_fixed_assignments_employee_day_active`
- **AND** no crea la asignación en conflicto

#### Scenario: Concurrencia sobre la misma plaza y día
- **GIVEN** dos peticiones `PUT` simultáneas de dos `ADMIN` distintos que asignan la misma plaza el día 1 a dos empleados diferentes
- **WHEN** ambas transacciones intentan persistir la fila activa
- **THEN** el sistema confirma una y la segunda recibe 409 al violar el índice único filtrado `UX_fixed_assignments_space_day_active`
- **AND** no quedan dos filas activas para la misma plaza/día

### Requirement: Revocar lógicamente la asignación fija
**El sistema DEBE revocar la asignación fija de un empleado marcando `active=false`, `revoked_at` y `revoked_by_id`, sin borrar la fila ni afectar a días pasados ni a solicitudes ya aprobadas.**

#### Scenario: Revocación válida por ADMIN
- **GIVEN** un `Employee` con una asignación fija activa y un `ADMIN` autenticado
- **WHEN** envía `DELETE /fixed-assignments/employee/{employeeId}`
- **THEN** el sistema marca las filas activas con `active=false`, `revoked_at = ahora` y `revoked_by_id = session.employeeId`
- **AND** responde 204 sin borrar físicamente la fila
- **AND** las solicitudes ya aprobadas para fechas previas permanecen intactas

#### Scenario: Revocación de empleado sin asignación activa
- **GIVEN** un `Employee` sin ninguna asignación fija activa
- **WHEN** un `ADMIN` envía `DELETE /fixed-assignments/employee/{employeeId}`
- **THEN** el sistema responde 404 (no hay asignación activa que revocar)
- **AND** no altera filas históricas ya revocadas

### Requirement: Restringir la consulta por rol y pertenencia
**El sistema DEBE permitir a cualquier `ADMIN` listar y consultar asignaciones fijas, y al `EMPLOYEE` consultar únicamente las propias, rechazando con 403 el acceso a las de otro empleado.**

#### Scenario: Empleado consulta sus propias asignaciones
- **GIVEN** un `EMPLOYEE` autenticado con asignaciones fijas activas
- **WHEN** envía `GET /fixed-assignments/employee/{suEmployeeId}`
- **THEN** el sistema responde 200 con el array de sus asignaciones activas

#### Scenario: Empleado intenta ver las asignaciones de otro
- **GIVEN** un `EMPLOYEE` autenticado
- **WHEN** envía `GET /fixed-assignments/employee/{otroEmployeeId}`
- **THEN** el sistema responde 403 (verificación de pertenencia BOLA: `employeeId != session.employeeId`)
- **AND** no revela datos de la asignación ajena

#### Scenario: Empleado intenta listar todas las asignaciones
- **GIVEN** un `EMPLOYEE` autenticado
- **WHEN** envía `GET /fixed-assignments`
- **THEN** el sistema responde 403 (operación reservada a `ADMIN`)

## Casos límite (edge cases)
- Reasignar el mismo empleado a la misma plaza y los mismos días que ya tiene activos: operación idempotente, sin crear filas duplicadas ni violar el índice filtrado.
- Quitar un día del conjunto en un `PUT` posterior: el día retirado se revoca lógicamente (`active=false` + `revoked_*`), no se borra.
- Un empleado o una plaza inactivos (`active=false`): el `PUT` responde 400/409 según el doc de validación _[verificar con docs/openapi.yaml — el contrato declara 400 y 409 pero no precisa cuál aplica a recurso inactivo]_.
- La revocación nunca afecta a una `Request APPROVED` ya emitida para una fecha futura; esa fecha sigue siendo del empleado hasta su propia cancelación/liberación (gestionada por `requests`/`releases`).
- Las filas revocadas conviven en la tabla con futuras filas activas del mismo par plaza/día gracias a los índices únicos filtrados (`WHERE active = 1`).

## Dependencias con otras capabilities
- Depende de `auth-local`/`auth-sso` para la identidad de sesión y la verificación de pertenencia (BOLA).
- Depende de `employees` (titular) y `parking-spaces` (recurso) para la integridad referencial.
- Es origen de disponibilidad para `availability-calendar` (un recurso con asignación fija ese día no está disponible salvo liberación).
- `releases` libera puntualmente un recurso con asignación fija; `requests` consulta la asignación para validar disponibilidad.
- En Fase 2, `generic-resource-refactor` generaliza `parking_space_id → resource_id`; `desks` reutiliza esta capability vía `BookableResource`.
