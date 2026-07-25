## MODIFIED Requirements

### Requirement: Controles del plano (fecha, filtros, zoom, panel lateral)
El plano DEBE (MUST) ofrecer navegación de fecha (día anterior/siguiente y "Hoy") desde hoy en adelante (sin tope superior; no se navega a fechas pasadas), filtros por estado con contadores, y un panel lateral con la lista de puestos buscable. Los controles de zoom DEBEN (MUST) incluir, además de los botones +/-/reset, **zoom con la rueda del ratón o trackpad** y **zoom con doble clic**, ambos centrados en el punto señalado por el cursor (el doble clic, si el plano ya está ampliado, restaura el encuadre en su lugar). El plano DEBE (MUST) ofrecer además un botón de **maximizado** que amplíe el lienzo dentro de la propia aplicación (overlay interno, no la Fullscreen API nativa del navegador), de modo que cualquier diálogo modal abierto mientras el plano está maximizado (p. ej. una confirmación de asignación) se siga mostrando por encima del plano.

#### Scenario: Navegación de fecha recarga el plano
- **GIVEN** el plano abierto para una fecha
- **WHEN** el usuario pulsa "día siguiente" (hoy o cualquier fecha futura)
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

#### Scenario: Zoom con la rueda del ratón hacia el cursor
- **GIVEN** el plano cargado
- **WHEN** el usuario gira la rueda del ratón sobre un punto del lienzo
- **THEN** el plano hace zoom centrado en ese punto (acercando o alejando según el sentido del giro)
- **AND** la página no hace scroll mientras el cursor está sobre el plano

#### Scenario: Doble clic amplía y, si ya está ampliado, restaura el encuadre
- **GIVEN** el plano en su encuadre inicial
- **WHEN** el usuario hace doble clic sobre un punto del lienzo
- **THEN** el plano se amplía centrado en ese punto
- **AND** un doble clic posterior con el plano ya ampliado restaura el encuadre inicial

#### Scenario: Maximizar el plano no oculta un diálogo modal abierto encima
- **GIVEN** el plano maximizado (overlay in-app)
- **WHEN** se abre un diálogo modal (p. ej. confirmación de asignación) mientras el plano está maximizado
- **THEN** el diálogo se muestra por encima del plano, no detrás
- **AND** pulsar Escape mientras el plano está maximizado lo restaura a su tamaño normal

## ADDED Requirements

### Requirement: Modo explorar (zoom por rectángulo y minimapa)
El plano, en modo de solo lectura (no edición), DEBE (MUST) ofrecer un modo de navegación "explorar" en el que el lienzo grande NO se arrastra directamente: arrastrar sobre el lienzo DEBE (MUST) dibujar un rectángulo de selección (marquee) que, al soltar, hace zoom a esa zona. Un minimapa DEBE (MUST) mostrar una vista general fija con un recuadro que representa la porción visible del lienzo grande; arrastrar (o pulsar) el minimapa DEBE (MUST) desplazar (panear) el lienzo grande centrándolo en el punto señalado. Pulsar Escape en modo explorar (fuera de cualquier modal) DEBE (MUST) restaurar el encuadre inicial. El modo edición (arrastre de marcadores por el `ADMIN` para reposicionar puestos) NO DEBE (MUST NOT) verse afectado por este modo: conserva el arrastre directo de marcadores tal como estaba.

#### Scenario: Arrastrar el lienzo dibuja un rectángulo de zoom
- **GIVEN** el plano en modo explorar, sin edición activa
- **WHEN** el usuario arrastra desde un punto vacío del lienzo hasta otro
- **THEN** se dibuja un rectángulo de selección mientras arrastra
- **AND** al soltar, el plano hace zoom a la zona delimitada por el rectángulo

#### Scenario: Clic en un marcador selecciona, no dibuja rectángulo
- **GIVEN** el plano en modo explorar
- **WHEN** el usuario pulsa directamente sobre un marcador de puesto
- **THEN** se ejecuta la selección/acción de ese marcador
- **AND** no se inicia ningún rectángulo de zoom

#### Scenario: El minimapa panea el lienzo grande
- **GIVEN** el plano en modo explorar, ampliado sobre una zona
- **WHEN** el usuario arrastra el recuadro del minimapa hacia otra zona
- **THEN** el lienzo grande se desplaza (panea) para centrarse en el punto señalado en el minimapa

#### Scenario: Escape restaura el encuadre en modo explorar
- **GIVEN** el plano en modo explorar, ampliado o desplazado respecto al encuadre inicial
- **WHEN** el usuario pulsa Escape (sin ningún modal abierto)
- **THEN** el plano vuelve a su encuadre inicial

#### Scenario: El modo edición conserva el arrastre de marcadores
- **GIVEN** un `ADMIN` en modo edición de posiciones
- **WHEN** arrastra un marcador de puesto
- **THEN** el marcador se mueve siguiendo el cursor (arrastre directo), sin dibujar ningún rectángulo de zoom

### Requirement: Énfasis visual de los puestos libres/elegibles
El plano en modo de solo lectura DEBE (MUST) poder activar un énfasis visual en el que los puestos libres o elegibles laten (animación de pulso) y el resto de puestos se atenúan, para guiar la atención hacia lo reservable. Este énfasis DEBE (MUST) anularse cuando el usuario prefiere movimiento reducido (`prefers-reduced-motion`).

#### Scenario: Los puestos libres laten cuando el énfasis está activo
- **GIVEN** el plano con el énfasis de disponibilidad activado
- **WHEN** se renderizan los marcadores
- **THEN** los puestos libres/elegibles muestran una animación de pulso
- **AND** el resto de puestos se muestran atenuados
