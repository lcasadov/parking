## ADDED Requirements

### Requirement: Acceso al mapa del puesto desde Mi Semana
En Mi Semana, cada tarjeta con un puesto asignado SHALL ofrecer un botón "Mapa" que abre el
plano enfocado en ese puesto, resaltándolo con una animación (que respeta
`prefers-reduced-motion`). Disponible en todas las tarjetas (Hoy, Mañana y Próximos días).

#### Scenario: Ver mi puesto en el plano
- **WHEN** el empleado pulsa "Mapa" en una tarjeta con puesto asignado
- **THEN** se abre el plano con ese puesto resaltado y en primer plano

### Requirement: Cómo llegar al parking desde Mi Semana
En Mi Semana, las tarjetas de Hoy y Mañana con plaza reservada SHALL ofrecer un botón "Ir al
parking" que abre la navegación externa hacia la dirección del parking configurada por el admin.

#### Scenario: Navegar al parking hoy
- **WHEN** el empleado pulsa "Ir al parking" en la tarjeta de Hoy con plaza reservada
- **THEN** se abre la navegación externa con la dirección del parking precargada

### Requirement: Mis solicitudes navegable por meses con doble fecha
La pantalla "Mis solicitudes" SHALL mostrarse por mes (mes actual por defecto, navegable
adelante y atrás sin tope, filtrando por fecha de recurso), mostrando tanto la fecha de
recurso ("Día reservado") como la fecha de solicitud ("Solicitado el"), y SHALL omitir los
botones de exportar y de "Nueva solicitud".

#### Scenario: Navegar a un mes futuro
- **WHEN** el empleado avanza el selector a un mes con reservas futuras
- **THEN** se listan las solicitudes de ese mes por fecha de recurso, con ambas fechas visibles

#### Scenario: Sin exportar ni nueva solicitud
- **WHEN** el empleado abre "Mis solicitudes"
- **THEN** no se muestran botones de exportar ni de "Nueva solicitud"

### Requirement: Acción por estado en Mis solicitudes
En "Mis solicitudes", la acción de cada fila SHALL depender del estado: una solicitud PENDING
ofrece "Cancelar solicitud" y una APPROVED ofrece "Liberar", con el modal de confirmación y el
toast coherentes con la acción (cancelada vs liberada, con el recurso).

#### Scenario: Liberar una aprobada
- **WHEN** el empleado pulsa la acción en una solicitud APPROVED
- **THEN** el modal habla de liberar y, al confirmar, el toast dice "Plaza/Puesto liberado"

#### Scenario: Cancelar una pendiente
- **WHEN** el empleado pulsa la acción en una solicitud PENDING
- **THEN** el modal habla de cancelar y, al confirmar, el toast dice "Solicitud cancelada"

### Requirement: Mis sitios fijos informativa
La sección antes llamada "Mis plazas" SHALL renombrarse a "Mis sitios fijos", ser de solo
lectura, y mostrar una fila por recurso fijo (plaza o puesto) con los días que aplica y su
icono por tipo; las filas de puesto SHALL ofrecer el botón "Mapa".

#### Scenario: Un recurso distinto por día
- **WHEN** el empleado tiene un puesto los lunes y otro los miércoles
- **THEN** se muestran dos filas, una por recurso, con sus días respectivos

### Requirement: Sin sección Plano en el empleado
La navegación del empleado NO SHALL incluir una sección "Plano"; el acceso al plano se hace
de forma contextual (botón "Mapa" en las tarjetas).

#### Scenario: Nav del empleado sin Plano
- **WHEN** el empleado ve su navegación
- **THEN** no aparece la entrada "Plano"
