# Capability: visitors

## Resumen
Gestión de **fichas de visitante** (personas externas que no son empleados, sin
cuenta ni acceso a la app) y de **reservas de plaza** puntuales para esos
visitantes. Solo `ADMIN`. Una `VisitorReservation` ocupa una plaza (`ParkingSpace`)
en una fecha y la hace **no disponible** ese día. Sin email y sin flujo de aprobación.

## Fase
🟢🔵 (ambas fases)

## Reglas de negocio implicadas
(README §"Reservas para visitantes" y §"Glosario"; NO hay códigos RN-xx)
- El visitante es una persona externa que no accede a la aplicación, no tiene cuenta y no recibe emails.
- `national_id` (DNI/pasaporte) es la clave natural única para reusar la ficha entre visitas → colisión = 409.
- Una `VisitorReservation` ocupa la plaza para esa fecha en el cálculo de disponibilidad (cuenta como plaza no disponible).
- La reserva de visitante no tiene estados ni flujo de aprobación: la crea directamente el admin.
- Editar la ficha del visitante afecta **solo a futuras reservas** (no reescribe datos históricos de reservas pasadas).
- El admin solo puede anular reservas **futuras** (no las pasadas).
- Las reservas de visitante aplican **solo a plazas** (`ParkingSpace`), nunca a puestos (`Desk`).
- Cada acción del admin sobre visitantes y reservas queda registrada en `audit_log`.

## Entidades implicadas
- `Visitor` (`firstName`, `lastName`, `nationalId` único, `licensePlate`, `company`, `usualReason`, `createdById`)
- `VisitorReservation` (`visitorId`, `parkingSpaceId`, `reservationDate`, `notes`, `createdById`)
- `ParkingSpace` (plaza ocupada por la reserva)
- `Employee` (el `ADMIN` creador, `createdById`)

## Endpoints
- GET /api/v1/visitors (operationId: `listVisitors`)
- POST /api/v1/visitors (operationId: `createVisitor`)
- GET /api/v1/visitors/{id} (operationId: `getVisitor`)
- PUT /api/v1/visitors/{id} (operationId: `updateVisitor`)
- GET /api/v1/visitor-reservations (operationId: `listVisitorReservations`)
- POST /api/v1/visitor-reservations (operationId: `createVisitorReservation`)
- DELETE /api/v1/visitor-reservations/{id} (operationId: `cancelVisitorReservation`)

## Permisos
| Rol | Permisos |
|---|---|
| ADMIN | CRUD de fichas de visitante; crear/listar/anular reservas de visita |
| EMPLOYEE | Sin acceso (403) |

## ADDED Requirements
### Requirement: Gestión de fichas de visitante
**El sistema DEBE permitir al `ADMIN` crear, consultar, listar y modificar fichas de visitante, garantizando la unicidad de `nationalId`.**

#### Scenario: Crear una ficha de visitante nueva
- **GIVEN** un `ADMIN` autenticado
- **WHEN** envía `POST /visitors` con `{ firstName, lastName, nationalId }` válidos y un `nationalId` no existente
- **THEN** el sistema crea el `Visitor` con `createdById` = admin actual y responde 201 con la ficha
- **AND** registra la acción en `audit_log`

#### Scenario: Crear visitante con nationalId duplicado
- **GIVEN** un `ADMIN` autenticado y un `Visitor` existente con `nationalId = "X1234567Z"`
- **WHEN** envía `POST /visitors` con `nationalId = "X1234567Z"`
- **THEN** el sistema responde 409 con `{ error, message, fields, timestamp }` indicando colisión de `nationalId`
- **AND** no crea ninguna ficha

#### Scenario: Crear visitante con campos obligatorios ausentes
- **GIVEN** un `ADMIN` autenticado
- **WHEN** envía `POST /visitors` sin `firstName` (o sin `lastName` o sin `nationalId`)
- **THEN** el sistema responde 400 con `error` de validación y `fields` indicando los campos faltantes
- **AND** no crea ninguna ficha

#### Scenario: Editar una ficha afecta solo a futuras reservas
- **GIVEN** un `Visitor` con reservas pasadas y futuras
- **WHEN** el `ADMIN` envía `PUT /visitors/{id}` cambiando `licensePlate`
- **THEN** el sistema actualiza la ficha y responde 200
- **AND** las reservas pasadas conservan los datos con los que se crearon (el cambio solo aplica a reservas futuras)

### Requirement: Creación de reservas de visitante con control de disponibilidad
**El sistema DEBE permitir al `ADMIN` crear una reserva de plaza para un visitante en una fecha, ocupando la plaza ese día, y rechazar la reserva si la plaza ya no está disponible para esa fecha.**

#### Scenario: Crear una reserva sobre una plaza disponible
- **GIVEN** un `ADMIN` autenticado, un `Visitor` y una `ParkingSpace` activa sin ocupación para la fecha F
- **WHEN** envía `POST /visitor-reservations` con `{ visitorId, parkingSpaceId, reservationDate: F }`
- **THEN** el sistema crea la `VisitorReservation` y responde 201
- **AND** la plaza pasa a contar como **no disponible** para la fecha F en el cálculo de disponibilidad
- **AND** registra la acción en `audit_log`

#### Scenario: Crear una reserva sobre una plaza ya ocupada esa fecha
- **GIVEN** una `ParkingSpace` que ya está ocupada para la fecha F (otra reserva de visitante, una solicitud aprobada o una asignación fija no liberada)
- **WHEN** el `ADMIN` envía `POST /visitor-reservations` para esa misma plaza y fecha F
- **THEN** el sistema responde 409 con `{ error, message, fields, timestamp }` indicando indisponibilidad
- **AND** no crea la reserva

#### Scenario: Crear reserva con campos obligatorios ausentes
- **GIVEN** un `ADMIN` autenticado
- **WHEN** envía `POST /visitor-reservations` sin `parkingSpaceId` (o sin `visitorId` o sin `reservationDate`)
- **THEN** el sistema responde 400 con `error` de validación y `fields`
- **AND** no crea la reserva

### Requirement: Anulación de reservas futuras
**El sistema DEBE permitir al `ADMIN` anular únicamente reservas de visitante cuya fecha sea futura, liberando la plaza ese día.**

#### Scenario: Anular una reserva futura
- **GIVEN** una `VisitorReservation` con `reservationDate` posterior a hoy
- **WHEN** el `ADMIN` envía `DELETE /visitor-reservations/{id}`
- **THEN** el sistema anula la reserva y responde 204
- **AND** la plaza vuelve a contar como disponible para esa fecha
- **AND** registra la acción en `audit_log`

#### Scenario: Intentar anular una reserva pasada
- **GIVEN** una `VisitorReservation` con `reservationDate` anterior a hoy
- **WHEN** el `ADMIN` envía `DELETE /visitor-reservations/{id}`
- **THEN** el sistema responde 400 indicando que solo se pueden anular reservas futuras
- **AND** no modifica la reserva

### Requirement: Autorización exclusiva de ADMIN
**El sistema DEBE restringir toda operación sobre visitantes y reservas de visita al rol `ADMIN`.**

#### Scenario: Empleado sin rol ADMIN intenta listar visitantes
- **GIVEN** un `Employee` autenticado con rol `EMPLOYEE`
- **WHEN** envía `GET /visitors` (o cualquier otro endpoint de la capability)
- **THEN** el sistema responde 403 con `{ error, message, fields, timestamp }`
- **AND** no devuelve datos de visitantes

#### Scenario: Petición sin sesión
- **GIVEN** una petición sin cookie de sesión válida
- **WHEN** invoca cualquier endpoint de visitantes o reservas
- **THEN** el sistema responde 401

## Casos límite (edge cases)
- Reserva para **hoy**: cuenta como ocupación del día actual; su anulación (al ser "no futura") sigue las reglas de fecha definidas en `cancelVisitorReservation` _[verificar con README §"Reservas para visitantes" si "hoy" se considera anulable]_.
- Concurrencia: dos reservas simultáneas sobre la misma plaza y fecha → solo una persiste; la segunda recibe 409 (la unicidad de disponibilidad se valida transaccionalmente, no solo en lectura previa).
- Anular una reserva ya anulada o inexistente → 404.
- Reserva sobre una `ParkingSpace` inactiva → 409/400 (la plaza inactiva no es reservable) _[verificar con docs/openapi.yaml: no hay código específico declarado]_.
- El visitante no recibe ningún email en ninguna operación (no tiene cuenta).
- Buscar/reusar visitante existente por `nationalId`, `firstName`, `lastName` o `licensePlate` se resuelve vía filtros de `listVisitors`.

## Dependencias con otras capabilities
- Depende de `auth-local`/`auth-sso` para la autenticación y de la autorización `ADMIN`.
- Depende de `parking-spaces` (la reserva referencia una `ParkingSpace` activa).
- Alimenta `availability-calendar`: una `VisitorReservation` es una de las cuatro condiciones que marcan una plaza como no disponible para una fecha (solo plazas, nunca puestos).
- Dispara `audit-retention`: cada acción se registra en `audit_log`; las `visitor_reservations` se purgan a 2 años, las fichas `Visitor` (dato vivo) no se purgan.
- **No** dispara `notifications` (el visitante no tiene cuenta).
