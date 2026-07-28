## MODIFIED Requirements

### Requirement: Elección de un puesto concreto en la solicitud unificada
El modal de solicitud unificada DEBE (MUST) permitir al empleado, cuando el recurso seleccionado incluye PUESTO, **elegir un puesto concreto** abriendo el plano de la planta como selector, y DEBE enviar el `resourceId` del puesto elegido en `POST /requests`. El modal DEBE (MUST) mostrar el **número** del puesto elegido (`Desk.number`), nunca su identificador interno. La elección de puesto es opcional: sin puesto elegido la solicitud se envía sin `resourceId` (comportamiento actual). En modo de aprobación `MANUAL`, donde el `resourceId` se ignora, el modal DEBE (MUST) indicar de forma explícita que el puesto elegido es una **preferencia** y no una reserva garantizada, para no crear una expectativa falsa en el empleado.

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

#### Scenario: En modo MANUAL el modal advierte que el puesto es una preferencia
- **GIVEN** `approvalMode = MANUAL` y un empleado que ha elegido un puesto concreto en el modal
- **WHEN** el modal presenta el puesto elegido antes de enviar
- **THEN** la UI indica que la elección es una preferencia y que el ADMIN resolverá la asignación final (el `resourceId` se ignora en el envío)

#### Scenario: El envío incluye el resourceId del puesto elegido
- **GIVEN** un empleado con un puesto elegido en el modal para una fecha dentro de la ventana
- **WHEN** confirma el envío de la solicitud
- **THEN** el sistema envía `POST /requests` con `resourceType = DESK` y `resourceId` igual al puesto elegido

#### Scenario: Solicitud de puesto sin elegir puesto concreto
- **GIVEN** un empleado con el recurso PUESTO seleccionado que no ha elegido ningún puesto en el plano
- **WHEN** confirma el envío de la solicitud
- **THEN** el sistema envía `POST /requests` con `resourceType = DESK` y sin `resourceId` (la solicitud nace `PENDING` en modo manual, como hasta ahora)

## ADDED Requirements

### Requirement: Confirmación al solicitar un puesto desde el plano
La solicitud de un puesto desde el plano (vista de empleado) DEBE (MUST) requerir una confirmación explícita antes de crear la solicitud, para evitar solicitudes accidentales por toques o clics involuntarios sobre el marcador (especialmente en pantallas táctiles con zoom/desplazamiento).

#### Scenario: Pinchar un puesto libre pide confirmación
- **GIVEN** un `EMPLOYEE` en el plano con un puesto libre para la fecha seleccionada
- **WHEN** pincha (o pulsa "Solicitar" en la lista móvil) sobre ese puesto
- **THEN** la UI muestra una confirmación con el puesto y la fecha antes de enviar
- **AND** la solicitud solo se crea si el empleado confirma

#### Scenario: Cancelar la confirmación no crea solicitud
- **GIVEN** un `EMPLOYEE` con la confirmación de solicitud de puesto abierta
- **WHEN** cancela la confirmación
- **THEN** no se envía ninguna solicitud y el plano permanece sin cambios
