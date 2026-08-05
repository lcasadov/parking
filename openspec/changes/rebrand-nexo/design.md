## Context

La marca visible de la aplicación es "ALEATICA", presente en ~20 puntos de UI/copy más plantillas de email y docs de API. Además, el identificador de código base es el paquete Java `com.aleatica.parking` (~1120 referencias) y la infraestructura de correo usa el dominio `aleatica.com` (`no-reply@parking.aleatica.com`, emails semilla/tests `@aleatica.com`).

El negocio quiere renombrar el **producto** a "Nexo". ALEATICA es la **empresa** propietaria: su dominio de correo, entidad legal y landing corporativa (usada en el flujo SSO de Fase 2) no cambian. Este change separa ambos conceptos para acotar el alcance a lo que el usuario ve como "nombre de la app".

## Goals / Non-Goals

**Goals:**
- El usuario ve "Nexo" como nombre de producto en: título del navegador, PWA (instalación/splash), lockup de marca en la app (sidebar/topbar/login), emails transaccionales, documentación de API y notificaciones push.
- Una única fuente para el nombre de producto en el frontend (la clave i18n `brandSub`) para evitar divergencias futuras.
- Cero cambios funcionales: mismos endpoints, mismo esquema, mismos flujos. Riesgo de regresión mínimo.

**Non-Goals:**
- **No** se renombra el paquete Java `com.aleatica.parking` (identificador interno; renombrarlo son ~1120 refs + posibles rupturas de imports/reflexión/Flyway locations, sin beneficio para el usuario).
- **No** se cambian dominios ni remitentes de correo (`no-reply@parking.aleatica.com`) ni los emails semilla/de test `@aleatica.com` (infraestructura de la empresa).
- **No** se altera la referencia a la *landing corporativa de ALEATICA* del SSO (es la empresa, no el producto).
- **No** se rediseña la identidad visual (colores, `theme_color` `#0f9e68`, tipografía Geist se mantienen).

## Decisions

**D1 — "Nexo" es nombre de producto; "ALEATICA" permanece como empresa.**
El renombrado toca solo superficies donde aparece el *nombre del producto*. Donde el texto se refiere a la *empresa* (landing SSO, dominio de correo, entidad legal) se mantiene ALEATICA. Alternativa descartada: rebrand total (eliminar ALEATICA de todo) — introduce cambios de infraestructura (dominios, cuentas) fuera del espíritu de "cambiar el título de la web" y con riesgo operativo alto.

**D2 — Lockup de dos líneas: producto arriba, empresa debajo.**
El lockup de marca se compone de la **línea principal** `common.appName` = **"Nexo"** (producto) y el **subtítulo** `common.brandSub` = **"ALEATICA"** (empresa). `AppShell` y `Sidebar` consumen ambas claves i18n; `BrandCurve`/`BrandLogo` (sin i18n) usa los literales "Nexo"/"ALEATICA". El `<title>` de `index.html` y el `manifest.webmanifest` son estáticos (fuera de i18n) y muestran "Nexo" a secas.

**D3 — El logo de ALEATICA se mantiene (arte y `alt`); solo cambia el texto del nombre. [RESUELTA]**
Decisión del negocio: de momento se conserva el logotipo actual de ALEATICA (ficheros PNG **y** su `alt`). El renombrado toca únicamente el **texto** del nombre de producto (`brandSub`, título, PWA, emails, API, push) → "Nexo". Se asume conscientemente el estado transitorio "texto Nexo + logotipo ALEATICA" hasta que exista arte de Nexo. Esto minimiza el cambio y no toca ningún activo gráfico ni las aserciones de tests sobre el `alt` del logo.

**D4 — Emails: renombrar el nombre de producto en cuerpo/cabecera, no el remitente.**
En las 10 plantillas Thymeleaf se cambia la mención de producto a "Nexo". La dirección `from` y el pie legal con la empresa (si procede) siguen siendo ALEATICA. Se revisa cada plantilla para distinguir "producto" de "empresa".

**D5 — Notificación push: título = "Nexo".**
El título mostrado en las notificaciones push (payload `{title,...}` construido en los notifiers, p. ej. `EmployeeVehicleAdminNotifier`) usa "Nexo". El `VAPID subject` (mailto de contacto) es infraestructura y no cambia.

## Risks / Trade-offs

- **[Co-branding inconsistente]** Mezclar "Nexo" (producto) y "ALEATICA" (empresa) puede leerse raro si no se decide el patrón. → Mitigación: nombre único "Nexo" (a secas) en título/UI; ALEATICA solo donde es literalmente la empresa (SSO/legal) o el logotipo (que se conserva de momento). Validar con una pasada visual (reality-checker) sobre login, shell, y un email.
- **[Tests que asertan el nombre viejo]** `Sidebar.test.tsx` asdserta `alt: 'ALEATICA'`; habrá otros. → Mitigación: `grep` de "ALEATICA" en tests visibles y actualizar aserciones en el mismo change.
- **[Assets desalineados]** Si se cambia el texto pero no el PNG, la app muestra "Nexo" junto al logotipo antiguo. → Mitigación: D3 (arte como sub-tarea condicionada); si Diseño no entrega a tiempo, se documenta como deuda visible y no bloquea el resto.
- **[Cache PWA]** El `name`/`short_name` cacheados pueden tardar en refrescar en dispositivos ya instalados. → Mitigación: aceptable; se resuelve al reinstalar/refrescar el manifest.

## Migration Plan

1. Cambios de copy/config frontend + tests → `npm run lint && npm test && npm run build` verdes.
2. Cambios de plantillas email + OpenApi + push backend → `mvn clean verify` verde.
3. (Condicional) Swap de assets de logo cuando exista arte de Nexo.
4. Deploy estándar (no requiere migración de datos). Rollback = revertir el commit (sin efectos colaterales de estado).

## Open Questions

- **OQ1 — Patrón de nombre visible: [RESUELTA]** "**Nexo**" a secas (de momento), sin prefijo "Reservas ·" ni co-brand "by ALEATICA". El `<title>`, el `name`/`short_name` del PWA y el lockup de marca muestran simplemente "Nexo".
- **OQ2 — Arte del logo: [RESUELTA]** Se **mantiene el logo de ALEATICA** (arte PNG y `alt`). No hay swap de assets en esta fase (ver D3). Queda como deuda visible hasta que exista logotipo de Nexo.
- **OQ3 — Pie legal de emails:** ¿los emails deben seguir mostrando "ALEATICA" como empresa en el pie? Propuesta: sí (empresa), producto = Nexo. (No bloqueante; por defecto se conserva ALEATICA como empresa.)
