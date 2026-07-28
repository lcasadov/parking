## Why

El change `redesign-login` (archivado) dejó una tarjeta centrada convencional (400px, logo + título serif) sobre el design system ALEATICA recién creado. Dos commits posteriores (`06990ca`, `d6ad1b6`) sustituyeron esa tarjeta por una composición de marca completamente distinta: cabecera centrada con logo mini flotante, titular en degradado, fondo con orbes de color derivando y spotlight que sigue al cursor, portando el mock aprobado `docs/design/login-mock.html`. Este trabajo se hizo directamente sobre la rama sin abrir un change de OpenSpec, por lo que la spec `login` describe una pantalla que ya no existe. Este change reconstruye **as-built** (documenta lo que realmente quedó implementado, sin tocar código) para que la spec vuelva a ser fuente de verdad.

## What Changes

- **Composición centrada de marca** en `AuthShell`: fondo fijo con tres orbes de color (verde lima, azul, naranja) que derivan con animación continua, spotlight radial que sigue el puntero (`--mx`/`--my` actualizados vía `pointermove` + `requestAnimationFrame`), logo mini de Aleatica flotante con tilt 3D sutil sobre el puntero, y una card compacta con `backdrop-filter` para el formulario.
- **Titular e identidad fijos de marca**, no por pantalla: `auth.brandTitleLead` + `auth.brandTitleHighlight` (degradado animado) y `auth.brandSubtitle`, con pie `auth.secureFooter`. El antiguo `title`/`subtitle` de `AuthShell` pasa a ser opcional y, si se usa (p. ej. "Cambiar contraseña"), se pinta como encabezado **dentro** de la card en lugar de sustituir el titular de marca.
- **Formulario compacto**: inputs con icono Tabler dentro del campo (`auth2-inp`), placeholder en vez de `<label>` visible (con `aria-label` explícito), toggle mostrar/ocultar contraseña, corrección del autofill de Chrome (fondo y color de texto respetan el tema activo), y CTA "ENTRAR" en mayúscula con barrido de brillo (`::before` animado) y flecha que se desplaza al hover.
- **Tema claro/oscuro** por `body.theme-dark`, con paleta propia del bloque `.auth2` (tokens `--a-*`) independiente del resto del design system.
- **Motion condicionado**: toda la animación (orbes, spotlight, tilt del logo, entrada escalonada de los bloques, brillo del CTA, degradado del titular) se desactiva bajo `prefers-reduced-motion: reduce` y el spotlight/tilt del logo se desactiva además en punteros no finos (`pointer: fine` false → móvil/táctil), evitando escuchas de `pointermove` innecesarias.
- **Se retira** la curva de marca (`BrandCurve`) que un commit previo (`06990ca`) ya había quitado de la cabecera de la card; no vuelve a aparecer en la composición final.
- La lógica de acceso (intentos fallidos, mensaje con intentos restantes, aviso de bloqueo tras 5 intentos) permanece intacta; solo cambia la presentación.
- `AuthShell` se reutiliza sin cambios de contrato en `ChangePasswordPage` (mismo fondo/composición de marca, con "Cambiar contraseña" como encabezado dentro de la card).

## Capabilities

### Modified Capabilities
- `login`: la presentación del login pasa de la tarjeta centrada original a la composición de marca (orbes, spotlight, logo flotante, titular en degradado, formulario compacto), sin alterar el flujo de autenticación ni sus estados de error/bloqueo.
- `auth-screens`: `ChangePasswordPage` (que comparte `AuthShell`) hereda la misma composición de fondo/marca; su encabezado ("Cambiar contraseña") se renderiza ahora dentro de la card en vez de en una cabecera de card separada.

## Impact

- **Frontend:** `frontend/src/components/AuthShell.tsx` (reescrito), `frontend/src/pages/LoginPage.tsx` (inputs/CTA adaptados a las clases `.auth2-*`), `frontend/src/styles/components.css` (bloque `.auth2` nuevo, ~110 líneas), `frontend/src/i18n/locales/{es,en}.ts` (claves `brandTitleLead`, `brandTitleHighlight`, `brandSubtitle`, `secureFooter`), `frontend/src/pages/LoginPage.test.tsx` (ajustado a los nuevos selectores).
- **Backend:** ninguno. No hay cambios de endpoints, contratos ni `docs/openapi.yaml`.
- **Datos:** ninguno.
- **Docs:** ninguno fuera de este change (no se ha actualizado `docs/ui-screens.md`; queda fuera de alcance de esta reconstrucción as-built).
- **Fuera de alcance:** cualquier cambio de código — este change es puramente documental (as-built de lo ya mergeado en `d6ad1b6` y `06990ca`).
