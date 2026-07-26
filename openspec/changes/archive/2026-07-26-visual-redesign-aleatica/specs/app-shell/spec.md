## MODIFIED Requirements

### Requirement: Sidebar con nav-items del design-system
El sidebar (escritorio) y el drawer off-canvas (móvil) DEBEN (MUST) ser el **único** chrome de la aplicación: alojan el lockup de marca (símbolo ALEATICA + "parking" + "ALEATICA"), la navegación (`nav-item`s agrupados por sección), el CTA destacado "Nueva reserva", los controles globales (idioma, tema) y la tarjeta de usuario. La aplicación NO DEBE (MUST NOT) presentar una barra superior (header/topbar) de escritorio con marca, curva decorativa o controles: el área de contenido queda libre para el título de la pantalla.

#### Scenario: Sidebar de escritorio con todos los slots
- **GIVEN** un usuario autenticado en escritorio
- **WHEN** se renderiza el sidebar
- **THEN** muestra, de arriba a abajo: lockup de marca, CTA "Nueva reserva" (si el rol puede reservar), navegación por secciones, controles de idioma/tema y la tarjeta de usuario
- **AND** no existe ninguna barra superior de escritorio con marca, curva o controles duplicados

#### Scenario: Ítem activo del sidebar
- **GIVEN** el usuario en una ruta del sidebar
- **WHEN** se renderiza el sidebar
- **THEN** el ítem de esa ruta aparece con fondo acento-suave y borde izquierdo de acento, con su icono en el color de acento
- **AND** los demás ítems se muestran con icono + etiqueta en color muted, sin subrayado

#### Scenario: Badge de solicitudes pendientes
- **GIVEN** hay solicitudes pendientes
- **WHEN** se renderiza el sidebar
- **THEN** el ítem "Solicitudes" muestra un badge con el número de pendientes

### Requirement: Modales con cabecera de color
Los modales DEBEN (MUST) presentar una cabecera de color (acento por defecto; rojo/`--busy` para acciones destructivas como rechazo o eliminación) con el título en blanco, montados sobre la primitiva Radix `Dialog`/`AlertDialog` y conservando el atributo `role="dialog"` para no romper los contratos de test existentes.

#### Scenario: Modal de confirmación destructiva
- **GIVEN** un modal de una acción destructiva (p. ej. rechazar o eliminar)
- **WHEN** se abre
- **THEN** su cabecera se muestra en el color destructivo con el título en blanco
- **AND** el modal expone `role="dialog"` y foco atrapado (comportamiento de Radix `Dialog`)

#### Scenario: Modal estándar
- **GIVEN** un modal de una acción no destructiva (p. ej. crear/aprobar)
- **WHEN** se abre
- **THEN** su cabecera se muestra en el color de acento con el título en blanco

## ADDED Requirements

### Requirement: Chrome fijo con motion en escritorio y móvil
La aplicación DEBE (MUST) mantener el chrome (sidebar en escritorio; header + drawer en móvil) **fijo** respecto al scroll del contenido. En escritorio, el sidebar DEBE (MUST) usar posicionamiento fijo a 100% del viewport vertical. En móvil, DEBE (MUST) ofrecer un header de cristal fijo (marca + botón de menú + avatar) y un drawer off-canvas que entra/sale con una transición de resorte (Framer Motion), bloqueando el scroll del `body` mientras está abierto.

#### Scenario: El sidebar no se desplaza con el contenido
- **GIVEN** un usuario en escritorio en una pantalla con contenido más alto que el viewport
- **WHEN** hace scroll en el área de contenido
- **THEN** el sidebar permanece fijo en su posición

#### Scenario: El drawer móvil bloquea el scroll de fondo
- **GIVEN** un usuario en móvil con el drawer abierto
- **WHEN** intenta hacer scroll sobre el contenido detrás del drawer
- **THEN** el `body` no se desplaza (scroll bloqueado mientras el drawer está abierto)

#### Scenario: El drawer respeta reduced-motion
- **GIVEN** un usuario con `prefers-reduced-motion: reduce`
- **WHEN** abre o cierra el drawer
- **THEN** la transición de entrada/salida se realiza sin la animación de resorte deslizante (duración efectivamente nula)

### Requirement: Cierre del drawer móvil por Escape, scrim o navegación
El drawer off-canvas DEBE (MUST) cerrarse al pulsar Escape, al pulsar el scrim que atenúa el fondo, o automáticamente al navegar a una nueva ruta.

#### Scenario: Escape cierra el drawer
- **GIVEN** el drawer abierto en móvil
- **WHEN** el usuario pulsa Escape
- **THEN** el drawer se cierra y se restaura el scroll del `body`

#### Scenario: Navegar cierra el drawer
- **GIVEN** el drawer abierto en móvil
- **WHEN** el usuario pulsa un destino de navegación dentro del drawer
- **THEN** la ruta cambia y el drawer se cierra automáticamente

### Requirement: CTA "Nueva reserva" condicionado por rol
El sidebar/drawer DEBE (MUST) mostrar un CTA destacado "Nueva reserva" para los roles `ADMIN` y `EMPLOYEE`, y DEBE (MUST NOT) mostrarlo para el rol `AGENCIA` (que solo gestiona liberaciones).

#### Scenario: Admin y empleado ven el CTA
- **GIVEN** un usuario con rol `ADMIN` o `EMPLOYEE`
- **WHEN** se renderiza el sidebar/drawer
- **THEN** el CTA "Nueva reserva" está visible bajo la marca

#### Scenario: Agencia no ve el CTA
- **GIVEN** un usuario con rol `AGENCIA`
- **WHEN** se renderiza el sidebar/drawer
- **THEN** el CTA "Nueva reserva" no se muestra
