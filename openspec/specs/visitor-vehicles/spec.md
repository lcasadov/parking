# visitor-vehicles Specification

## Purpose
TBD - created by archiving change visitor-vehicles. Update Purpose after archive.
## Requirements
### Requirement: Registro de varios vehículos por visitante
El sistema SHALL permitir al `ADMIN` registrar más de un vehículo asociado a un visitante, cada uno con marca, modelo, matrícula y color, siendo la matrícula el único dato obligatorio.

#### Scenario: Alta de un vehículo con solo la matrícula
- **GIVEN** un `ADMIN` autenticado y un visitante existente
- **WHEN** envía `POST /visitors/{visitorId}/vehicles` con `licensePlate` y sin marca/modelo/color
- **THEN** el sistema responde `201` con el vehículo creado asociado a ese visitante (marca/modelo/color vacíos)

#### Scenario: Alta de un vehículo con todos los datos
- **GIVEN** un `ADMIN` autenticado y un visitante existente
- **WHEN** envía `POST /visitors/{visitorId}/vehicles` con `brand`, `model`, `licensePlate` y `color`
- **THEN** el sistema responde `201` con el vehículo creado con todos los campos

#### Scenario: Un visitante puede tener varios vehículos
- **GIVEN** un visitante con un vehículo ya registrado
- **WHEN** el `ADMIN` da de alta un segundo vehículo con distinta matrícula
- **THEN** el listado del visitante devuelve ambos vehículos

### Requirement: Matrícula obligatoria y única por visitante
El sistema SHALL exigir una matrícula no vacía en el alta y edición de un vehículo, y SHALL impedir que un mismo visitante tenga dos vehículos con la misma matrícula.

#### Scenario: Matrícula ausente o vacía
- **WHEN** el `ADMIN` crea o edita un vehículo con la matrícula vacía o en blanco
- **THEN** el sistema responde `400` y no persiste el vehículo

#### Scenario: Matrícula duplicada para el mismo visitante
- **GIVEN** un visitante con un vehículo de matrícula `1234ABC`
- **WHEN** el `ADMIN` intenta dar de alta otro vehículo con matrícula `1234ABC` para ese mismo visitante
- **THEN** el sistema responde `409` (conflicto) y no crea el vehículo

### Requirement: Edición y borrado de un vehículo
El sistema SHALL permitir al `ADMIN` editar y borrar cada vehículo de un visitante de forma individual.

#### Scenario: Editar un vehículo
- **GIVEN** un vehículo existente de un visitante
- **WHEN** el `ADMIN` envía `PUT /visitors/{visitorId}/vehicles/{vehicleId}` con nuevos valores válidos
- **THEN** el sistema responde `200` con el vehículo actualizado

#### Scenario: Borrar un vehículo
- **GIVEN** un vehículo existente de un visitante
- **WHEN** el `ADMIN` envía `DELETE /visitors/{visitorId}/vehicles/{vehicleId}`
- **THEN** el sistema responde `204` y el vehículo deja de aparecer en el listado del visitante

### Requirement: Listado de vehículos de un visitante
El sistema SHALL devolver la lista de vehículos de un visitante al `ADMIN`.

#### Scenario: Listar los vehículos
- **GIVEN** un visitante con cero o más vehículos
- **WHEN** el `ADMIN` envía `GET /visitors/{visitorId}/vehicles`
- **THEN** el sistema responde `200` con la lista (posiblemente vacía) de vehículos de ese visitante

### Requirement: Gestión de vehículos reservada al administrador
El sistema SHALL reservar la gestión de vehículos (listar, crear, editar, borrar) al rol `ADMIN`, igual que el resto del CRUD de visitante.

#### Scenario: Usuario no administrador
- **GIVEN** un usuario autenticado con rol distinto de `ADMIN`
- **WHEN** envía cualquier operación sobre `/visitors/{visitorId}/vehicles`
- **THEN** el sistema responde `403` (fail closed)

### Requirement: Borrado en cascada con el visitante
El sistema SHALL eliminar los vehículos de un visitante cuando el visitante es eliminado, sin dejar vehículos huérfanos.

#### Scenario: Eliminar el visitante elimina sus vehículos
- **GIVEN** un visitante con uno o más vehículos
- **WHEN** el visitante es eliminado
- **THEN** sus vehículos quedan eliminados y no son accesibles

### Requirement: Tab "Vehículos" en el formulario de visitante
La interfaz de administración SHALL ofrecer un tab "Vehículos" en el formulario de visitante con una tabla (Marca, Modelo, Matrícula, Color) y acciones para añadir, editar y borrar vehículos, exigiendo solo la matrícula en el formulario del vehículo. En el alta de un visitante nuevo el tab SHALL permitir añadir vehículos sin obligar a guardar antes, persistiéndolos al crear el visitante.

#### Scenario: El tab lista y permite gestionar vehículos en edición
- **GIVEN** el `ADMIN` edita un visitante ya existente
- **WHEN** abre el tab "Vehículos"
- **THEN** ve la tabla de vehículos del visitante con los botones para añadir, editar y borrar cada uno

#### Scenario: Alta de vehículos en borrador durante el alta del visitante
- **GIVEN** el `ADMIN` está creando un visitante nuevo (aún sin id)
- **WHEN** abre el tab "Vehículos" y añade uno o más vehículos
- **THEN** los vehículos se acumulan en la interfaz sin obligar a guardar antes el visitante

#### Scenario: Los vehículos en borrador se persisten al crear el visitante
- **GIVEN** el `ADMIN` ha añadido vehículos en borrador y datos válidos del visitante
- **WHEN** guarda el visitante
- **THEN** el sistema crea el visitante y a continuación sus vehículos asociados

#### Scenario: Validación de matrícula obligatoria en el formulario
- **WHEN** el `ADMIN` intenta añadir o guardar un vehículo con la matrícula vacía
- **THEN** el formulario muestra un error de campo y no envía la petición

### Requirement: Retirada del campo de matrícula del formulario de visitante
El formulario de visitante SHALL dejar de incluir un campo de matrícula suelto; la matrícula del visitante se gestiona exclusivamente en el tab "Vehículos".

#### Scenario: El formulario de visitante no pide una matrícula única
- **GIVEN** el `ADMIN` crea o edita un visitante
- **WHEN** abre la pestaña "Detalles" del formulario
- **THEN** no existe un campo de matrícula en el formulario (la matrícula se añade como vehículo en el tab "Vehículos")

