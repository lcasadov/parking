## MODIFIED Requirements

### Requirement: Presentación del plano acorde al design-system
**La vista del plano DEBE (MUST) renderizar cada puesto en su coordenada propia (sin apilamiento por defecto) sobre la imagen real de la oficina (`floor-plan.png`), con tratamiento visual dependiente del tema (blueprint invertido en oscuro, atenuado en claro), usar únicamente colores del design-system dark-premium (sin hex sueltos) y pesos tipográficos 400/500 (nunca 700), y distinguir los puestos `EXECUTIVE` con anillo ámbar y el símbolo ◆. Los marcadores DEBEN (MUST) mostrar un halo y una transición de resorte al pasar el cursor, y el ocupante de un puesto asignado DEBE (MUST) representarse con un pin de avatar. La vista NO DEBE (MUST NOT) mostrar una leyenda flotante superpuesta al plano (los chips de filtro cumplen esa función).**

#### Scenario: Puestos renderizados sobre la imagen real, sin apilamiento
- **GIVEN** un conjunto de puestos con coordenadas distintas
- **WHEN** el usuario abre el plano para una fecha válida
- **THEN** cada puesto se dibuja sobre `floor-plan.png` en su propia posición `(coordX, coordY)` como porcentaje del plano
- **AND** ningún grupo de puestos se superpone por compartir la coordenada central por defecto

#### Scenario: Tratamiento visual de la imagen según el tema
- **GIVEN** el plano cargado
- **WHEN** el tema activo es oscuro
- **THEN** la imagen del plano se muestra con tratamiento blueprint (filtro de inversión/matiz)
- **AND** en tema claro se muestra atenuada, sin el filtro de inversión

#### Scenario: Marcador con estilo premium y ocupante visible
- **GIVEN** un puesto en cualquier estado (`FREE`, `ASSIGNED`, `REQUESTED`, `MINE`, `RELEASED`)
- **WHEN** se renderiza su marcador
- **THEN** el color de fondo y texto provienen de la paleta pastel semántica del design-system dark-premium
- **AND** un puesto `EXECUTIVE` muestra el anillo ámbar y el símbolo ◆ en marcador y leyenda
- **AND** si el puesto está asignado, se muestra un pin de avatar del ocupante junto al marcador

#### Scenario: Sin leyenda flotante superpuesta
- **GIVEN** el plano cargado con puestos en varios estados
- **WHEN** el usuario visualiza el plano
- **THEN** no existe un panel de leyenda flotante superpuesto sobre la imagen
- **AND** los chips de filtro por estado (con sus contadores) son el único referente de leyenda

### Requirement: Edición de posiciones de puestos (editor de arrastre)
**El sistema DEBE (MUST) permitir al `ADMIN` persistir las coordenadas relativas de un puesto, validando que están en el rango 0-100, y rechazar la operación a un `EMPLOYEE`. La edición en el frontend DEBE (MUST) ser no destructiva: entrar en modo edición DEBE (MUST) sustituir el botón "Editar posiciones" por las acciones "Cancelar" y "Guardar cambios"; los arrastres DEBEN (MUST) bufferizarse en el cliente sin persistir hasta que el admin confirme "Guardar cambios"; "Cancelar" DEBE (MUST) descartar el buffer y revertir cada marcador a su posición original.**

#### Scenario: Admin reposiciona un puesto (contrato de API sin cambios)
- **GIVEN** un usuario `ADMIN` autenticado y un `Desk` existente
- **WHEN** confirma "Guardar cambios" tras arrastrar el marcador a una posición dentro de 0-100
- **THEN** el sistema envía `PUT /floor-plan/desks/{deskId}/position` con `{ coordX, coordY }` y persiste `coord_x`/`coord_y` en el `Desk`
- **AND** responde 204

#### Scenario: Arrastre en curso no se persiste hasta guardar
- **GIVEN** un `ADMIN` en modo edición que arrastra uno o más marcadores
- **WHEN** el arrastre concluye pero el admin no ha pulsado "Guardar cambios"
- **THEN** ninguna llamada a `PUT /floor-plan/desks/{deskId}/position` se ha realizado
- **AND** la nueva posición solo existe en el estado local del cliente

#### Scenario: Cancelar revierte al original
- **GIVEN** un `ADMIN` en modo edición con marcadores movidos localmente
- **WHEN** pulsa "Cancelar"
- **THEN** todos los marcadores vuelven a su posición original (la última persistida)
- **AND** el modo edición se cierra sin llamadas al backend

#### Scenario: Coordenadas fuera de rango
- **GIVEN** un usuario `ADMIN` autenticado
- **WHEN** envía `PUT /floor-plan/desks/{deskId}/position` con `coordX = 150` (o un valor negativo)
- **THEN** el sistema responde 400 con `error` de validación y `fields` indicando `coordX`/`coordY`
- **AND** no modifica el puesto

#### Scenario: Empleado intenta editar posiciones
- **GIVEN** un usuario `EMPLOYEE` autenticado
- **WHEN** envía `PUT /floor-plan/desks/{deskId}/position`
- **THEN** el sistema responde 403 (autorización denegada)
- **AND** no modifica el puesto

## ADDED Requirements

### Requirement: Tooltip del marcador sin recorte ni escalado con el zoom
El tooltip que aparece al pasar el cursor sobre un marcador DEBE (MUST) renderizarse fuera de la capa clipada/transformada del plano (para no recortarse en los bordes ni escalar con el zoom aplicado a la superficie), y DEBE (MUST) ajustar su posición horizontal (clamp) y verticalmente (flip) según el espacio disponible respecto al viewport.

#### Scenario: Tooltip en un marcador cerca del borde
- **GIVEN** un marcador ubicado cerca del borde derecho o inferior del plano
- **WHEN** el usuario pasa el cursor sobre él
- **THEN** el tooltip se muestra completo (sin recortarse) ajustando su posición horizontal/vertical
- **AND** su tamaño no varía con el nivel de zoom aplicado al plano
