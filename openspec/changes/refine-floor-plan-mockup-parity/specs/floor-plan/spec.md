# floor-plan Specification (delta)

## ADDED Requirements
### Requirement: Presentación del plano acorde al design-system
**La vista del plano DEBE (MUST) renderizar cada puesto en su coordenada propia (sin apilamiento por defecto), usar únicamente colores del design-system (sin hex sueltos) y pesos tipográficos 400/500 (nunca 700), y distinguir los puestos `EXECUTIVE` con anillo ámbar y el símbolo ◆.**

#### Scenario: Puestos renderizados sin apilamiento
- **GIVEN** un conjunto de puestos con coordenadas distintas
- **WHEN** el usuario abre el plano para una fecha válida
- **THEN** cada puesto se dibuja en su propia posición `(coordX, coordY)` como porcentaje del plano
- **AND** ningún grupo de puestos se superpone por compartir la coordenada central por defecto

#### Scenario: Marcador con estilo del design-system
- **GIVEN** un puesto en cualquier estado (`FREE`, `ASSIGNED`, `REQUESTED`, `MINE`, `RELEASED`)
- **WHEN** se renderiza su marcador
- **THEN** el color de fondo y texto provienen de la paleta pastel semántica del design-system
- **AND** un puesto `EXECUTIVE` muestra el anillo ámbar y el símbolo ◆ en marcador y leyenda

### Requirement: Controles del plano (fecha, filtros, zoom, panel lateral)
**El plano DEBE (MUST) ofrecer navegación de fecha (día anterior/siguiente y "Hoy") dentro de la ventana de reserva, filtros por estado con contadores, controles de zoom, y un panel lateral con la lista de puestos buscable.**

#### Scenario: Navegación de fecha recarga el plano
- **GIVEN** el plano abierto para una fecha
- **WHEN** el usuario pulsa "día siguiente" (dentro de la ventana hoy..+14d)
- **THEN** el plano recarga los estados de los puestos para la nueva fecha

#### Scenario: Filtro por estado con contador
- **GIVEN** el plano con puestos en varios estados
- **WHEN** el usuario activa el chip de un estado (p. ej. "Libre")
- **THEN** el plano resalta/filtra los puestos de ese estado
- **AND** cada chip muestra el número de puestos en ese estado

#### Scenario: Buscar un puesto en el panel lateral
- **GIVEN** el panel lateral con la lista de puestos
- **WHEN** el usuario escribe un número de puesto en la búsqueda
- **THEN** la lista se filtra a los puestos coincidentes

### Requirement: Plano usable en móvil
**En viewport móvil el plano DEBE (MUST) permitir interacción táctil (pan/zoom) y ofrecer una lista "Disponibles para solicitar" con un botón de solicitud por fila, de modo que el empleado pueda solicitar un puesto sin depender de pulsar marcadores diminutos.**

#### Scenario: Solicitud desde la lista móvil
- **GIVEN** un empleado en viewport móvil con puestos libres para la fecha
- **WHEN** pulsa "Solicitar" en la fila de un puesto libre de la lista
- **THEN** se crea la solicitud del puesto para esa fecha (mismo flujo que pulsar el marcador)
- **AND** recibe feedback de éxito o de conflicto

#### Scenario: Reposicionar un puesto por gesto táctil (admin)
- **GIVEN** un `ADMIN` en modo edición en viewport móvil
- **WHEN** arrastra un marcador con un gesto táctil
- **THEN** la nueva posición se persiste igual que con el ratón

### Requirement: Disponibilidad en la solicitud unificada
**El modal de solicitud unificada DEBE (MUST) mostrar, para cada recurso seleccionable (plaza y puesto), un indicador de disponibilidad para la fecha elegida antes de enviar.**

#### Scenario: Indicador de disponibilidad por recurso
- **GIVEN** un empleado en el modal de solicitud con una fecha elegida
- **WHEN** el modal consulta la disponibilidad de plaza y de puesto para esa fecha
- **THEN** muestra junto a cada opción un banner informativo con la disponibilidad
- **AND** el envío sigue permitido (el banner es informativo, la validación final la hace el backend)
