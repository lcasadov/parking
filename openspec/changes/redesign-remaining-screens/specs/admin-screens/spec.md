# admin-screens

## ADDED Requirements

### Requirement: Patrones de UI compartidos
Todas las pantallas de administración DEBEN construirse con un conjunto común de patrones
(PageHeader, Toolbar, DataTable, Tabs, StatusPill/Badge, Modal, Banner, Field) apoyados en
los tokens del design system, para garantizar un aspecto homogéneo.

#### Scenario: Aspecto homogéneo
- **WHEN** el usuario navega entre dos pantallas de administración cualesquiera
- **THEN** cabecera, toolbar, tablas, pills y modales comparten estructura, tipografía y color
- **AND** no existen estilos ad-hoc ni colores fuera de los tokens

### Requirement: Estados de datos consistentes
Las pantallas de listado DEBEN presentar estados de carga, vacío y error de forma uniforme.

#### Scenario: Lista vacía
- **WHEN** una consulta no devuelve resultados
- **THEN** se muestra el patrón de vacío (mensaje + acción sugerida) con el estilo común

### Requirement: Pills de estado y categoría por color
Los estados (activo/inactivo, tipos de liberación, categorías de puesto, resultado de login,
tipo de recurso) DEBEN usar el mapa estado→color del design system.

#### Scenario: Categoría de puesto
- **WHEN** se muestra un puesto STANDARD o EXECUTIVE
- **THEN** su badge usa el token de color correspondiente, igual en todas las vistas

### Requirement: Modales con estructura común
Los modales de administración DEBEN usar el shell de diálogo común (cabecera, cuerpo, footer)
con banners informativos y una variante destructiva para acciones de rechazo/eliminación,
sin alterar las mutaciones existentes.

#### Scenario: Rechazo de solicitud
- **WHEN** el admin abre el modal de rechazo
- **THEN** se presenta con la variante destructiva y el motivo obligatorio
- **AND** al confirmar se ejecuta la mutación de rechazo existente
