# app-shell Specification

## Purpose
TBD - created by archiving change align-app-shell-with-mockups. Update Purpose after archive.
## Requirements
### Requirement: Header con identidad visual del design-system
**El header global DEBE (MUST) reproducir la identidad de los mockups: curva decorativa verde/naranja, logo, marca "Nexo / ALEATICA" (producto sobre empresa), título de la página y avatar del usuario, conservando los controles de idioma, tema, exportación y cierre de sesión.**

#### Scenario: Header en una pantalla de administración
- **GIVEN** un usuario autenticado en cualquier pantalla con chrome
- **WHEN** se renderiza el header
- **THEN** muestra la curva decorativa, el logo, la marca, el título de la pantalla y el avatar con las iniciales del usuario
- **AND** los controles de idioma, tema, exportación y cierre de sesión siguen accesibles

### Requirement: Sidebar con nav-items del design-system
**El sidebar DEBE (MUST) mostrar cada entrada como un `nav-item` (icono + etiqueta) con estado activo resaltado (fondo verde-soft y borde izquierdo verde), sin aspecto de enlace subrayado, y un badge de conteo en Solicitudes.**

#### Scenario: Ítem activo del sidebar
- **GIVEN** el usuario en una ruta del sidebar
- **WHEN** se renderiza el sidebar
- **THEN** el ítem de esa ruta aparece con fondo verde-soft, borde izquierdo verde y su icono en verde
- **AND** los demás ítems se muestran con icono + etiqueta en color muted, sin subrayado

#### Scenario: Badge de solicitudes pendientes
- **GIVEN** hay solicitudes pendientes
- **WHEN** se renderiza el sidebar
- **THEN** el ítem "Solicitudes" muestra un badge rojo con el número de pendientes

### Requirement: Modales con cabecera de color
**Los modales DEBEN (MUST) presentar una cabecera de color (verde por defecto; roja para acciones destructivas como rechazo o eliminación) con el título en blanco, según los mockups.**

#### Scenario: Modal de confirmación destructiva
- **GIVEN** un modal de una acción destructiva (p. ej. rechazar o eliminar)
- **WHEN** se abre
- **THEN** su cabecera se muestra en rojo con el título en blanco

#### Scenario: Modal estándar
- **GIVEN** un modal de una acción no destructiva (p. ej. crear/aprobar)
- **WHEN** se abre
- **THEN** su cabecera se muestra en verde con el título en blanco

### Requirement: Destino de la CTA "Nueva reserva" según el rol
La CTA "Nueva reserva" del topbar DEBE (MUST) abrir una superficie distinta según el rol de quien la pulsa: el `ADMIN` DEBE (MUST) abrir el asistente de reserva multipaso (capability `reservation-wizard`), y el `EMPLOYEE` DEBE (MUST) abrir su propio modal de solicitud. Esta decisión de destino es independiente de la visibilidad del CTA por rol (ya cubierta por su propia capability de UI).

#### Scenario: El admin abre el asistente de reserva
- **GIVEN** un `ADMIN` autenticado
- **WHEN** pulsa "Nueva reserva"
- **THEN** se abre el asistente de reserva multipaso (no el modal de solicitud propia)

#### Scenario: El empleado abre su modal de solicitud propia
- **GIVEN** un `EMPLOYEE` autenticado
- **WHEN** pulsa "Nueva reserva"
- **THEN** se abre su modal de solicitud propia (no el asistente de reserva del admin)

### Requirement: Los modales no se cierran con la tecla Escape
Los modales de la aplicación (`Dialog` sobre Radix, y los modales legacy `Modal`/`ConfirmDialog`) NO DEBEN (MUST NOT) cerrarse al pulsar Escape, para evitar cierres accidentales en flujos largos (p. ej. el asistente de reserva multipaso). DEBEN (MUST) seguir cerrándose mediante el botón de cerrar (x), una acción del pie del modal, o un clic en el fondo (overlay).

#### Scenario: Escape no cierra un diálogo Radix
- **GIVEN** cualquier `Dialog` abierto (p. ej. el asistente de reserva)
- **WHEN** el usuario pulsa Escape
- **THEN** el diálogo permanece abierto

#### Scenario: Escape no cierra un modal legacy
- **GIVEN** cualquier `Modal`/`ConfirmDialog` legacy abierto
- **WHEN** el usuario pulsa Escape
- **THEN** el modal permanece abierto

#### Scenario: El modal se sigue cerrando por la (x), el pie o el overlay
- **GIVEN** cualquier modal o diálogo abierto
- **WHEN** el usuario pulsa el botón de cerrar, una acción de cierre del pie, o hace clic fuera del panel
- **THEN** el modal se cierra con normalidad

### Requirement: Marca de la aplicación "Nexo"
La aplicación SHALL mostrarse con el nombre de producto "Nexo" (con "ALEATICA" como subtítulo)
en el sidebar, y con "Nexo" como título del documento. (Ver la capability `app-branding` para
el detalle de superficies y la separación producto/empresa.)

#### Scenario: Marca en el sidebar
- **WHEN** el usuario ve el sidebar
- **THEN** la marca muestra "Nexo" y debajo "ALEATICA"

### Requirement: Cierre de sesión con confirmación
Al accionar cerrar sesión desde la píldora de perfil, el sistema SHALL mostrar un diálogo de
confirmación ("¿Seguro que deseas cerrar sesión?") con opciones cancelar/cerrar sesión, en
lugar de abrir el menú de perfil.

#### Scenario: Confirmar cierre de sesión
- **WHEN** el usuario acciona cerrar sesión
- **THEN** aparece un diálogo de confirmación y la sesión solo se cierra al confirmar

### Requirement: Sin popover de perfil redundante ni exportar
El menú/popover de perfil NO SHALL duplicar los controles de idioma y tema (ya presentes en el
pie del sidebar) ni ofrecer "Exportar mis datos".

#### Scenario: Perfil sin duplicados
- **WHEN** el usuario interactúa con su perfil en el sidebar
- **THEN** no ve un popover que repita idioma/tema ni la opción "Exportar mis datos"

