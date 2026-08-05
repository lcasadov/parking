# employee-vehicle-self-service Specification

## Purpose
TBD - created by archiving change employee-vehicle-self-service. Update Purpose after archive.
## Requirements
### Requirement: Gestión self-service de los propios vehículos por el empleado
El sistema SHALL permitir a un empleado autenticado dar de alta, modificar y borrar **sus propios** vehículos (marca, modelo, matrícula y color; solo la matrícula obligatoria) desde el portal del empleado, sin poder acceder a los vehículos de otros empleados.

#### Scenario: El empleado da de alta un vehículo propio
- **GIVEN** un empleado autenticado
- **WHEN** envía `POST /me/vehicles` con `licensePlate` (y opcionalmente marca/modelo/color)
- **THEN** el sistema responde `201` con el vehículo creado, asociado a ese empleado y en estado `PENDING`

#### Scenario: El empleado edita o borra un vehículo propio
- **GIVEN** un empleado con un vehículo propio
- **WHEN** envía `PUT /me/vehicles/{vehicleId}` con datos válidos o `DELETE /me/vehicles/{vehicleId}`
- **THEN** el sistema responde `200`/`204` respectivamente y solo sobre vehículos de ese empleado

#### Scenario: El empleado no puede acceder a vehículos de otro empleado
- **GIVEN** un empleado autenticado
- **WHEN** intenta operar sobre un vehículo que no le pertenece
- **THEN** el sistema responde `404` (no se filtra la existencia de vehículos ajenos)

### Requirement: Estado de validación del vehículo
Cada vehículo SHALL tener un estado del conjunto `PENDING`, `IN_PROGRESS` (en trámite), `APPROVED`, `REJECTED` o `PENDING_DELETION` (pendiente de borrado). Un vehículo `REJECTED` SHALL conservar el motivo del rechazo. Los vehículos creados por el `ADMIN` (gestión existente de vehículos de empleado) SHALL nacer `APPROVED`.

#### Scenario: Alta por el empleado nace pendiente
- **WHEN** un empleado da de alta un vehículo propio
- **THEN** el vehículo queda en estado `PENDING`

#### Scenario: Alta por el administrador nace aprobada
- **WHEN** el `ADMIN` da de alta un vehículo para un empleado
- **THEN** el vehículo queda en estado `APPROVED`

#### Scenario: Los vehículos existentes se consideran validados
- **GIVEN** vehículos ya registrados antes de esta capacidad
- **WHEN** se aplica la migración de estado
- **THEN** esos vehículos quedan en estado `APPROVED`

### Requirement: La edición por el empleado deja el vehículo pendiente de validación
El sistema SHALL poner en estado `PENDING` cualquier vehículo que el empleado modifique, con independencia de su estado anterior (`APPROVED` o `REJECTED`), limpiando el motivo de rechazo previo.

#### Scenario: Editar un vehículo aprobado lo devuelve a pendiente
- **GIVEN** un vehículo propio en estado `APPROVED`
- **WHEN** el empleado lo modifica
- **THEN** el vehículo pasa a `PENDING` y requiere una nueva validación

#### Scenario: Editar un vehículo rechazado lo devuelve a pendiente
- **GIVEN** un vehículo propio en estado `REJECTED` con motivo
- **WHEN** el empleado lo modifica
- **THEN** el vehículo pasa a `PENDING` y el motivo de rechazo anterior deja de mostrarse

### Requirement: Aviso a los administradores al enviar o modificar un vehículo
El sistema SHALL notificar a cada administrador activo (por email y push) cuando un empleado da de alta o modifica uno de sus vehículos, para que pueda revisarlo.

#### Scenario: Alta de un vehículo notifica a los administradores
- **WHEN** un empleado da de alta un vehículo propio
- **THEN** el sistema emite una notificación (email + push) a cada `ADMIN` activo indicando el empleado y el vehículo

#### Scenario: Modificación de un vehículo notifica a los administradores
- **WHEN** un empleado modifica un vehículo propio
- **THEN** el sistema emite una notificación (email + push) a cada `ADMIN` activo

#### Scenario: El borrado no genera aviso de validación
- **WHEN** un empleado borra un vehículo propio
- **THEN** no se emite una notificación de "pendiente de validación" (no hay nada que validar)

### Requirement: El empleado ve el estado y el motivo de rechazo de sus vehículos
La interfaz del portal SHALL ofrecer una sección "Mis vehículos" que muestra cada vehículo con su estado (Pendiente / Aprobado / Rechazado) y, cuando está rechazado, el motivo; y SHALL avisar, al dar de alta o modificar, de que el vehículo quedará pendiente de validación.

#### Scenario: Ver el estado de cada vehículo
- **GIVEN** un empleado con vehículos en distintos estados
- **WHEN** abre "Mis vehículos"
- **THEN** ve cada vehículo con un indicador de estado y, si está rechazado, el motivo

#### Scenario: Aviso de validación al guardar
- **WHEN** el empleado abre el formulario de alta o edición de un vehículo
- **THEN** la interfaz indica que, al guardar, el vehículo quedará pendiente de validación

### Requirement: Bandeja de validación de vehículos para el administrador (Fase 2)
El sistema SHALL ofrecer al `ADMIN` una pantalla que liste los vehículos por estado (con foco en los pendientes), con búsqueda por empleado o matrícula, orden por fecha de solicitud y un contador de pendientes de acción (`PENDING` + `PENDING_DELETION`).

#### Scenario: Listar los vehículos pendientes de validación
- **GIVEN** uno o más vehículos en estado `PENDING`
- **WHEN** el `ADMIN` abre la bandeja de validación
- **THEN** ve los vehículos pendientes con el empleado, el vehículo, la matrícula y la fecha de solicitud, y un contador de pendientes

### Requirement: En trámite, aprobación y rechazo con motivo por el administrador (Fase 2)
El sistema SHALL permitir al `ADMIN` marcar un vehículo pendiente como `IN_PROGRESS` (en trámite), aprobarlo o rechazarlo; el rechazo SHALL exigir un motivo en **texto libre**. La gestión SHALL estar reservada al rol `ADMIN`.

#### Scenario: Marcar un vehículo en trámite
- **GIVEN** un vehículo en estado `PENDING`
- **WHEN** el `ADMIN` lo marca en trámite
- **THEN** el vehículo pasa a `IN_PROGRESS`

#### Scenario: Aprobar un vehículo pendiente o en trámite
- **GIVEN** un vehículo en estado `PENDING` o `IN_PROGRESS`
- **WHEN** el `ADMIN` lo aprueba
- **THEN** el vehículo pasa a `APPROVED`

#### Scenario: Rechazar un vehículo exige motivo
- **GIVEN** un vehículo en estado `PENDING` o `IN_PROGRESS`
- **WHEN** el `ADMIN` lo rechaza sin indicar motivo
- **THEN** el sistema no aplica el rechazo y exige un motivo

#### Scenario: Rechazar un vehículo con motivo en texto libre
- **GIVEN** un vehículo en estado `PENDING` o `IN_PROGRESS`
- **WHEN** el `ADMIN` lo rechaza indicando un motivo en texto libre
- **THEN** el vehículo pasa a `REJECTED` y conserva el motivo

#### Scenario: Validación ya resuelta por otro administrador
- **GIVEN** un vehículo que otro `ADMIN` acaba de procesar (ya no es `PENDING` ni `IN_PROGRESS`)
- **WHEN** un `ADMIN` intenta aprobarlo o rechazarlo
- **THEN** el sistema responde con conflicto y la interfaz refresca el estado real

### Requirement: Aviso al empleado del resultado de la revisión (Fase 2)
El sistema SHALL notificar al empleado (por email y push) cuando su vehículo pasa a en trámite, es aprobado o es rechazado, incluyendo el motivo en caso de rechazo.

#### Scenario: Notificar en trámite
- **WHEN** el `ADMIN` marca en trámite un vehículo de un empleado
- **THEN** el sistema notifica al empleado (email + push) que su vehículo está en trámite

#### Scenario: Notificar la aprobación
- **WHEN** el `ADMIN` aprueba un vehículo de un empleado
- **THEN** el sistema notifica al empleado (email + push) que su vehículo ha sido aprobado

#### Scenario: Notificar el rechazo con el motivo
- **WHEN** el `ADMIN` rechaza un vehículo de un empleado con un motivo
- **THEN** el sistema notifica al empleado (email + push) el rechazo y el motivo

### Requirement: Borrado por el empleado de un vehículo en trámite o aprobado (Fase 2)
Cuando un empleado solicita borrar un vehículo que está `IN_PROGRESS` o `APPROVED`, el sistema NO SHALL borrarlo directamente: SHALL dejarlo `PENDING_DELETION`, avisar al `ADMIN` y permitir que el `ADMIN` confirme el borrado o lo restaure a su estado previo. Si el vehículo estaba `PENDING` o `REJECTED`, el borrado del empleado SHALL ser directo.

#### Scenario: Borrar un vehículo aprobado lo deja pendiente de borrado
- **GIVEN** un vehículo propio en estado `APPROVED` (o `IN_PROGRESS`)
- **WHEN** el empleado solicita borrarlo
- **THEN** el vehículo pasa a `PENDING_DELETION` y se avisa a los administradores

#### Scenario: Borrar un vehículo pendiente o rechazado es directo
- **GIVEN** un vehículo propio en estado `PENDING` o `REJECTED`
- **WHEN** el empleado solicita borrarlo
- **THEN** el vehículo se borra directamente

#### Scenario: El administrador confirma el borrado
- **GIVEN** un vehículo en estado `PENDING_DELETION`
- **WHEN** el `ADMIN` confirma el borrado
- **THEN** el vehículo se elimina

#### Scenario: El administrador restaura el vehículo
- **GIVEN** un vehículo en estado `PENDING_DELETION`
- **WHEN** el `ADMIN` lo restaura
- **THEN** el vehículo vuelve a su estado previo (`IN_PROGRESS` o `APPROVED`)

### Requirement: Histórico de cambios del vehículo (Fase 2)
El sistema SHALL registrar el histórico de un vehículo (alta, ediciones del empleado con los datos previos, cambios de estado con su motivo, y la solicitud de borrado) y SHALL permitir al `ADMIN` consultarlo.

#### Scenario: Registrar una edición del empleado
- **GIVEN** un vehículo existente
- **WHEN** el empleado modifica sus datos
- **THEN** el histórico registra la edición conservando los datos previos

#### Scenario: El administrador consulta el histórico
- **GIVEN** un vehículo con cambios registrados
- **WHEN** el `ADMIN` abre su histórico
- **THEN** ve la secuencia de eventos (alta, ediciones, cambios de estado, solicitud de borrado)

### Requirement: Estado del vehículo visible en el formulario de empleado (Fase 2)
El tab "Vehículos" del formulario de empleado (gestión del `ADMIN`) SHALL mostrar el estado de validación de cada vehículo y dar acceso a su histórico.

#### Scenario: El administrador ve el estado en el tab de vehículos
- **GIVEN** el `ADMIN` edita un empleado con vehículos en distintos estados
- **WHEN** abre el tab "Vehículos"
- **THEN** ve el estado de cada vehículo y puede abrir su histórico


### Requirement: Corrección manual del estado por el administrador (Fase 2)
El sistema SHALL permitir al `ADMIN` cambiar manualmente el estado de un vehículo entre `PENDING`, `IN_PROGRESS`, `APPROVED` y `REJECTED` (para corregir, p.ej., un aprobado por error). El cambio a `REJECTED` SHALL exigir un motivo. No SHALL permitir fijar `PENDING_DELETION` por esta vía ni cambiar el estado de un vehículo `PENDING_DELETION` (se usa confirmar/restaurar). Registra histórico y avisa al empleado (salvo al volver a `PENDING`).

#### Scenario: Deshacer un aprobado por error
- **GIVEN** un vehículo en estado `APPROVED`
- **WHEN** el `ADMIN` cambia su estado a `PENDING` (o `REJECTED` con motivo)
- **THEN** el vehículo pasa a ese estado y queda registrado en el histórico

#### Scenario: Cambiar a rechazado exige motivo
- **WHEN** el `ADMIN` cambia el estado de un vehículo a `REJECTED` sin indicar motivo
- **THEN** el sistema no aplica el cambio y exige un motivo

### Requirement: Bandeja filtrable por estado con contadores (Fase 2)
La bandeja de validación SHALL ofrecer un filtro "Abiertos" que muestra los vehículos `PENDING`, `IN_PROGRESS` y `PENDING_DELETION`, filtros individuales por estado y un filtro "Todos", cada uno con su contador.

#### Scenario: El filtro "Abiertos" muestra todo lo pendiente de gestión
- **GIVEN** vehículos en varios estados
- **WHEN** el `ADMIN` selecciona el filtro "Abiertos"
- **THEN** ve los vehículos `PENDING`, `IN_PROGRESS` y `PENDING_DELETION`, y cada filtro muestra su contador
