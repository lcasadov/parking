# redesign-design-system

## Why
El frontend actual no tiene una identidad visual coherente con la marca ALEATICA.
Este change introduce la base compartida (tokens de color, tipografía y el layout
de shell) sobre la que se apoyan el resto de rediseños de pantalla. Es bloqueante:
ningún otro change de rediseño debe implementarse antes que este.

Referencia visual: `docs/aleatica-design-contract.md` y el prototipo aprobado.

## What Changes
- Se añaden tokens globales de color y tipografía como variables CSS.
- Se cargan las fuentes Cormorant Garamond (títulos/numerales) y Mulish (UI).
- Se crea un layout de shell reutilizable: sidebar 252px + header + tarjeta de usuario.
- Se define la escala tipográfica y los estilos base de botón/panel/pill/avatar.

## Impact
- Capa de presentación únicamente.
- NO se modifican: contratos de API, modelos, SQL, routing de datos ni lógica de negocio.
- Affected specs: `design-system` (nueva capability).
- Affected code (a confirmar por el agente antes de implementar): tema/estilos globales
  del frontend, componente de layout principal y navegación lateral.
