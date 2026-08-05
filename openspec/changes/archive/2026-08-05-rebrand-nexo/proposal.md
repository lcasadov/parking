## Why

El producto se muestra hoy al usuario como **"ALEATICA"** (título de la web, lockup de marca en la app, PWA, emails, docs de API). El negocio ha decidido que la aplicación pase a llamarse **"Nexo"**. Necesitamos un nombre de producto único y consistente en todas las superficies visibles, sin tocar la infraestructura interna (paquete Java, dominios de correo) que sigue perteneciendo a la empresa **ALEATICA**.

## What Changes

- Se introduce **Nexo** como **nombre de producto** de la aplicación. **ALEATICA** se mantiene como **empresa/entidad** propietaria (no desaparece del dominio legal ni de la infraestructura).
- **Título de la web** (`frontend/index.html` `<title>`): `Reservas · ALEATICA` → `Nexo` (a secas, sin prefijo "Reservas ·").
- **PWA** (`manifest.webmanifest`): `name` y `short_name` → "Nexo"; `description` con "Nexo".
- **Lockup de marca en la app**: la **línea principal** (`common.appName` i18n es/en) muestra el producto **"Nexo"** y el **subtítulo** (`common.brandSub`) muestra la empresa **"ALEATICA"**, en `AppShell`, `Sidebar` y `BrandCurve`. **El logotipo de ALEATICA se mantiene** (arte PNG y `alt` sin cambios); no se tocan assets ni las aserciones de tests sobre el `alt` del logo.
- **Emails transaccionales** (10 plantillas Thymeleaf): la referencia de marca de producto en cabecera/pie pasa a Nexo (la dirección remitente y el dominio NO cambian).
- **Documentación de API** (`OpenApiConfig`): título/descripción del OpenAPI → Nexo.
- **Notificaciones push**: el nombre de app mostrado en el título de las notificaciones → Nexo.
- **Copy de referencia** que nombra el producto en textos i18n visibles → Nexo (distinto de las referencias a la *empresa/landing* ALEATICA en el flujo SSO, que se mantienen).
- **Logotipo**: se **mantiene el logo de ALEATICA** (arte PNG y `alt`) de momento — no se reemplazan assets. Solo cambia el **texto** del nombre de producto. Se asume el estado transitorio "texto Nexo + logotipo ALEATICA" (ver `design.md` D3).

## Capabilities

### New Capabilities

- `app-branding`: Nombre de producto ("Nexo") e identidad de marca visible: dónde debe aparecer (título del navegador, PWA, lockup en app, emails, API docs, push), y la separación entre **nombre de producto (Nexo)** y **empresa (ALEATICA)** que gobierna qué NO se renombra (paquete Java, dominios de correo, landing SSO corporativa).

### Modified Capabilities

- `app-shell`: el requisito "Header con identidad visual del design-system" cambia el lockup de marca de `parking / ALEATICA` a `parking / Nexo`.

> La marca en emails y en el título de las notificaciones push se rige por el nuevo requisito de `app-branding` (las specs de `notifications` describen el *comportamiento* de envío, no la cadena de marca, por lo que no se modifican a nivel de requisito).

## Impact

- **Frontend**: `index.html` (`<title>`), `public/manifest.webmanifest`, `src/i18n/locales/{es,en}.ts` (`brandSub` + copy de producto), texto de marca en `src/components/{AppShell,Sidebar,BrandCurve}.tsx`. **Sin** cambios de assets ni de `alt`/logo (`AuthShell.tsx` y `Sidebar.test.tsx` no se tocan).
- **Backend**: `src/main/resources/templates/email/*.html` (10), `config/OpenApiConfig.java` (título/descripción), builders de payload push (título de app). **No** se toca el paquete `com.aleatica.parking` ni `application.yml`/remitentes de correo.
- **Sin cambios de esquema de datos, API funcional, ni contratos de endpoints.** Cambio puramente de marca/copy + un asset opcional.
- **Fuera de alcance**: paquete Java `com.aleatica.parking` (~1120 refs, identificador interno), dominios `@aleatica.com` / `no-reply@parking.aleatica.com`, fixtures/seed de correos, referencia a la *landing de ALEATICA* del SSO (empresa, no producto).
