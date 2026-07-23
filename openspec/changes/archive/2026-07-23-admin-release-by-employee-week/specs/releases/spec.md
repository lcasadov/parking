# releases

## ADDED Requirements

### Requirement: Selección de empleado y ocupación semanal para liberación (ADMIN/AGENCIA)
**El sistema DEBE (MUST) ofrecer a un `ADMIN` o `AGENCIA`, para el flujo de liberación administrativa: (a) un listado de empleados seleccionables (datos mínimos: id y nombre) y (b) la ocupación de un empleado para una semana dada, devolviendo por cada día las reservas del empleado en plaza y en puesto, con su origen (`FIXED_ASSIGNMENT` o `REQUEST_APPROVED`), la fecha, el recurso (número/etiqueta) y el `requestId` cuando la ocupación proviene de una solicitud aprobada. Cualquier otro rol (incluido `EMPLOYEE`) DEBE (MUST) recibir 403.**

#### Scenario: Ocupación semanal de un empleado para ADMIN
- **GIVEN** un `ADMIN` autenticado, un `Employee` con una plaza por asignación fija y un puesto por solicitud aprobada dentro de la semana consultada
- **WHEN** solicita la ocupación semanal de ese empleado indicando el inicio de semana
- **THEN** el sistema responde 200 con las reservas de la semana (plaza y puesto), cada una con su fecha, recurso y origen
- **AND** las reservas por solicitud incluyen su `requestId`

#### Scenario: Ocupación semanal de un empleado para AGENCIA
- **GIVEN** un usuario `AGENCIA` autenticado
- **WHEN** solicita el listado de empleados seleccionables y la ocupación semanal de un empleado
- **THEN** el sistema responde 200 en ambos casos con los datos correspondientes

#### Scenario: Un EMPLOYEE no puede consultar la ocupación de otros
- **GIVEN** un `Employee` (rol `EMPLOYEE`) autenticado
- **WHEN** solicita el listado de empleados seleccionables o la ocupación semanal de un empleado
- **THEN** el sistema responde 403
- **AND** no devuelve datos
