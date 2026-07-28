# employee-portal Specification

## Purpose
TBD - created by archiving change redesign-remaining-screens. Update Purpose after archive.
## Requirements
### Requirement: Portal del empleado con identidad ALEATICA
Las pantallas del Portal del Empleado (móvil-first y escritorio) DEBEN (MUST) seguir la identidad
ALEATICA usando los patrones compartidos y las variantes móviles, sin cambiar la lógica.

#### Scenario: Mi semana
- **WHEN** el empleado abre "Mi semana"
- **THEN** cada día se muestra con una tarjeta cuyo estado usa el mapa estado→color
- **AND** la botonera Solicitar/Liberar usa los estilos de botón del design system

#### Scenario: Solicitud unificada
- **WHEN** el empleado solicita plaza y/o puesto
- **THEN** los bloques de recurso, los estados de error/validación y el botón de envío
  siguen los patrones comunes
- **AND** el envío usa los endpoints existentes (una solicitud por recurso)

### Requirement: Plano del empleado consistente con el de admin
El plano del empleado (escritorio y móvil) DEBE (MUST) reutilizar el patrón de plano
(imagen real + marcadores por coordenadas coloreados por estado) del design system.

#### Scenario: Puestos disponibles
- **WHEN** el empleado ve el plano de una fecha
- **THEN** los marcadores libres se distinguen por color y el panel/lista de disponibles
  usa el patrón común

### Requirement: Aviso de resultado esperado en la solicitud unificada
El modal de solicitud unificada DEBE (MUST), consultando el modo de aprobación vigente, mostrar al empleado un aviso explícito de qué resultado esperar antes de enviar: en modo `AUTOMATIC`, un aviso indicando que la solicitud **se confirmará al instante**; en modo `MANUAL`, un aviso indicando que la solicitud **quedará pendiente de aprobación** y que la plaza o el puesto exacto **puede cambiar**.

#### Scenario: Aviso de confirmación instantánea en modo automático
- **GIVEN** `approvalMode = AUTOMATIC` y un empleado con el modal de solicitud abierto
- **WHEN** el modal resuelve el modo vigente
- **THEN** muestra un aviso indicando que la solicitud se confirmará al instante

#### Scenario: Aviso de pendiente en modo manual
- **GIVEN** `approvalMode = MANUAL` y un empleado con el modal de solicitud abierto
- **WHEN** el modal resuelve el modo vigente
- **THEN** muestra un aviso indicando que la solicitud quedará pendiente de aprobación y que la ubicación exacta puede cambiar

### Requirement: "Mi Semana" con héroe HOY/MAÑANA accionable
El portal del empleado DEBE (MUST) mostrar, encima de la tira semanal navegable de "Mi Semana", un héroe con dos tarjetas (HOY y MAÑANA) que resuman de un vistazo el estado de la plaza y del puesto del empleado para cada uno de esos dos días. Cuando un recurso del héroe esté libre, la UI DEBE (MUST) ofrecer una acción de reserva en un toque que abra el modal de solicitud con la fecha y el tipo de recurso ya preseleccionados. Cuando un recurso del héroe tenga una solicitud `PENDING`, la UI DEBE (MUST) mostrar en línea el aviso de pendiente. Cuando un recurso del héroe esté ocupado (asignado o con solicitud propia liberable), la UI DEBE (MUST) ofrecer la acción de liberar/cancelar correspondiente en el mismo toque.

#### Scenario: El héroe muestra plaza y puesto de HOY y de MAÑANA por separado
- **GIVEN** un `EMPLOYEE` con estados distintos de plaza y puesto para hoy y para mañana
- **WHEN** abre "Mi Semana"
- **THEN** la tarjeta HOY y la tarjeta MAÑANA muestran, cada una, el estado de la plaza y el estado del puesto de forma independiente

#### Scenario: Reserva en 1 toque desde un recurso libre del héroe
- **GIVEN** un `EMPLOYEE` en el héroe de "Mi Semana" con el puesto de MAÑANA libre
- **WHEN** pulsa la acción de reserva sobre ese recurso
- **THEN** se abre el modal de solicitud con la fecha de mañana y el tipo de recurso puesto ya preseleccionados

#### Scenario: Aviso de pendiente en línea en el héroe
- **GIVEN** un `EMPLOYEE` con una solicitud `PENDING` de plaza para hoy
- **WHEN** ve la tarjeta HOY del héroe
- **THEN** el recurso plaza de esa tarjeta muestra en línea el aviso de pendiente

#### Scenario: Liberación en 1 toque desde el héroe
- **GIVEN** un `EMPLOYEE` con un recurso del héroe ocupado y liberable (asignación fija futura o solicitud propia cancelable)
- **WHEN** pulsa la acción de liberar/cancelar sobre ese recurso
- **THEN** se ofrece el mecanismo de liberación correspondiente (liberación de recurso fijo o cancelación de la solicitud) para esa fecha y recurso

### Requirement: Acción principal de reserva siempre visible
El portal del empleado DEBE (MUST) mantener una acción principal de "Reservar" siempre visible en "Mi Semana", independiente del héroe y de la tira semanal, que abra el modal de solicitud sin preselección.

#### Scenario: La acción de reservar está siempre disponible
- **GIVEN** un `EMPLOYEE` en "Mi Semana", con independencia del contenido del héroe o de la tira semanal
- **WHEN** la pantalla está renderizada
- **THEN** la acción principal "Reservar" está visible y disponible
- **AND** al pulsarla se abre el modal de solicitud sin fecha ni recurso preseleccionados

### Requirement: Comunicación honesta de la falta de hueco y de la lista de espera
El portal del empleado DEBE (MUST) comunicar con claridad que, aunque un día aparezca sin recursos libres, puede solicitarlo igualmente porque los recursos se liberan con frecuencia (ausencias, vacaciones, cancelaciones). Cuando la disponibilidad del día/tipo es 0, el modal de reserva DEBE (MUST) explicar la situación y ofrecer **apuntarse a la lista de espera** en lugar de bloquear la acción; tras un `409 NO_AVAILABILITY` en modo AUTOMÁTICO, DEBE (MUST) permitir reintentar con `waitlist: true`.

#### Scenario: Sin hueco, el modal ofrece apuntarse en vez de bloquear
- **GIVEN** un empleado en el modal de reserva y un día/tipo con 0 disponibilidad
- **WHEN** ve la disponibilidad del día
- **THEN** el modal muestra un aviso honesto (los recursos se liberan a menudo) y ofrece "apuntarme a la lista de espera"
- **AND** no impide enviar la solicitud

#### Scenario: Reintento como lista de espera tras 409
- **GIVEN** modo AUTOMÁTICO y un envío que devolvió `409 NO_AVAILABILITY`
- **WHEN** el empleado confirma apuntarse a la lista de espera
- **THEN** la UI reenvía la solicitud con `waitlist: true` y muestra el resultado en espera

### Requirement: Estado "en lista de espera" visible en Mi Semana y Mis solicitudes
El portal DEBE (MUST) mostrar un distintivo "En lista de espera" para las solicitudes `PENDING` con `waitlisted = true`, tanto en "Mi Semana" (héroe HOY/MAÑANA y tira semanal) como en "Mis solicitudes". NO DEBE (MUST NOT) mostrar una posición numérica en la cola. `MyWeekDay` DEBE (MUST) exponer el estado de espera del recurso para poder pintarlo.

#### Scenario: Chip de espera en Mis solicitudes
- **GIVEN** una solicitud `PENDING` con `waitlisted = true`
- **WHEN** el empleado abre "Mis solicitudes"
- **THEN** esa fila muestra el distintivo "En lista de espera"
- **AND** no muestra ninguna posición numérica

#### Scenario: Chip de espera en Mi Semana
- **GIVEN** un día del héroe HOY/MAÑANA cuyo recurso está en lista de espera
- **WHEN** el empleado ve la tarjeta de ese día
- **THEN** el recurso muestra el estado "En lista de espera"

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

