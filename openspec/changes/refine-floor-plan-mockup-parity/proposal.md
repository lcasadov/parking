# Proposal: refine-floor-plan-mockup-parity

## Why
El plano interactivo de puestos (`floor-plan`) es funcionalmente correcto pero su
UI diverge de los mockups de diseño: los 65 puestos se siembran en la coordenada
central `(50,50)` y se renderizan **apilados** (ilegible de partida), la imagen de
fondo es un placeholder azul fuera de la paleta del design-system, y faltan piezas
enteras de los mockups (barra de fecha, chips de filtro con contadores, zoom, panel
lateral buscable) y todo el soporte móvil (táctil, pinch-zoom, lista "Solicitar").
Este change alinea la presentación del plano con los mockups y el design-system,
sin tocar el contrato de la API ni el backend de dominio.

## What Changes
- **Fin del apilamiento**: se siembran coordenadas distintas por puesto (rejilla) en el seed dev y se deja de tratar el centro por defecto como "colocado".
- **Imagen de planta** neutra acorde a tokens en sustitución del placeholder SVG azul.
- **Barra de fecha** (`plano-datebar`): día anterior/siguiente, "Hoy", fecha larga y "Ventana de reserva: 14 días".
- **Chips de filtro por estado** con contadores (Libre, Liberado hoy, Mi puesto, Solicitado, Ocupado) + distinción Dirección.
- **Controles de zoom** (+/−/restablecer) sobre un viewport.
- **Panel lateral** de puestos buscable (escritorio) y variante admin "ocupación del día".
- **Marcadores** en paleta pastel semántica del design-system (texto oscuro, borde fino); sin `font-weight:700`; hex tokenizados; EXECUTIVE con anillo ámbar + rombo ◆.
- **Móvil**: eventos táctiles/pointer para arrastre, pinch-zoom o botones de zoom, lista "Disponibles para solicitar" con botón por fila, breakpoints `<768px`.
- **CreateRequestModal**: banners de disponibilidad por recurso (plaza/puesto) antes de enviar.

## Capabilities
- `floor-plan` (MODIFIED — requisitos de presentación/UX de la vista de plano)

## Impact
- **Frontend**: `FloorPlanPage`, `FloorPlanSurface`, `FloorPlanMarker`, componentes nuevos (datebar, filtros, zoom, panel lateral, lista móvil), `CreateRequestModal`, `styles/*.css` (sección plano), assets (imagen de planta), `DeskFormModal` (coord por defecto), seed dev de coordenadas.
- **Contrato API / backend de dominio**: sin cambios (la respuesta `getFloorPlan` ya expone `coordX/coordY/state/category`).
- **Diseño**: coherencia con `docs/design-system.md` (pesos 400/500, colores desde tokens) y con los mockups `plano-*`.

## Out of scope
- Cambios en el contrato de la API `/floor-plan` o en el backend de dominio.
- Creación de tests E2E (van en el change `e2e-employee-request-approval-mobile`).
- Selección de puesto concreto en la solicitud unificada (el admin asigna al aprobar; se mantiene el flujo genérico `resourceType=DESK`).
- Editor de posiciones como pantalla dedicada separada (se mantiene el modo edición sobre el propio plano, mejorado con feedback de selección).
