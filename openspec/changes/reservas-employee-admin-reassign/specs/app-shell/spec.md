## ADDED Requirements

### Requirement: Marca de la aplicación "Reservas"
La aplicación SHALL mostrarse con el nombre "Reservas" (con "ALEATICA" como subtítulo) en el
sidebar y en el título del documento, en lugar de "parking".

#### Scenario: Marca en el sidebar
- **WHEN** el usuario ve el sidebar
- **THEN** la marca muestra "Reservas" y debajo "ALEATICA"

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
