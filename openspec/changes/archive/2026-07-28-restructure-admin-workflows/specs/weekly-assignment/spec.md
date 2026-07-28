## MODIFIED Requirements

### Requirement: Presentación de la asignación semanal
La vista semanal (destino "Ocupación") DEBE (MUST) presentar el estado de cada recurso por día usando el mapa estado→color del design system, y sus celdas DEBEN (MUST) ser **accionables**: desde una celda el `ADMIN` puede iniciar en contexto la asignación o la liberación del recurso para ese día/empleado, sin abandonar la pantalla ni abrir la ficha del empleado. La vista integra la disponibilidad (el estado `FREE` filtrable) que antes vivía en una pantalla separada.

#### Scenario: Lectura de estado por color
- **WHEN** se carga una semana
- **THEN** cada celda recurso×día se colorea según su estado (ocupado/liberado/pendiente/solicitud/libre)
- **AND** la columna del día actual queda resaltada

#### Scenario: Navegación de semana
- **WHEN** el usuario cambia de semana o pulsa Hoy
- **THEN** el grid se actualiza con los datos de la semana seleccionada usando los endpoints existentes

#### Scenario: Asignar desde una celda libre
- **GIVEN** un `ADMIN` viendo una celda en estado `FREE` para un recurso y un día
- **WHEN** activa la celda y elige un empleado
- **THEN** el sistema aplica la asignación en contexto —fija (`PUT /fixed-assignments/employee/{id}`) para el día de la semana, o puntual (asignación puntual del admin) para esa fecha concreta— y la rejilla se refresca

#### Scenario: Liberar desde una celda ocupada
- **GIVEN** un `ADMIN` viendo una celda en estado `ASSIGNED` o `REQUEST_APPROVED`
- **WHEN** activa la celda y confirma la liberación
- **THEN** el sistema libera el recurso para esa fecha (liberación administrativa o admin-cancel según el origen) y la rejilla se refresca

#### Scenario: Conflicto de asignación mostrado en contexto
- **GIVEN** un `ADMIN` que intenta asignar un recurso ya ocupado por otro empleado ese día
- **WHEN** confirma la acción
- **THEN** el sistema responde con el conflicto (409) y la vista muestra el error traducido sin romperse
