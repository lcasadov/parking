## ADDED Requirements

### Requirement: Preferencia por el recurso fijo propio en la auto-asignación
Al crear una solicitud en modo automático sin recurso indicado, el sistema SHALL asignar el
recurso FIJO del propio empleado para ese día de la semana si existe y está libre esa fecha,
antes de aplicar la asignación por categoría. Aplica a plaza (PARKING) y puesto (DESK).

#### Scenario: Recupera su fijo tras liberarlo
- **WHEN** un empleado con puesto fijo P para los martes libera P un martes y vuelve a reservar puesto ese día
- **AND** P sigue libre esa fecha
- **THEN** se le reasigna P (su fijo), no otro puesto de su categoría

#### Scenario: Su fijo ya no está libre
- **WHEN** el fijo del empleado ese día ya lo ocupa otra persona
- **THEN** se cae a la auto-asignación por categoría actual

#### Scenario: Sin fijo ese día
- **WHEN** el empleado no tiene asignación fija ese día de la semana
- **THEN** se aplica la auto-asignación por categoría (comportamiento actual)

### Requirement: Filtro de "mis solicitudes" por rango de fechas
El endpoint de "mis solicitudes" SHALL aceptar un rango de fechas (desde/hasta) sobre la
fecha de recurso (`requestedDate`) para acotar el listado a un mes, manteniendo la paginación.

#### Scenario: Listar un mes concreto
- **WHEN** el cliente solicita sus solicitudes con un rango que cubre un mes
- **THEN** solo se devuelven las solicitudes cuya `requestedDate` cae en ese rango, paginadas

#### Scenario: Mes sin solicitudes
- **WHEN** el rango no contiene ninguna solicitud del empleado
- **THEN** se devuelve una página vacía (sin error)
