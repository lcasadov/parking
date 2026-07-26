# Tasks — login-redesign

Reconstrucción as-built: todas las tareas describen trabajo ya implementado y mergeado en `develop`
(commits `d6ad1b6`, `06990ca`). No hay tareas pendientes de código; este change es documental.

## 1. Composición de marca (`AuthShell`)

- [x] 1.1 Sustituir la tarjeta lateral/centrada previa por la composición `.auth2`: fondo fijo con tres orbes de color de marca (`o1` verde lima, `o2` azul, `o3` naranja) derivando con animación continua
- [x] 1.2 Spotlight radial de fondo (`--mx`/`--my`) que sigue al puntero vía `pointermove` + `requestAnimationFrame`, activado solo tras el primer movimiento (clase `is-live`)
- [x] 1.3 Logo mini de Aleatica flotante (`auth2-badge`) con tilt 3D sutil hacia el puntero
- [x] 1.4 Titular fijo de marca en dos líneas con segunda línea en degradado animado ("Gestión de" + "plazas y puestos") y subtítulo de marca
- [x] 1.5 Retirar la curva de marca (`BrandCurve`) de la cabecera de la card (ya retirada en `06990ca`, confirmada ausente en el resultado final)
- [x] 1.6 `title`/`subtitle` de `AuthShell` pasan a opcionales y se pintan como encabezado dentro de la card (usado hoy por `ChangePasswordPage`)

## 2. Formulario compacto (`LoginPage`)

- [x] 2.1 Inputs con icono Tabler dentro del campo (usuario `ti-user`, contraseña `ti-lock`) y placeholder + `aria-label` (sin `<label>` visible)
- [x] 2.2 Toggle mostrar/ocultar contraseña (`ti-eye`/`ti-eye-off`) con `aria-pressed`/`aria-label`
- [x] 2.3 Fix de autofill de Chrome: el campo respeta el color de texto y fondo del tema activo en vez de forzar blanco
- [x] 2.4 CTA "ENTRAR" en mayúscula con barrido de brillo animado y flecha que se desplaza al hover

## 3. Tema y motion condicionado

- [x] 3.1 Paleta clara/oscura propia del bloque `.auth2` (tokens `--a-*`) conmutada por `body.theme-dark`
- [x] 3.2 Desactivar toda animación CSS (orbes, degradado del titular, brillo del CTA, entrada escalonada) bajo `prefers-reduced-motion: reduce`
- [x] 3.3 Desactivar en JS el listener de `pointermove` (spotlight + tilt del logo) cuando `prefers-reduced-motion: reduce` es true o `pointer: fine` es false

## 4. i18n

- [x] 4.1 Añadir claves `auth.brandTitleLead`, `auth.brandTitleHighlight`, `auth.brandSubtitle`, `auth.secureFooter` en `es.ts` y `en.ts`

## 5. Verificación y reconstrucción de spec

- [x] 5.1 Confirmar que el flujo de auth y los endpoints no cambian (intentos restantes, bloqueo tras 5 intentos)
- [x] 5.2 Confirmar que `ChangePasswordPage` reutiliza `AuthShell` sin cambios de contrato
- [x] 5.3 Actualizar `openspec/specs/login/spec.md` (`MODIFIED Requirements`) con la composición as-built
- [x] 5.4 Actualizar `openspec/specs/auth-screens/spec.md` (`MODIFIED Requirements`) reflejando el encabezado de "Cambiar contraseña" dentro de la card
