# app-shell Specification

## Purpose
TBD - created by archiving change align-app-shell-with-mockups. Update Purpose after archive.
## Requirements
### Requirement: Header con identidad visual del design-system
**El header global DEBE (MUST) reproducir la identidad de los mockups: curva decorativa verde/naranja, logo, marca "parking / ALEATICA", título de la página y avatar del usuario, conservando los controles de idioma, tema, exportación y cierre de sesión.**

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
