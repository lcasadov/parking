# desks Specification (delta)

## ADDED Requirements
### Requirement: Asignación fija de puesto desde la UI admin
**La UI de asignación fija DEBE (MUST) permitir al `ADMIN` asignar un puesto (además de una plaza) a un empleado para uno o varios días de la semana, enviando el `resourceType` correspondiente.**

#### Scenario: Asignar un puesto fijo a un empleado
- **GIVEN** un `ADMIN` en la pantalla de asignación fija de un empleado
- **WHEN** elige recurso "Puesto", selecciona un puesto y los días de la semana
- **THEN** el sistema crea la asignación fija de tipo `DESK` para esos días
- **AND** el empleado puede tener a la vez plaza fija y puesto fijo (recursos independientes)
