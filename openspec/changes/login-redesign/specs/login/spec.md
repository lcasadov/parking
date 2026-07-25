## MODIFIED Requirements

### Requirement: Presentación del login
La pantalla de login DEBE (MUST) seguir la identidad ALEATICA mediante una composición centrada de marca —fondo con orbes de color derivando, spotlight que sigue al cursor, logo mini de Aleatica flotante, titular en degradado ("Gestión de plazas y puestos") y subtítulo de marca— con un formulario compacto (usuario/contraseña con icono dentro del campo, mostrar/ocultar contraseña, CTA "ENTRAR") dentro de una card centrada, sin alterar el flujo de autenticación existente. Todo el movimiento (orbes, spotlight, tilt del logo, brillo del CTA, degradado del titular) DEBE (MUST) desactivarse bajo `prefers-reduced-motion: reduce`; el spotlight y el tilt del logo DEBEN (MUST) además desactivarse en dispositivos sin puntero fino (`pointer: fine` falso, p. ej. móvil/táctil).

#### Scenario: Acceso correcto
- **GIVEN** un usuario con credenciales válidas en la pantalla de login
- **WHEN** las introduce y pulsa "ENTRAR"
- **THEN** se ejecuta el mismo flujo de autenticación que antes del rediseño
- **AND** la UI usa el logo, la paleta de marca y la tipografía en degradado del titular

#### Scenario: Error de credenciales
- **GIVEN** un usuario en la pantalla de login
- **WHEN** introduce credenciales inválidas
- **THEN** se muestra un banner de error dentro de la card con los intentos restantes
- **AND** tras 5 intentos fallidos se comunica el bloqueo temporal de 15 minutos

#### Scenario: Composición de marca en escritorio con puntero fino
- **GIVEN** un usuario con puntero fino (ratón) y sin preferencia de movimiento reducido
- **WHEN** mueve el cursor sobre la pantalla de login
- **THEN** el spotlight radial de fondo sigue la posición del cursor
- **AND** el logo mini flotante aplica un tilt 3D sutil hacia el cursor

#### Scenario: Movimiento reducido o puntero no fino desactiva el motion
- **GIVEN** un usuario con `prefers-reduced-motion: reduce` activado, o un dispositivo sin puntero fino (móvil/táctil)
- **WHEN** carga o interactúa con la pantalla de login
- **THEN** los orbes de fondo, el brillo del CTA y el degradado del titular no animan
- **AND** el spotlight y el tilt del logo tampoco reaccionan al movimiento (no se registra el listener de puntero)

#### Scenario: Tema claro y oscuro
- **GIVEN** un usuario en la pantalla de login
- **WHEN** el body tiene la clase `theme-dark`
- **THEN** la composición adopta la paleta oscura de marca (fondo, card, campos y autofill de Chrome incluidos) sin perder contraste ni legibilidad
