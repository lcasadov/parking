# requests

## MODIFIED Requirements

### Requirement: Auto-asignación de plaza por categoría y planta en modo AUTOMATIC
**Cuando `approvalMode = AUTOMATIC` y un empleado solicita una PLAZA, el sistema DEBE (MUST) asignar automáticamente una plaza LIBRE para la fecha eligiéndola por la categoría del empleado y la planta FÍSICA de la plaza, y la solicitud DEBE nacer en estado `APPROVED` con la plaza asignada. El aparcamiento es un garaje SUBTERRÁNEO: la planta física es un sótano `-(número de plaza / 1000)`, de modo que las plazas `1xxx` están en la planta `-1` (la más alta, cercana a la superficie) y las `5xxx` en la planta `-5` (la más baja/profunda). Las categorías altas hasta Director nivel 2 (CEO, Consejo, Director N1, Director N2) DEBEN preferir las plantas físicas más altas disponibles (planta `-1` primero, probando de la `-1` a la `-5`, es decir del número de millar más bajo al más alto); el resto de categorías (Gerente, Mando intermedio, Empleado) DEBEN preferir las plantas físicas más bajas (planta `-5` primero, de la `-5` a la `-1`, del número de millar más alto al más bajo), cayendo a la siguiente planta según el orden de preferencia cuando la preferida no tiene plazas libres. Dentro de una misma planta la elección es determinista (menor número).**

#### Scenario: Categoría alta recibe la planta física más alta disponible
- **GIVEN** `approvalMode = AUTOMATIC`, un `Employee` de categoría `DIRECTOR_N2` y plazas libres en la planta `-5` (números `5xxx`) y la planta `-1` (números `1xxx`) para `requested_date`
- **WHEN** envía `POST /requests` de tipo `PARKING`
- **THEN** el sistema responde 201 con la solicitud en estado `APPROVED` y una plaza de la planta `-1` (número `1xxx`) asignada

#### Scenario: Categoría base recibe la planta física más baja disponible
- **GIVEN** `approvalMode = AUTOMATIC`, un `Employee` de categoría `EMPLEADO` y plazas libres en la planta `-5` (números `5xxx`) y la planta `-1` (números `1xxx`) para `requested_date`
- **WHEN** envía `POST /requests` de tipo `PARKING`
- **THEN** el sistema responde 201 con la solicitud en estado `APPROVED` y una plaza de la planta `-5` (número `5xxx`) asignada

#### Scenario: Fallback a la siguiente planta según preferencia
- **GIVEN** `approvalMode = AUTOMATIC`, un `Employee` de categoría `DIRECTOR_N1` y sin plazas libres en la planta `-1` (`1xxx`) pero con plazas libres en la planta `-2` (`2xxx`) para `requested_date`
- **WHEN** envía `POST /requests` de tipo `PARKING`
- **THEN** el sistema responde 201 con la solicitud en estado `APPROVED` y una plaza de la planta `-2` (número `2xxx`) asignada

#### Scenario: Sin ninguna plaza libre no se puede auto-asignar
- **GIVEN** `approvalMode = AUTOMATIC`, un `Employee` de cualquier categoría y ninguna plaza libre en ninguna planta para `requested_date`
- **WHEN** envía `POST /requests` de tipo `PARKING`
- **THEN** el sistema responde 409 con `error = NO_AVAILABILITY`
- **AND** no crea ninguna solicitud

#### Scenario: Concurrencia sobre la misma plaza auto-asignada
- **GIVEN** `approvalMode = AUTOMATIC` y dos empleados cuyo algoritmo selecciona simultáneamente la misma plaza para la misma `requested_date`
- **WHEN** la segunda solicitud se confirma tras la primera
- **THEN** el sistema responde 409 a la segunda (colisión detectada por el índice único filtrado `APPROVED`)
- **AND** solo la primera solicitud queda `APPROVED` con esa plaza
