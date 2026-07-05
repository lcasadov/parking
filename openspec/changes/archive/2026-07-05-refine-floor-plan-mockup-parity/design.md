# Design: refine-floor-plan-mockup-parity

## Context
La capability `floor-plan` entrega hoy una vista mínima: imagen placeholder +
marcadores por coordenada + leyenda + aside de "no colocados". Los mockups del
plano (`Escritorio_plano_panel lateral`, `Editor de posiciones`, `Plano admin_titulares`,
`Móvil_pinch-zoom`) definen un sistema de componentes `plano-*` mucho más rico
(`plano-datebar`, `chip-filter`, `plano-zoom`, `plano-side`, `puesto-marker` pastel,
lista móvil `mlist-row`). El backend ya expone todo lo necesario (`coordX/coordY`,
`state`, `category`) vía `GET /floor-plan?date`; este change es exclusivamente de
presentación y no altera el contrato.

## Goals
- Que el plano sea legible de partida (sin apilamiento) y visualmente idéntico a los mockups en escritorio, admin y móvil.
- Reutilizar el design-system (tokens de color, pesos 400/500) — cero hex sueltos, cero `font-weight:700`.
- Que el empleado pueda solicitar un puesto desde el plano **también en móvil** (lista "Disponibles para solicitar").
- No cambiar el contrato ni el backend de dominio.

## Decisions
- **Coordenadas sembradas en rejilla (seed dev)**. *Por qué:* el apilamiento en (50,50) es el fallo visual principal; una rejilla determinista (p. ej. 9 columnas × filas) da un plano legible sin depender de que el admin recoloque 65 puestos a mano. El seed vive solo en `db/seed/dev` (no PRE/PRO). `DeskFormModal` deja de forzar (50,50): un puesto nuevo nace "sin colocar" hasta que el admin lo posiciona.
- **Imagen de planta neutra desde tokens**. *Por qué:* el placeholder azul introduce colores fuera de paleta; se sustituye por un SVG/imagen en tonos del design-system (crema/neutros) o una planta real si se aporta. La posición sigue siendo relativa (%), independiente de la resolución.
- **Componentes `plano-*` nuevos, semántica de estado intacta**. *Por qué:* se añaden `FloorPlanDatebar`, `FloorPlanFilters`, `FloorPlanZoom`, `FloorPlanSidePanel`, `FloorPlanMobileList` reutilizando el `state` que ya calcula el backend; no se toca la lógica de disponibilidad.
- **Marcadores pastel + texto oscuro (design-system §2.4)**. *Por qué:* el estilo sólido saturado actual no coincide con la paleta soft del sistema; se mapea cada estado a su token pastel, borde 1px, EXECUTIVE con anillo ámbar (token) + rombo ◆.
- **Interacción por Pointer Events**. *Por qué:* unifica ratón y táctil (arrastre del admin y pan/tap del empleado) con una sola implementación; habilita móvil sin duplicar handlers. El zoom se ofrece con botones (+/−/restablecer) y, donde el navegador lo permita, gesto de pinch.
- **Banners de disponibilidad en `CreateRequestModal`**. *Por qué:* el mockup de solicitud unificada muestra disponibilidad por recurso antes de enviar; se consulta `GET /availability?date&resourceType` y se pinta un banner informativo (no bloqueante) por plaza/puesto.

## Risks
- **Sobre-ingeniería del plano**: el sistema `plano-*` es amplio. *Mitigación:* priorizar por severidad (ALTA→MEDIA→BAJA) y mantener los componentes pequeños (complejidad <15), con tests unitarios por componente.
- **Regresión del flujo de solicitud desde el plano**: al reestructurar la superficie. *Mitigación:* preservar los tests existentes de `FloorPlanPage`/`floorPlanRbac` y añadir los de los nuevos componentes; el contrato no cambia.
- **Rendimiento con 65 marcadores + zoom en móvil**. *Mitigación:* transform CSS (GPU) para zoom/pan, marcadores como nodos ligeros; lista móvil como vía principal de solicitud (no depende de pinchar marcadores diminutos).

## Migration Plan
- **Seed dev**: nueva migración en `db/seed/dev` que actualiza las coordenadas de los 65 puestos a una rejilla (idempotente). No afecta a PRE/PRO (solo esquema).
- **Sin migración de esquema**: `coord_x/coord_y` ya existen en `desks`.
- **Assets**: añadir la imagen/SVG de planta neutra a `frontend/src/assets` y retirar el placeholder azul.
- Sin cambios en `docs/openapi.yaml` (contrato intacto).
