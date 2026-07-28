## ADDED Requirements

### Requirement: Recurso distinto por día en la asignación fija
El sistema DEBE (MUST) permitir que un empleado tenga **recursos distintos del mismo tipo en días distintos** de la semana (p. ej. puesto 1 el lunes y puesto 3 el miércoles; o plaza P-08 el lunes y P-12 el jueves). La única restricción es la ya existente: un empleado NO PUEDE (MUST NOT) tener **dos recursos del mismo tipo el mismo día** (índice único `(employee_id, resource_type, day_of_week)` entre filas activas). El modelo de datos y el backend (`PUT /fixed-assignments/employee/{id}`, cuya reconciliación de días está acotada a un `resource_id` concreto) YA lo soportan; esta capacidad expone esa posibilidad en la UI de escritura.

#### Scenario: Asignar un recurso a un día sin alterar otros días con recurso distinto
- **GIVEN** un empleado con el puesto 1 asignado el lunes
- **WHEN** el admin le asigna el puesto 3 el miércoles (desde la rejilla de Ocupación o el modal)
- **THEN** el empleado queda con puesto 1 el lunes Y puesto 3 el miércoles
- **AND** la asignación del lunes al puesto 1 NO se modifica ni se revoca

#### Scenario: La rejilla usa los días del recurso de la propia celda
- **GIVEN** un empleado con el puesto 1 el lunes y el puesto 3 el miércoles
- **WHEN** el admin añade el viernes al puesto 3 desde su celda en la rejilla
- **THEN** el `PUT` se emite para el puesto 3 con sus días propios (miércoles + viernes), sin arrastrar los días del puesto 1

#### Scenario: Conflicto de dos recursos del mismo tipo el mismo día
- **GIVEN** un empleado con el puesto 1 el lunes
- **WHEN** el admin intenta asignarle también el puesto 3 el lunes
- **THEN** el sistema responde 409 (índice único por empleado/tipo/día) y la UI muestra el conflicto traducido sin romperse

### Requirement: Selección de recurso por día en el modal de empleado
El modal de empleado DEBE (MUST) permitir elegir, por cada día de la semana y por cada tipo de recurso, **qué recurso concreto** se asigna, de modo que se pueda configurar un mapa día→recurso (no un único recurso para todos los días). El caso común (un mismo recurso para varios días) DEBE (MUST) seguir siendo cómodo de configurar.

#### Scenario: Configurar días con recursos distintos desde el modal
- **GIVEN** el admin editando un empleado en el modal
- **WHEN** asigna el puesto 1 al lunes y el puesto 3 al miércoles y guarda
- **THEN** se persisten ambas asignaciones (puesto 1 lunes, puesto 3 miércoles) mediante los `PUT` por recurso correspondientes

#### Scenario: El prefill del modal refleja recursos distintos por día
- **GIVEN** un empleado que ya tiene puesto 1 el lunes y puesto 3 el miércoles
- **WHEN** el admin abre su modal
- **THEN** el modal muestra ambos recursos con sus días respectivos (no colapsa a un único recurso por tipo)
