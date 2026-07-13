# employees Specification

## Purpose
TBD - created by archiving change init-employees. Update Purpose after archive.
## Requirements
### Requirement: Alta de empleado con unicidad de login y email
**El sistema DEBE (MUST) permitir a un `ADMIN` crear un `Employee` validando que `login` y `email` no estén ya en uso.**

#### Scenario: Alta válida de empleado
- **GIVEN** un `ADMIN` autenticado
- **AND** no existe ningún `Employee` con el `login` ni el `email` indicados
- **WHEN** envía `POST /employees` (operationId `createEmployee`) con los campos obligatorios válidos
- **THEN** el sistema crea el `Employee` con `active = true` y `enabled = true`
- **AND** responde 201 con la identidad del empleado creado

#### Scenario: Alta con login ya existente
- **GIVEN** un `ADMIN` autenticado
- **AND** existe ya un `Employee` con el mismo `login`
- **WHEN** envía `POST /employees` con ese `login`
- **THEN** el sistema responde 409 con `{ error, message, fields, timestamp }` señalando `login`
- **AND** no crea ningún empleado

#### Scenario: Alta con email ya existente
- **GIVEN** un `ADMIN` autenticado
- **AND** existe ya un `Employee` con el mismo `email`
- **WHEN** envía `POST /employees` con ese `email`
- **THEN** el sistema responde 409 con `fields` señalando `email`
- **AND** no crea ningún empleado

#### Scenario: Alta con campos obligatorios inválidos
- **GIVEN** un `ADMIN` autenticado
- **WHEN** envía `POST /employees` con `email` mal formado o `role` fuera de `ADMIN`/`EMPLOYEE`
- **THEN** el sistema responde 400 con `error` de validación y `fields` indicando el campo inválido
- **AND** no crea ningún empleado

### Requirement: Edición, baja lógica y reactivación
**El sistema DEBE (MUST) permitir a un `ADMIN` modificar los datos de un `Employee`, darlo de baja lógicamente (`active = false`) y reactivarlo, preservando siempre la fila.**

#### Scenario: Edición válida de empleado
- **GIVEN** un `ADMIN` autenticado y un `Employee` existente
- **WHEN** envía `PUT /employees/{id}` (operationId `updateEmployee`) con datos válidos y sin colisión de `login`/`email`
- **THEN** el sistema actualiza los campos modificables y `updated_at`
- **AND** responde 200 con el empleado actualizado

#### Scenario: Edición que colisiona con email de otro empleado
- **GIVEN** un `ADMIN` autenticado y dos empleados distintos
- **WHEN** envía `PUT /employees/{id}` cambiando el `email` al de otro empleado
- **THEN** el sistema responde 409 con `fields` señalando `email`
- **AND** no modifica ningún empleado
- **NOTA:** el `login` es **inmutable** por contrato — el schema `EmployeeUpdate`
  de `docs/openapi.yaml` (autoridad del contrato) no incluye `login`; la
  identidad de acceso no cambia tras el alta. La colisión de unicidad editable
  aplica por tanto al `email`. La unicidad de `login` sigue garantizada en el alta
  y por el índice `UX_employees_login`.

#### Scenario: Baja lógica de empleado
- **GIVEN** un `ADMIN` autenticado y un `Employee` con `active = true`
- **WHEN** envía `DELETE /employees/{id}` (operationId `deactivateEmployee`)
- **THEN** el sistema marca `active = false` sin borrar la fila
- **AND** responde 204

#### Scenario: Reactivación de empleado dado de baja
- **GIVEN** un `ADMIN` autenticado y un `Employee` con `active = false`
- **WHEN** envía `POST /employees/{id}/reactivate` (operationId `reactivateEmployee`)
- **THEN** el sistema marca `active = true`
- **AND** responde 204

### Requirement: Reset administrativo de contraseña
**El sistema DEBE (MUST) permitir a un `ADMIN` resetear la contraseña de un `Employee`, fijando `password_must_change = true` y entregando la contraseña temporal según la fase.**

#### Scenario: Reset de contraseña en Fase 1
- **GIVEN** un `ADMIN` autenticado y un `Employee` existente
- **AND** el sistema opera en Fase 1 🟢
- **WHEN** envía `POST /employees/{id}/reset-password` (operationId `resetEmployeePassword`)
- **THEN** el sistema genera una contraseña temporal, la persiste como `password_hash` y pone `password_must_change = true`
- **AND** responde 200 devolviendo la contraseña temporal para mostrarla una sola vez en pantalla

#### Scenario: Reset de contraseña en Fase 2
- **GIVEN** un `ADMIN` autenticado y un `Employee` existente
- **AND** el sistema opera en Fase 2 🔵
- **WHEN** envía `POST /employees/{id}/reset-password`
- **THEN** el sistema fija `password_must_change = true` y envía la contraseña temporal por email al empleado
- **AND** responde 200 sin devolver la contraseña en claro

### Requirement: Autorización por rol
**El sistema DEBE (MUST) restringir todos los endpoints de empleados al rol `ADMIN` y rechazar cualquier acceso de un `EMPLOYEE`.**

#### Scenario: EMPLOYEE intenta listar empleados
- **GIVEN** un `Employee` autenticado con rol `EMPLOYEE`
- **WHEN** envía `GET /employees` (operationId `listEmployees`)
- **THEN** el sistema responde 403 con `{ error, message, fields, timestamp }`
- **AND** no devuelve ningún dato de empleados

#### Scenario: EMPLOYEE intenta resetear una contraseña
- **GIVEN** un `Employee` autenticado con rol `EMPLOYEE`
- **WHEN** envía `POST /employees/{id}/reset-password`
- **THEN** el sistema responde 403
- **AND** no modifica ninguna contraseña

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

