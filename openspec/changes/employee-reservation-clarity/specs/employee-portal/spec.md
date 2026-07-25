## ADDED Requirements

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
