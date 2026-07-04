# desks Specification

## Purpose
TBD - created by archiving change init-desks. Update Purpose after archive.
## Requirements
### Requirement: Alta de puesto numerado
**El sistema DEBE (MUST) permitir al `ADMIN` crear un `Desk` con `number` único en el rango 1-65 y una `DeskCategory` válida, e impedir números duplicados o fuera de rango.**

#### Scenario: Alta de puesto válido
- **GIVEN** un `ADMIN` autenticado
- **WHEN** envía `POST /desks` con `{ number: 12, category: STANDARD, coordX: 30.5, coordY: 47.0 }`
- **THEN** el sistema crea el `Desk` con `active = true`
- **AND** responde 201 con la representación del puesto

#### Scenario: Número de puesto duplicado
- **GIVEN** un `Desk` existente con `number = 12`
- **WHEN** un `ADMIN` envía `POST /desks` con `number = 12`
- **THEN** el sistema responde 409 con `error` de conflicto de unicidad y no crea un segundo puesto

#### Scenario: Número fuera del rango 1-65
- **GIVEN** un `ADMIN` autenticado
- **WHEN** envía `POST /desks` con `number = 70`
- **THEN** el sistema responde 400 con `fields` indicando que `number` está fuera de rango (1-65)

#### Scenario: Empleado intenta crear un puesto
- **GIVEN** un `EMPLOYEE` autenticado
- **WHEN** envía `POST /desks`
- **THEN** el sistema responde 403 y no crea ningún puesto

### Requirement: Edición y activación/desactivación de puesto
**El sistema DEBE (MUST) permitir al `ADMIN` editar la categoría y coordenadas de un puesto y activarlo/desactivarlo, sin que un puesto inactivo sea reservable.**

#### Scenario: Cambio de categoría a EXECUTIVE
- **GIVEN** un `Desk` `STANDARD` existente
- **WHEN** un `ADMIN` envía `PUT /desks/{id}` con `category = EXECUTIVE`
- **THEN** el sistema actualiza la categoría
- **AND** responde 200 con el puesto actualizado

#### Scenario: Desactivar puesto lo retira de disponibilidad
- **GIVEN** un `Desk` activo sin asignaciones ni solicitudes para una fecha F
- **WHEN** un `ADMIN` envía `PATCH /desks/{id}/activation` con `active = false`
- **THEN** el sistema marca el puesto `active = false`
- **AND** el puesto deja de aparecer como disponible en el cálculo de disponibilidad para F

### Requirement: Asignación fija de puesto independiente de la de plaza
**El sistema DEBE (MUST) permitir asignar fijamente un puesto (`DESK`) a un empleado que ya tenga una plaza fija (`PARKING`), tratándolos como recursos distintos, y mantener la unicidad recurso/día y empleado/día por tipo de recurso.**

#### Scenario: Empleado con plaza fija recibe además puesto fijo
- **GIVEN** un `Employee` con una `FixedAssignment` activa de tipo `PARKING` para el lunes
- **WHEN** un `ADMIN` crea una `FixedAssignment` de tipo `DESK` para el mismo empleado y el mismo lunes
- **THEN** el sistema crea la asignación de puesto sin conflicto con la de plaza
- **AND** responde 201

#### Scenario: Dos empleados sobre el mismo puesto el mismo día
- **GIVEN** una `FixedAssignment` activa de `DESK` para el puesto 5 el martes
- **WHEN** un `ADMIN` intenta asignar fijamente el puesto 5 a otro empleado el martes
- **THEN** el sistema responde 409 por unicidad recurso/día

### Requirement: Solicitud y disponibilidad de puesto, EXECUTIVE liberable
**El sistema DEBE (MUST) permitir a un empleado solicitar un puesto para una fecha dentro de la ventana hoy..+14d, calcular su disponibilidad como cualquier recurso y permitir liberar un puesto `EXECUTIVE` igual que uno `STANDARD`.**

#### Scenario: Solicitud de puesto disponible
- **GIVEN** un `EMPLOYEE` autenticado y un `Desk` activo sin asignación ni solicitud aprobada para la fecha F dentro de la ventana
- **WHEN** envía una solicitud (`Request`) de tipo `DESK` para la fecha F
- **THEN** el sistema crea la `Request` en estado `PENDING`
- **AND** responde 201

#### Scenario: Puesto EXECUTIVE liberado queda disponible
- **GIVEN** un `Desk` `EXECUTIVE` con asignación fija L-V a un directivo
- **WHEN** el directivo crea una liberación voluntaria (`Release`) de ese puesto para una fecha futura F
- **THEN** el sistema marca el puesto como disponible para F en el cálculo de disponibilidad
- **AND** otro empleado puede solicitarlo para F

#### Scenario: Solicitud de puesto ya asignado ese día
- **GIVEN** un `Desk` con `Request APPROVED` para la fecha F
- **WHEN** un `ADMIN` intenta aprobar otra solicitud del mismo puesto para F
- **THEN** el sistema responde 409 por falta de disponibilidad y no aprueba la segunda
