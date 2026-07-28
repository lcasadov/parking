## MODIFIED Requirements

### Requirement: Creación de reservas de visitante con control de disponibilidad
**El sistema DEBE (MUST) permitir al `ADMIN` crear una reserva de un recurso genérico (plaza o puesto, `resourceType`/`resourceId`) para un visitante en una fecha, ocupando ese recurso ese día, y rechazar la reserva si el recurso ya no está disponible para esa fecha. La reserva NUNCA notifica por email al visitante.**

#### Scenario: Crear una reserva sobre una plaza disponible
- **GIVEN** un `ADMIN` autenticado, un `Visitor` y una `ParkingSpace` activa sin ocupación para la fecha F
- **WHEN** envía `POST /visitor-reservations` con `{ visitorId, resourceType: "PARKING", resourceId, reservationDate: F }`
- **THEN** el sistema crea la `VisitorReservation` y responde 201
- **AND** la plaza pasa a contar como **no disponible** para la fecha F en el cálculo de disponibilidad
- **AND** registra la acción en `audit_log`
- **AND** no se envía ningún email (a diferencia de la reserva de un empleado)

#### Scenario: Crear una reserva sobre un puesto disponible
- **GIVEN** un `ADMIN` autenticado, un `Visitor` y un `Desk` activo sin ocupación para la fecha F
- **WHEN** envía `POST /visitor-reservations` con `{ visitorId, resourceType: "DESK", resourceId, reservationDate: F }`
- **THEN** el sistema crea la `VisitorReservation` y responde 201
- **AND** el puesto pasa a contar como **no disponible** para la fecha F en el cálculo de disponibilidad
- **AND** registra la acción en `audit_log`

#### Scenario: Crear una reserva sobre un recurso ya ocupado esa fecha
- **GIVEN** un recurso (plaza o puesto) que ya está ocupado para la fecha F (otra reserva de visitante, una solicitud aprobada o una asignación fija no liberada)
- **WHEN** el `ADMIN` envía `POST /visitor-reservations` para ese mismo recurso y fecha F
- **THEN** el sistema responde 409 con `{ error, message, fields, timestamp }` indicando indisponibilidad
- **AND** no crea la reserva

#### Scenario: Crear una reserva sobre un recurso inexistente
- **GIVEN** un `ADMIN` autenticado
- **WHEN** envía `POST /visitor-reservations` con un `resourceId` que no corresponde a ninguna plaza/puesto del `resourceType` indicado
- **THEN** el sistema responde 404
- **AND** no crea la reserva

#### Scenario: Crear reserva con campos obligatorios ausentes
- **GIVEN** un `ADMIN` autenticado
- **WHEN** envía `POST /visitor-reservations` sin `resourceType` (o sin `resourceId`, `visitorId` o `reservationDate`)
- **THEN** el sistema responde 400 con `error` de validación y `fields`
- **AND** no crea la reserva

#### Scenario: La reserva se crea desde el asistente de reserva unificado
- **GIVEN** un `ADMIN` en el asistente de reserva con el conmutador de beneficiario en "Visitante"
- **WHEN** elige un visitante, una o varias fechas y un recurso concreto (sin auto-asignación por categoría, ya que el visitante no tiene categoría) y confirma
- **THEN** el sistema crea una `VisitorReservation` por cada fecha mediante `POST /visitor-reservations`
- **AND** el resumen previo a confirmar advierte explícitamente que no se enviará ningún email
- **AND** el resultado final identifica al visitante como beneficiario, sin mencionar ninguna notificación

## ADDED Requirements

### Requirement: Entrada a la reserva de visita desde Visitantes
**El sistema DEBE (MUST) ofrecer la creación de una reserva de visita desde la pantalla de Visitantes, abriendo el asistente de reserva unificado preseleccionado en el beneficiario "Visitante".**

#### Scenario: "Nueva reserva" desde Reservas futuras
- **GIVEN** un `ADMIN` en la sub-vista "Reservas futuras" de Visitantes
- **WHEN** pulsa "Nueva reserva"
- **THEN** se abre el asistente de reserva unificado con el beneficiario ya fijado en "Visitante"
- **AND** el `ADMIN` debe elegir el visitante concreto en el paso de beneficiario

#### Scenario: "Reservar" desde una ficha de visitante
- **GIVEN** un `ADMIN` en la sub-vista "Fichas" de Visitantes, viendo la fila de un `Visitor` concreto
- **WHEN** pulsa "Reservar" en esa fila
- **THEN** se abre el asistente de reserva unificado con el beneficiario fijado en "Visitante" y ese `Visitor` ya preseleccionado
