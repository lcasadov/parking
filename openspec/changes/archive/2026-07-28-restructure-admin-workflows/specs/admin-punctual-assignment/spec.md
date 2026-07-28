## ADDED Requirements

### Requirement: Asignación puntual de un recurso a un empleado para una fecha
El sistema DEBE (MUST) permitir a un `ADMIN` asignar un recurso reservable (plaza o puesto) a un empleado para una **fecha concreta**, creando una asignación que nace directamente en estado `APPROVED`, sin requerir que el empleado la solicite previamente. La operación DEBE (MUST) reutilizar la entidad `Request` (con `resolved_by_id` = admin actuante) para que la asignación aparezca en el calendario admin, en "Mi Semana" del empleado, en las exportaciones y en la auditoría.

#### Scenario: Admin asigna una plaza concreta a un empleado para una fecha
- **GIVEN** un `ADMIN` autenticado, un `Employee` activo y una plaza disponible para la fecha F
- **WHEN** envía la asignación puntual con `{ employeeId, requestedDate: F, resourceType: PARKING, resourceId }`
- **THEN** el sistema responde 201 con una `Request` en estado `APPROVED`, con `resolved_by_id` = admin y el recurso ocupado esa fecha
- **AND** la asignación queda visible para el empleado en "Mi Semana" y "Mis solicitudes"

#### Scenario: Admin asigna sin elegir recurso y el sistema auto-asigna
- **GIVEN** un `ADMIN` autenticado y al menos una plaza libre para la fecha F
- **WHEN** envía la asignación puntual con `{ employeeId, requestedDate: F, resourceType: PARKING }` sin `resourceId`
- **THEN** el sistema auto-asigna una plaza libre por categoría/planta y responde 201 con la `Request` en `APPROVED`

#### Scenario: Un EMPLOYEE no puede usar la asignación puntual
- **GIVEN** un usuario con rol `EMPLOYEE` autenticado
- **WHEN** intenta invocar el endpoint de asignación puntual del admin
- **THEN** el sistema responde 403 y no crea ninguna asignación

### Requirement: Validación de disponibilidad y concurrencia en la asignación puntual
El sistema DEBE (MUST) validar que el recurso está disponible para la fecha antes de crear la asignación puntual, respondiendo 409 cuando no lo esté, y NO DEBE (MUST NOT) crear la asignación en ese caso.

#### Scenario: Recurso ya ocupado esa fecha
- **GIVEN** un `ADMIN` autenticado y un recurso ya ocupado (asignación fija no liberada, solicitud aprobada o reserva de visitante) para la fecha F
- **WHEN** intenta asignarlo puntualmente a un empleado para F
- **THEN** el sistema responde 409 y no crea la asignación

#### Scenario: Sin disponibilidad en auto-asignación
- **GIVEN** un `ADMIN` autenticado y ninguna plaza libre para la fecha F
- **WHEN** envía la asignación puntual de PARKING sin `resourceId`
- **THEN** el sistema responde 409 con `error = NO_AVAILABILITY` y no crea la asignación

### Requirement: Traza de auditoría de la asignación puntual
El sistema DEBE (MUST) registrar en `audit_log` la asignación puntual con el admin actuante como actor y el empleado y recurso afectados.

#### Scenario: La asignación puntual queda auditada
- **GIVEN** un `ADMIN` que realiza una asignación puntual con éxito
- **WHEN** la operación se completa (201)
- **THEN** existe una entrada en `audit_log` con el admin como actor, la acción de asignación puntual, el empleado destino y el recurso/fecha asignados

### Requirement: Notificación por email al empleado asignado
El sistema DEBE (MUST) notificar por email al empleado destino cuando el admin le asigna un recurso puntualmente, usando una **plantilla propia** (distinta de la de "solicitud aprobada", ya que el empleado no inició la petición). El envío DEBE (MUST) ser resiliente: un fallo de correo NO DEBE (MUST NOT) revertir ni impedir la asignación (se encola en `email_outbox` como el resto de notificaciones). No hay límite de ventana temporal para la fecha asignada (solo se rechazan fechas pasadas), y la capacidad queda reservada al rol `ADMIN`.

#### Scenario: El empleado recibe el aviso de asignación puntual
- **GIVEN** un `ADMIN` que asigna con éxito un recurso a un empleado con email para una fecha
- **WHEN** la operación se completa (201)
- **THEN** se encola una notificación de email al empleado con la plantilla de asignación puntual, indicando el recurso y la fecha

#### Scenario: Un fallo de correo no revierte la asignación
- **GIVEN** una asignación puntual creada con éxito y un fallo en el envío del email
- **WHEN** se intenta notificar
- **THEN** la asignación permanece `APPROVED` y el fallo se gestiona como en el resto de notificaciones (sin propagar error al 201)
