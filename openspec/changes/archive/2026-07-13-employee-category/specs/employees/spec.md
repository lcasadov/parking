## ADDED Requirements

### Requirement: Categoría jerárquica del empleado

El sistema DEBE (MUST) asociar a cada `Employee` una **categoría** obligatoria tomada de un conjunto cerrado de 7 valores jerárquicos, ordenados de mayor a menor rango: `CEO`, `CONSEJO`, `DIRECTOR_N1`, `DIRECTOR_N2`, `GERENTE`, `MANDO_INTERMEDIO`, `EMPLEADO`. La categoría DEBE persistirse en la columna `category` de `employees` (`NOT NULL`) y las filas existentes en el momento de la migración DEBEN quedar con el valor por defecto `EMPLEADO`. Cada valor DEBE tener una única etiqueta i18n para su presentación en la UI.

#### Scenario: Alta de empleado con categoría válida
- **GIVEN** un `ADMIN` autenticado
- **WHEN** envía `POST /employees` (operationId `createEmployee`) con `category = DIRECTOR_N1` y el resto de campos obligatorios válidos
- **THEN** el sistema crea el `Employee` persistiendo `category = DIRECTOR_N1`
- **AND** responde 201 incluyendo la categoría en la representación del empleado creado

#### Scenario: Alta de empleado con categoría fuera del dominio
- **GIVEN** un `ADMIN` autenticado
- **WHEN** envía `POST /employees` con `category` con un valor no perteneciente al conjunto cerrado (p. ej. `JEFE`)
- **THEN** el sistema responde 400 con `error` de validación y `fields` señalando `category`
- **AND** no crea ningún empleado

#### Scenario: Alta de empleado sin categoría
- **GIVEN** un `ADMIN` autenticado
- **WHEN** envía `POST /employees` sin el campo `category`
- **THEN** el sistema responde 400 con `fields` señalando `category` como obligatorio
- **AND** no crea ningún empleado

#### Scenario: Edición de la categoría de un empleado
- **GIVEN** un `ADMIN` autenticado y un `Employee` existente con `category = EMPLEADO`
- **WHEN** envía `PUT /employees/{id}` (operationId `updateEmployee`) con `category = GERENTE` y el resto de datos válidos
- **THEN** el sistema actualiza `category = GERENTE` y `updated_at`
- **AND** responde 200 con la categoría actualizada en la representación del empleado

#### Scenario: Listado incluye la categoría de cada empleado
- **GIVEN** un `ADMIN` autenticado y empleados con distintas categorías
- **WHEN** envía `GET /employees` (operationId `listEmployees`)
- **THEN** cada empleado de la respuesta incluye su `category` dentro del conjunto cerrado de valores

#### Scenario: Filas existentes reciben la categoría por defecto en la migración
- **GIVEN** empleados persistidos antes de introducir la columna `category`
- **WHEN** se aplica la migración que añade la columna `category` como `NOT NULL`
- **THEN** todas esas filas quedan con `category = EMPLEADO`
- **AND** ninguna fila queda con `category` nula
