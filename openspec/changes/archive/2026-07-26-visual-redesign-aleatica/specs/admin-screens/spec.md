## MODIFIED Requirements

### Requirement: Patrones de UI compartidos
Todas las pantallas de administración DEBEN (MUST) construirse con un conjunto común de patrones (`PageHeader`/`EmbeddablePageHeader`, `SectionSwitch`, `Toolbar`, `DataTable` con variante de tarjetas en móvil, `Tabs`, `StatusPill`/`Badge`, `StatTile`, `Modal`, `Banner`, `Field`) apoyados en los tokens del design-system dark-premium, para garantizar un aspecto homogéneo. En una sección que agrupa varias sub-páginas (p. ej. Recursos, Registros, Mis plazas), la sub-página embebida NO DEBE (MUST NOT) pintar su propia cabecera completa ni tabs pequeñas encima del título de sección: el título va arriba (`PageHeader`) y el conmutador entre sub-páginas usa `SectionSwitch` (control grande, no tabs), siempre debajo del título.

#### Scenario: Aspecto homogéneo
- **WHEN** el usuario navega entre dos pantallas de administración cualesquiera
- **THEN** cabecera, toolbar, tablas, pills y modales comparten estructura, tipografía y color
- **AND** no existen estilos ad-hoc ni colores fuera de los tokens

#### Scenario: Sección fusionada sin cabecera duplicada
- **GIVEN** el `ADMIN` en una sección que agrupa sub-páginas (p. ej. "Recursos")
- **WHEN** se renderiza la sección
- **THEN** el título de sección aparece una sola vez, arriba, seguido del `SectionSwitch` grande para elegir la sub-página
- **AND** la sub-página embebida solo aporta su barra de acción compacta (`EmbeddablePageHeader`: crear, exportar), sin repetir el título

#### Scenario: Tablas admin como tarjetas en móvil
- **GIVEN** un `ADMIN` en viewport móvil sobre una pantalla con tabla (Empleados, Plazas, Puestos, Auditoría, Accesos)
- **WHEN** la tabla se renderiza
- **THEN** cada fila se presenta como tarjeta apilada (`.table-cards-mobile`) en vez de columnas de tabla tradicionales
- **AND** la información y acciones de la fila original siguen accesibles

### Requirement: Estados de datos consistentes
Las pantallas de listado DEBEN (MUST) presentar estados de carga, vacío y error de forma uniforme, y las de Gestión (Recursos, Registros, Ajustes) DEBEN (MUST) mostrar además una fila de mini-KPIs (`StatTile`) con los totales relevantes de la sección (p. ej. total/activas/inactivas en Recursos; total/OK/KO en Registros).

#### Scenario: Lista vacía
- **WHEN** una consulta no devuelve resultados
- **THEN** se muestra el patrón de vacío (mensaje + acción sugerida) con el estilo común

#### Scenario: Mini-KPIs en una pantalla de Gestión
- **GIVEN** el `ADMIN` en Recursos, Registros o Ajustes
- **WHEN** la pantalla carga sus datos
- **THEN** se muestra una fila de `StatTile` con los totales relevantes de esa sección, calculados a partir de los datos ya cargados (sin llamadas adicionales al backend)

## ADDED Requirements

### Requirement: Ocupación como vista control-room con modo explícito
La pantalla "Ocupación" DEBE (MUST) presentar un modo explícito de recurso ("Plazas de parking" / "Puestos de oficina") en el título, anunciado por región `aria-live`, conmutable mediante un selector grande de modo; DEBE (MUST) mostrar una fila de KPIs del modo activo (total/ocupados/libres/liberados hoy) con datos reales; y DEBE (MUST) ofrecer filtros rápidos siempre visibles con contadores (Todos/Solo libres/Ocupados/Liberados/Solicitudes) que filtran realmente la rejilla, sin una pestaña "Disponibilidad" separada.

#### Scenario: Cambiar el modo de Ocupación
- **GIVEN** el `ADMIN` en Ocupación con el modo "Plazas de parking" activo
- **WHEN** conmuta al modo "Puestos de oficina"
- **THEN** el título cambia a "Puestos de oficina" (anunciado vía `aria-live`)
- **AND** la fila de KPIs y la rejilla recurso×día se recargan con los datos de puestos

#### Scenario: Filtro rápido "Solo libres"
- **GIVEN** la rejilla de Ocupación cargada con recursos en varios estados
- **WHEN** el `ADMIN` activa el filtro "Solo libres"
- **THEN** la rejilla muestra únicamente las celdas en estado libre
- **AND** el chip del filtro muestra el contador real de recursos libres

### Requirement: "Ver en plano" desde una fila de Ocupación
Cada fila de puesto en la rejilla de Ocupación DEBE (MUST) ofrecer una acción "Ver en plano" que, al pasar el cursor, muestre un mini-plano en tooltip marcando la posición del puesto, y al pulsarla, abra un modal con el plano completo (reutilizando el componente de superficie del plano en modo **solo lectura**) resaltando ese puesto con una animación de pulso. Esta acción NO DEBE (MUST NOT) navegar a la ruta del plano.

#### Scenario: Hover muestra el mini-plano
- **GIVEN** una fila de puesto en Ocupación
- **WHEN** el `ADMIN` pasa el cursor sobre "Ver en plano"
- **THEN** aparece un tooltip con un mini-plano marcando la posición de ese puesto

#### Scenario: Click abre el modal de plano completo
- **GIVEN** una fila de puesto en Ocupación
- **WHEN** el `ADMIN` pulsa "Ver en plano"
- **THEN** se abre un modal con el plano completo en modo solo lectura, con el puesto resaltado mediante una animación de pulso
- **AND** la ruta de la aplicación no cambia (no navega al Plano)
