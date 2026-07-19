# redesign-requests

## Why
Rediseñar la bandeja de solicitudes del administrador con la identidad ALEATICA para
resolver peticiones por orden de llegada de forma clara.

Referencia: sección "Solicitudes" de `docs/aleatica-design-contract.md` y del prototipo.

## What Changes
- Header + pestañas de filtro (Pendientes/Aprobadas/Rechazadas/Todas) con contador.
- Buscador de empleado.
- Tabla con avatar de color por empleado, fecha solicitada (serif), día, creada y
  acciones Aprobar/Rechazar.

## Impact
- Solo presentación. Mantiene las mutaciones y endpoints actuales (aprobar/rechazar).
- Requiere `redesign-design-system`.
- Affected specs: `requests`.
