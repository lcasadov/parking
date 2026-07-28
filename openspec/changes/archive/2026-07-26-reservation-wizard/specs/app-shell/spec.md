## ADDED Requirements

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
