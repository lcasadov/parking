## ADDED Requirements

### Requirement: "Mi Semana" como hub índice y multi-recurso
El portal del empleado DEBE (MUST) usar "Mi Semana" como pantalla índice y DEBE (MUST) mostrar, por cada día, el estado de **ambos** tipos de recurso del empleado (plaza y puesto), no solo la plaza. Las acciones de solicitar, liberar y cancelar DEBEN (MUST) ofrecerse en contexto desde el propio día.

#### Scenario: Mi Semana muestra plaza y puesto del día
- **GIVEN** un `EMPLOYEE` con una plaza fija y un puesto fija (o solicitud) el mismo día
- **WHEN** abre "Mi Semana"
- **THEN** el día muestra el estado de la plaza y el estado del puesto de forma independiente
- **AND** no rotula "sin plaza" cuando el empleado tiene un puesto ese día

#### Scenario: Acciones contextuales por día
- **GIVEN** un `EMPLOYEE` en "Mi Semana" sobre un día concreto
- **WHEN** el día admite una acción (solicitar libre, liberar asignado, cancelar solicitud)
- **THEN** la acción correspondiente se ofrece en contexto para ese día y recurso

### Requirement: Fusión de asignaciones fijas y liberaciones del empleado
El portal DEBE (MUST) agrupar "Mis asignaciones fijas" y "Mis liberaciones" en un único destino ("Mis plazas") con dos pestañas, conservando la funcionalidad de consulta y anulación de cada una.

#### Scenario: Mis plazas agrupa fijas y liberaciones
- **GIVEN** un `EMPLOYEE` en el destino "Mis plazas"
- **WHEN** alterna entre las pestañas Asignaciones fijas y Liberaciones
- **THEN** cada pestaña ofrece su listado y sus acciones (liberar recurso fijo / anular liberación futura) como hoy

### Requirement: Liberación de recurso fijo con tipo y etiqueta correctos
Al liberar un recurso fijo desde el portal del empleado, el sistema DEBE (MUST) enviar el `resourceType` correcto del recurso (nunca asumir PARKING por defecto para un puesto) y la UI DEBE (MUST) identificar el recurso por su número/etiqueta de negocio, no por su identificador interno.

#### Scenario: Liberar un puesto fijo envía resourceType DESK
- **GIVEN** un `EMPLOYEE` con un puesto fijo asignado
- **WHEN** libera ese puesto para una fecha
- **THEN** la petición de liberación incluye `resourceType = DESK` y libera el puesto correcto (no una plaza)

#### Scenario: La lista de asignaciones fijas muestra la etiqueta real
- **GIVEN** un `EMPLOYEE` con una plaza fija y un puesto fijo
- **WHEN** ve su lista de asignaciones fijas
- **THEN** cada fila muestra "Plaza P-08" o "Puesto 12" (número + tipo), no el identificador interno del recurso
