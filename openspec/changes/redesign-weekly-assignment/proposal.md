# redesign-weekly-assignment

## Why
Rediseñar la pantalla de asignación semanal con la identidad ALEATICA: leer el estado de
cada plaza día a día de un vistazo mediante color, sin abrir fichas.

Referencia: sección "Asignación semanal" de `docs/aleatica-design-contract.md` y del prototipo.

## What Changes
- Header de página + tarjetas resumen (plazas activas, asignaciones, liberadas, solicitudes).
- Navegador de semana (anterior/siguiente/hoy) con rango y nº de semana.
- Grid plaza × día con celdas coloreadas según el mapa estado→color, columna "hoy" resaltada.
- Leyenda de estados.

## Impact
- Solo presentación. Usa los datos y endpoints existentes tal cual.
- Requiere `redesign-design-system`.
- Affected specs: `weekly-assignment`.
