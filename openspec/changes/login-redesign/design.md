## Context

Este change es una reconstrucción **as-built**: el código ya está mergeado en `develop` (commits `d6ad1b6` y `06990ca`, dentro de la ola de rediseño "ALEATICA"). No se modifica ningún fichero de la aplicación; el propósito es que `openspec/specs/login/spec.md` y `openspec/specs/auth-screens/spec.md` vuelvan a describir con precisión lo que existe en `frontend/src/components/AuthShell.tsx`, `frontend/src/pages/LoginPage.tsx` y el bloque `.auth2` de `frontend/src/styles/components.css`.

Cronología real (por timestamp de commit, no por el orden en que se citan en la tarea): `06990ca` (24 jul) quitó la curva de marca (`BrandCurve`) de la cabecera de la card, que en ese momento aún era la tarjeta lateral/centrada de la ola B4 (`ffd6005`). `d6ad1b6` (25 jul) sustituyó por completo esa composición por la actual (`auth2`), portando el mock aprobado `docs/design/login-mock.html`.

## Goals / Non-Goals

**Goals:**
- Documentar la composición `.auth2` (fondo con orbes, spotlight por puntero, logo flotante, titular en degradado, formulario compacto) como el estado vigente de la capacidad `login`.
- Documentar que `AuthShell` es compartido por `LoginPage` y `ChangePasswordPage`, y cómo el `title`/`subtitle` opcional se integra dentro de la card.
- Documentar las condiciones de desactivación de movimiento (`prefers-reduced-motion`, puntero no fino) como parte del comportamiento esperado, no como detalle de implementación incidental.
- Confirmar que la lógica de acceso (intentos, bloqueo) no forma parte de este change.

**Non-Goals:**
- No se propone ningún cambio de código, backend, contrato de API o esquema de datos.
- No se audita ni se rediseña `ChangePasswordPage` más allá de reflejar que hereda `AuthShell`.
- No se crea una capacidad `theming` separada: la paleta `.auth2` es local al bloque de auth (prefijo `--a-*`) y no reutiliza ni extiende los tokens globales de tema documentados en la capacidad `theming`; por eso no se lista como capability modificada.

## Decisions

**D1. Se documenta como MODIFIED sobre `login`, no como capability nueva.**
El requisito "Presentación del login" ya existía (creado por el change archivado `redesign-login`). La composición `.auth2` sustituye la implementación descrita en ese requisito pero no cambia su intención (identidad ALEATICA, flujo de auth intacto), así que se modela como `MODIFIED Requirements`, añadiendo el detalle de la composición y el motion condicionado.

**D2. `auth-screens` se marca como MODIFIED, no `theming`.**
`ChangePasswordPage` monta `AuthShell` sin cambios de contrato: solo pasa `title`, que ahora se pinta dentro de la card en vez de en una cabecera de card separada. Es un cambio de presentación de una capacidad ya cubierta por `auth-screens` ("Cambiar contraseña"). El tema claro/oscuro del bloque `.auth2` se resuelve con su propio juego de variables `--a-*` conmutadas por `body.theme-dark` (el mismo selector que usa la capacidad `theming` para el resto de la app), pero no añade ni modifica ningún requisito de esa capacidad: no se toca el conmutador de Preferencias ni la persistencia de la elección.

**D3. El motion condicionado se documenta como parte del requisito, no como nota de implementación.**
`prefers-reduced-motion: reduce` desactiva animaciones CSS (orbes, brillo del CTA, degradado del titular, entrada escalonada) vía media query, y además corta en JS el listener de `pointermove` (spotlight + tilt del logo) cuando `reduce` es true o `pointer: fine` es false. Se documenta como escenario explícito porque es observable por el usuario y relevante para accesibilidad/rendimiento en móvil.

## Risks / Trade-offs

- **Deriva entre spec y código si se vuelve a iterar el diseño sin change.** Mitigación: este change dedica su `tasks.md` a poner al día `login`/`auth-screens`; futuras iteraciones visuales de la pantalla de acceso deberían abrir un change propio en vez de commitear directo.
- **Paleta `.auth2` (`--a-lime`, `--a-blue`, etc.) no reutiliza los tokens del design system.** Riesgo de inconsistencia si el design system cambia de paleta de marca. Se documenta el hecho, no se corrige (fuera de alcance de un as-built).

## Migration Plan

No aplica: no hay código que migrar. Al aprobarse este change, se sincroniza `openspec/specs/login/spec.md` y `openspec/specs/auth-screens/spec.md` (vía `openspec-archive-change` o `openspec-sync-specs`) para que reflejen el `MODIFIED Requirements` aquí propuesto.
