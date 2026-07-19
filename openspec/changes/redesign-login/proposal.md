# redesign-login

## Why
Rediseñar la pantalla de acceso con la identidad ALEATICA. Es un cambio aislado y de
bajo riesgo, ideal como primera validación visual del design system.

Referencia: sección "Login" de `docs/aleatica-design-contract.md` y del prototipo.

## What Changes
- Tarjeta de login centrada con logo ALEATICA y subtítulo "Gestión de parking".
- Campos usuario/contraseña con iconos Tabler y toggle de visibilidad.
- Banner de error y aviso de bloqueo tras intentos fallidos.

## Impact
- Solo presentación. NO cambia la lógica de autenticación ni los endpoints.
- Requiere `redesign-design-system` (tokens + fuentes).
- Affected specs: `login`.
