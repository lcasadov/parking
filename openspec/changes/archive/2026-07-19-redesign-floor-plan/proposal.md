# redesign-floor-plan

## Why
Rediseñar el plano del día con la identidad ALEATICA usando la planta real como fondo y
marcadores de puesto coloreados por estado, más un panel de ocupación.

Referencia: sección "Plano del día" de `docs/aleatica-design-contract.md` y del prototipo.

## What Changes
- Fondo con la imagen real `frontend/src/assets/floor-plan.png`.
- Marcadores de puesto posicionados en % sobre sus coordenadas (las del seed de puestos),
  coloreados según el mapa estado→color, con hover y número.
- Panel lateral "Ocupación del día": contadores (ocupado/libre/liberado/solicitado) + listado.
- Leyenda de estados.

## Impact
- Solo presentación. No cambia el origen de datos de puestos ni sus coordenadas.
- Requiere `redesign-design-system`.
- Affected specs: `floor-plan`.
