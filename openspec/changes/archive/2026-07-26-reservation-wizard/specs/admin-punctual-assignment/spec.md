## ADDED Requirements

### Requirement: Vista previa de la plaza auto-asignable a un empleado
El sistema DEBE (MUST) permitir a un `ADMIN` consultar, sin crear ninguna asignación, qué plaza se auto-asignaría a un empleado para una fecha concreta según la misma regla de categoría/planta que usa la asignación puntual real. La consulta DEBE (MUST) responder 200 con `available = false` (y el resto de campos `null`) cuando no haya ninguna plaza libre esa fecha, en vez de un error 404/409: no elegir plaza no es un fallo, es información de negocio que la interfaz debe poder mostrar.

#### Scenario: Vista previa con plaza disponible
- **GIVEN** un `ADMIN` autenticado, un empleado con categoría asignada y al menos una plaza libre para la fecha F
- **WHEN** envía `GET /requests/admin/suggested-space?employeeId={id}&date=F`
- **THEN** el sistema responde 200 con `available = true` y el `parkingSpaceId`, `number` y `floor` de la plaza que la auto-asignación elegiría
- **AND** no crea ninguna asignación ni ocupa la plaza

#### Scenario: Vista previa sin ninguna plaza libre esa fecha
- **GIVEN** un `ADMIN` autenticado y ninguna plaza libre para la fecha F
- **WHEN** envía `GET /requests/admin/suggested-space?employeeId={id}&date=F`
- **THEN** el sistema responde 200 con `available = false` y el resto de campos `null`

#### Scenario: Un EMPLOYEE no puede consultar la vista previa
- **GIVEN** un usuario con rol `EMPLOYEE` autenticado
- **WHEN** envía `GET /requests/admin/suggested-space?employeeId={id}&date=F`
- **THEN** el sistema responde 403 y no revela ninguna información de plaza

#### Scenario: Parámetros ausentes o inválidos
- **GIVEN** un `ADMIN` autenticado
- **WHEN** envía `GET /requests/admin/suggested-space` sin `employeeId`, sin `date`, o con `date` en un formato inválido
- **THEN** el sistema responde 400

#### Scenario: Empleado inexistente
- **GIVEN** un `ADMIN` autenticado
- **WHEN** envía `GET /requests/admin/suggested-space?employeeId={id-inexistente}&date=F`
- **THEN** el sistema responde 404
