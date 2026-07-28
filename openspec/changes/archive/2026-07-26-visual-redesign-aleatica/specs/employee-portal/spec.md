## MODIFIED Requirements

### Requirement: Portal del empleado con identidad ALEATICA
Las pantallas del Portal del Empleado (móvil-first y escritorio) DEBEN (MUST) seguir la identidad visual dark-premium ALEATICA usando los patrones compartidos y las variantes móviles, sin cambiar la lógica de negocio. "Mi Semana" DEBE (MUST) presentar cada día como una tarjeta con una entrada animada **escalonada** (stagger) respetando `prefers-reduced-motion`. Los modales del portal (solicitud, liberación, confirmación de puesto) DEBEN (MUST) montarse sobre la primitiva Radix `Dialog`, conservando `role="dialog"`.

#### Scenario: Mi semana con tarjetas y motion escalonado
- **WHEN** el empleado abre "Mi semana"
- **THEN** cada día se muestra con una tarjeta cuyo estado usa el mapa estado→color
- **AND** las tarjetas entran con una animación escalonada (cada una con un pequeño retraso respecto a la anterior), salvo que el usuario tenga `prefers-reduced-motion` activado
- **AND** la botonera Solicitar/Liberar usa los estilos de botón del design system

#### Scenario: Solicitud unificada
- **WHEN** el empleado solicita plaza y/o puesto
- **THEN** los bloques de recurso, los estados de error/validación y el botón de envío siguen los patrones comunes
- **AND** el modal de solicitud está montado sobre Radix `Dialog` (`role="dialog"`)
- **AND** el envío usa los endpoints existentes (una solicitud por recurso)

#### Scenario: Listados del empleado como tarjetas en móvil
- **GIVEN** el empleado en viewport móvil sobre "Mis solicitudes" o "Mis plazas"
- **WHEN** la lista se renderiza
- **THEN** cada elemento se presenta como tarjeta apilada (solo CSS, sin cambio de datos ni de comportamiento) en vez de columnas de tabla

### Requirement: Plano del empleado consistente con el de admin
El plano del empleado (escritorio y móvil) DEBE (MUST) reutilizar el patrón de plano dark-premium (imagen real de la oficina + marcadores por coordenadas coloreados por estado, sin leyenda flotante) del design system, incluyendo el `PageHeader` común de la pantalla.

#### Scenario: Puestos disponibles
- **WHEN** el empleado ve el plano de una fecha
- **THEN** los marcadores libres se distinguen por color y el panel/lista de disponibles usa el patrón común
- **AND** la pantalla presenta el mismo `PageHeader` que el resto del portal
