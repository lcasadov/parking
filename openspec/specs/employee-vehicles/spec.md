# employee-vehicles Specification

## Purpose
TBD - created by archiving change employee-vehicles. Update Purpose after archive.
## Requirements
### Requirement: Registro de varios vehículos por empleado
El sistema SHALL permitir al `ADMIN` registrar más de un vehículo asociado a un empleado, cada uno con marca, modelo, matrícula y color, siendo la matrícula el único dato obligatorio.

#### Scenario: Alta de un vehículo con solo la matrícula
- **GIVEN** un `ADMIN` autenticado y un empleado existente
- **WHEN** envía `POST /employees/{employeeId}/vehicles` con `licensePlate` y sin marca/modelo/color
- **THEN** el sistema responde `201` con el vehículo creado asociado a ese empleado (marca/modelo/color vacíos)

#### Scenario: Alta de un vehículo con todos los datos
- **GIVEN** un `ADMIN` autenticado y un empleado existente
- **WHEN** envía `POST /employees/{employeeId}/vehicles` con `brand`, `model`, `licensePlate` y `color`
- **THEN** el sistema responde `201` con el vehículo creado con todos los campos

#### Scenario: Un empleado puede tener varios vehículos
- **GIVEN** un empleado con un vehículo ya registrado
- **WHEN** el `ADMIN` da de alta un segundo vehículo con distinta matrícula
- **THEN** el listado del empleado devuelve ambos vehículos

### Requirement: Matrícula obligatoria y única por empleado
El sistema SHALL exigir una matrícula no vacía en el alta y edición de un vehículo, y SHALL impedir que un mismo empleado tenga dos vehículos con la misma matrícula.

#### Scenario: Matrícula ausente o vacía
- **WHEN** el `ADMIN` crea o edita un vehículo con la matrícula vacía o en blanco
- **THEN** el sistema responde `400` y no persiste el vehículo

#### Scenario: Matrícula duplicada para el mismo empleado
- **GIVEN** un empleado con un vehículo de matrícula `1234ABC`
- **WHEN** el `ADMIN` intenta dar de alta otro vehículo con matrícula `1234ABC` para ese mismo empleado
- **THEN** el sistema responde `409` (conflicto) y no crea el vehículo

### Requirement: Edición y borrado de un vehículo
El sistema SHALL permitir al `ADMIN` editar y borrar cada vehículo de un empleado de forma individual.

#### Scenario: Editar un vehículo
- **GIVEN** un vehículo existente de un empleado
- **WHEN** el `ADMIN` envía `PUT /employees/{employeeId}/vehicles/{vehicleId}` con nuevos valores válidos
- **THEN** el sistema responde `200` con el vehículo actualizado

#### Scenario: Borrar un vehículo
- **GIVEN** un vehículo existente de un empleado
- **WHEN** el `ADMIN` envía `DELETE /employees/{employeeId}/vehicles/{vehicleId}`
- **THEN** el sistema responde `204` y el vehículo deja de aparecer en el listado del empleado

### Requirement: Listado de vehículos de un empleado
El sistema SHALL devolver la lista de vehículos de un empleado al `ADMIN`.

#### Scenario: Listar los vehículos
- **GIVEN** un empleado con cero o más vehículos
- **WHEN** el `ADMIN` envía `GET /employees/{employeeId}/vehicles`
- **THEN** el sistema responde `200` con la lista (posiblemente vacía) de vehículos de ese empleado

### Requirement: Gestión de vehículos reservada al administrador
El sistema SHALL reservar la gestión de vehículos (listar, crear, editar, borrar) al rol `ADMIN`, igual que el resto del CRUD de empleados.

#### Scenario: Usuario no administrador
- **GIVEN** un usuario autenticado con rol distinto de `ADMIN`
- **WHEN** envía cualquier operación sobre `/employees/{employeeId}/vehicles`
- **THEN** el sistema responde `403` (fail closed)

### Requirement: Borrado en cascada con el empleado
El sistema SHALL eliminar los vehículos de un empleado cuando el empleado es eliminado, sin dejar vehículos huérfanos.

#### Scenario: Eliminar el empleado elimina sus vehículos
- **GIVEN** un empleado con uno o más vehículos
- **WHEN** el empleado es eliminado
- **THEN** sus vehículos quedan eliminados y no son accesibles

### Requirement: Tab "Vehículos" en el formulario de empleado
La interfaz de administración SHALL ofrecer un tab "Vehículos" en el formulario de empleado con una tabla (Marca, Modelo, Matrícula, Color) y acciones para añadir, editar y borrar vehículos, exigiendo solo la matrícula en el formulario del vehículo.

#### Scenario: El tab lista y permite gestionar vehículos en edición
- **GIVEN** el `ADMIN` edita un empleado ya existente
- **WHEN** abre el tab "Vehículos"
- **THEN** ve la tabla de vehículos del empleado con los botones para añadir, editar y borrar cada uno

#### Scenario: El tab pide guardar antes en el alta de un empleado nuevo
- **GIVEN** el `ADMIN` está creando un empleado nuevo (aún sin id)
- **WHEN** abre el tab "Vehículos"
- **THEN** la interfaz indica que debe guardar primero el empleado antes de añadir vehículos

#### Scenario: Validación de matrícula obligatoria en el formulario
- **WHEN** el `ADMIN` intenta añadir o guardar un vehículo con la matrícula vacía
- **THEN** el formulario muestra un error de campo y no envía la petición

