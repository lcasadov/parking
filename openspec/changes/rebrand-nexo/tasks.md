## 1. Frontend — título, PWA y fuente única del nombre

- [ ] 1.1 `frontend/index.html`: `<title>Reservas · ALEATICA</title>` → `<title>Nexo</title>` (a secas)
- [ ] 1.2 `frontend/public/manifest.webmanifest`: `name` → "Nexo", `short_name` → "Nexo", `description` → texto con "Nexo" (mantener `theme_color`/iconos)
- [ ] 1.3 `frontend/src/i18n/locales/es.ts` y `en.ts`: `brandSub: 'ALEATICA'` → `'Nexo'`
- [ ] 1.4 Revisar copy i18n que nombre el **producto** (distinto de la referencia a la *empresa/landing* del SSO, que se mantiene ALEATICA)

## 2. Frontend — texto del lockup de marca (NO se toca el logo ni el `alt`)

- [ ] 2.1 `src/components/AppShell.tsx`: `<span class="shell-topbar-sub">ALEATICA` → "Nexo" (consumir `brandSub`). **No** tocar el `<img alt="ALEATICA">`
- [ ] 2.2 `src/components/Sidebar.tsx`: `sidebar-brand-sub` "ALEATICA" → "Nexo" (consumir `brandSub`). **No** tocar el `<img alt="ALEATICA">`
- [ ] 2.3 `src/components/BrandCurve.tsx`: `brand-sub` → consumir `brandSub` (Nexo)
- [ ] 2.4 `src/components/AuthShell.tsx`: sin cambios (solo tiene el logo con `alt` de ALEATICA, que se conserva)
- [ ] 2.5 Verificar que ningún test asertaba el **texto** de marca antiguo; la aserción del `alt` del logo (`Sidebar.test.tsx` `getByRole('img', { name: 'ALEATICA' })`) **se mantiene** (el logo sigue siendo el de ALEATICA)

## 3. Backend — emails, API docs y push

- [ ] 3.1 `src/main/resources/templates/email/*.html` (10 plantillas): sustituir la mención de **producto** por "Nexo" en cabecera/cuerpo; mantener remitente/dominio y pie de empresa (ALEATICA)
- [ ] 3.2 `config/OpenApiConfig.java`: título/descripción del OpenAPI → "Nexo" (no tocar el email de contacto `admin@aleatica.local`)
- [ ] 3.3 Builders de payload push (p. ej. `EmployeeVehicleAdminNotifier` y demás notifiers): título de la notificación → "Nexo" (no tocar `VAPID subject`)
- [ ] 3.4 Regenerar `docs/openapi.yaml` si aplica, reflejando el nuevo título

## 4. Logotipo — SIN CAMBIOS en esta fase (decisión: se mantiene el de ALEATICA)

- [ ] 4.1 No se reemplazan ni renombran assets de `public/` (logos/favicons). El arte de Nexo queda como deuda visible futura (nuevo change cuando Diseño lo entregue)

## 5. Verificación (Quality Gate)

- [ ] 5.1 Frontend: `npm run lint && npm test && npm run build` verdes (cobertura no baja)
- [ ] 5.2 Backend: `mvn clean verify` verde (0 violations nuevas Sonar)
- [ ] 5.3 `grep -rniE "aleatica" frontend/src backend/src/main/resources/templates` — confirmar que las apariciones restantes son **empresa** (dominios, landing SSO) o el **logotipo/`alt`** (que se conserva a propósito), no el nombre de producto
- [ ] 5.4 Pasada visual (reality-checker): login, shell autenticado y un email — coherencia de co-branding "Nexo" (producto) vs "ALEATICA" (empresa)
