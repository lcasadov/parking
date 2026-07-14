## ADDED Requirements

### Requirement: Elección de un puesto concreto en la solicitud unificada
El modal de solicitud unificada DEBE (MUST) permitir al empleado, cuando el recurso seleccionado incluye PUESTO, **elegir un puesto concreto** abriendo el plano de la planta como selector, y DEBE enviar el `resourceId` del puesto elegido en `POST /requests`. El modal DEBE (MUST) mostrar el **número** del puesto elegido (`Desk.number`), nunca su identificador interno. La elección de puesto es opcional: sin puesto elegido la solicitud se envía sin `resourceId` (comportamiento actual).

#### Scenario: El empleado abre el selector de puesto desde el modal
- **GIVEN** un empleado en el modal de solicitud con el recurso PUESTO seleccionado y una fecha dentro de la ventana
- **WHEN** pulsa el botón "Seleccionar puesto"
- **THEN** se abre el plano de la planta como selector para esa fecha
- **AND** el plano muestra los puestos con su disponibilidad para la fecha elegida

#### Scenario: Al elegir un puesto se muestra su número en el modal
- **GIVEN** el selector de plano abierto desde el modal de solicitud
- **WHEN** el empleado pincha un puesto libre en el plano
- **THEN** el selector se cierra y el modal muestra el **número** del puesto elegido (`Desk.number`)
- **AND** el modal no muestra en ningún momento el identificador interno del puesto

#### Scenario: El envío incluye el resourceId del puesto elegido
- **GIVEN** un empleado con un puesto elegido en el modal para una fecha dentro de la ventana
- **WHEN** confirma el envío de la solicitud
- **THEN** el sistema envía `POST /requests` con `resourceType = DESK` y `resourceId` igual al puesto elegido

#### Scenario: Solicitud de puesto sin elegir puesto concreto
- **GIVEN** un empleado con el recurso PUESTO seleccionado que no ha elegido ningún puesto en el plano
- **WHEN** confirma el envío de la solicitud
- **THEN** el sistema envía `POST /requests` con `resourceType = DESK` y sin `resourceId` (la solicitud nace `PENDING` en modo manual, como hasta ahora)

### Requirement: Auto-aprobación del puesto elegido en modo AUTOMATIC desde el modal
Cuando `approvalMode = AUTOMATIC` y la solicitud de PUESTO incluye un `resourceId`, el sistema DEBE (MUST) crear la solicitud directamente en estado `APPROVED` con ese puesto (siempre que esté disponible para la fecha), reflejando en la UI el feedback de asignación inmediata; en modo `MANUAL` el `resourceId` se ignora y la solicitud nace `PENDING`.

#### Scenario: Puesto elegido auto-aprobado en modo automático
- **GIVEN** `approvalMode = AUTOMATIC`, un empleado autenticado y un puesto disponible para `requested_date`
- **WHEN** envía `POST /requests` con `resourceType = DESK` y el `resourceId` del puesto elegido
- **THEN** el sistema responde 201 con la solicitud en estado `APPROVED` y el puesto elegido asignado

#### Scenario: Puesto elegido no disponible en modo automático
- **GIVEN** `approvalMode = AUTOMATIC` y un puesto ya ocupado (`ASSIGNED`, `REQUESTED` o `APPROVED`) para `requested_date`
- **WHEN** el empleado envía `POST /requests` con `resourceType = DESK` y el `resourceId` de ese puesto
- **THEN** el sistema responde 409 por disponibilidad y no crea la solicitud

#### Scenario: El resourceId se ignora en modo manual
- **GIVEN** `approvalMode = MANUAL` y un empleado que elige un puesto concreto en el modal
- **WHEN** envía `POST /requests` con `resourceType = DESK` y `resourceId`
- **THEN** el sistema responde 201 con la solicitud en estado `PENDING` y `resource_id = NULL` (el ADMIN resolverá la asignación)
