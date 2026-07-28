## ADDED Requirements

### Requirement: Reasignación de recurso por fecha (admin)
El sistema SHALL permitir a un ADMIN cambiar el recurso (plaza o puesto) de una asignación
APPROVED de un empleado para una fecha concreta, desde el modal de asignación de Ocupación,
validando que el recurso destino esté libre esa fecha.

#### Scenario: Reasignar a un recurso libre
- **WHEN** un ADMIN reasigna la asignación de un empleado de un día al recurso destino R
- **AND** R está libre esa fecha y es compatible con el tipo del recurso original
- **THEN** la asignación pasa a R para esa fecha, el recurso anterior queda libre
- **AND** queda registrado en auditoría con el admin como actor

#### Scenario: Recurso destino ocupado
- **WHEN** un ADMIN intenta reasignar a un recurso ya ocupado esa fecha
- **THEN** la operación se rechaza con 409 y no se modifica nada

### Requirement: Intercambio (swap) de recursos entre empleados por fecha
El sistema SHALL permitir a un ADMIN intercambiar en una sola operación atómica los recursos
de dos empleados que tienen recurso asignado la misma fecha, de forma que ninguno de los dos
cambios se aplique si alguno falla.

#### Scenario: Swap correcto
- **WHEN** un ADMIN intercambia los recursos del empleado A y el empleado B para una fecha
- **AND** ambos tienen recurso asignado esa fecha
- **THEN** A queda con el recurso de B y B con el de A para esa fecha, en una transacción única

#### Scenario: Swap atómico ante fallo
- **WHEN** durante el intercambio uno de los dos lados no puede aplicarse
- **THEN** no se aplica ningún cambio (rollback) y se informa del error

### Requirement: Notificación por email a ambos afectados
El sistema SHALL enviar un email a cada uno de los dos empleados afectados por una
reasignación o intercambio, indicando el recurso resultante y la fecha. El fallo del email
no SHALL revertir la operación de datos ya confirmada.

#### Scenario: Emails tras el intercambio
- **WHEN** se completa un intercambio de recursos entre A y B para una fecha
- **THEN** A y B reciben cada uno un email con su nuevo recurso y la fecha
