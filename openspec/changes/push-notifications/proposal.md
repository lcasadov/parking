## Why

Hoy las notificaciones del sistema (solicitud aprobada/rechazada, reasignación, cancelación admin, nueva solicitud pendiente) solo salen por **email**, que llega tarde y se pierde entre el correo corporativo. El empleado quiere enterarse **al instante** de que le confirman, rechazan o cambian un recurso, y el admin quiere un aviso inmediato cuando entra una solicitud que requiere aprobación **manual**. **Web Push** (VAPID, estándar del navegador) da avisos en tiempo real **sin coste** y reutiliza toda la infraestructura de eventos ya existente. Además, el admin necesita poder **activar/desactivar cada canal (email y push) de forma independiente** desde Configuración.

## What Changes

- **Nuevo canal Web Push** en paralelo al email, enganchado a los **mismos eventos de dominio** ya publicados (`RequestApproved`, `RequestRejected`, `RequestAdminAssigned`, `RequestCancelled`, `RequestCreated`, `WaitlistAvailable`). No cambia la lógica de negocio: un segundo listener `AFTER_COMMIT`.
- **Avisos al EMPLEADO** (a sus dispositivos suscritos): recurso confirmado (aprobada / auto‑asignada / asignación puntual admin), rechazada, **cambiada** (reasignación o intercambio), y **cancelación admin de una aprobada**.
- **NUEVO respecto a hoy**: cuando un ADMIN cancela la reserva **aprobada** de un empleado, hoy solo se avisa a los admins; este change añade el aviso **al empleado afectado** (por email y push). El empleado que cancela lo suyo no se auto‑notifica (ver design D12).
- **Lista de espera** (`WaitlistAvailable`): el aviso se mantiene **a los admins** (lo resuelven desde pendientes), como hoy — **no** al empleado.
- **Aviso al ADMIN**: cuando entra una `POST /requests` con `approvalMode = MANUAL` (pendiente de aprobar) → push/email a **todos los admins activos** (fan‑out). En `AUTOMATIC` no se avisa al admin (nace aprobada).
- **Preferencias de canal GLOBALES en Configuración (ADMIN)**: dos checkboxes **independientes** en `system_settings` — `emailNotificationsEnabled` y `pushNotificationsEnabled`. Cada canal se respeta por separado (ambos on, ambos off, o solo uno). El email queda como **fallback** natural si push no está soportado/concedido.
- **Preferencias de canal POR EMPLEADO en el formulario de empleado (ADMIN)**: cada empleado tiene sus propios flags `emailNotificationsEnabled`/`pushNotificationsEnabled`, editables por el ADMIN en el formulario de empleado (EmployeeFormModal), **activos por defecto**. Un aviso llega por un canal solo si **el interruptor global Y el del empleado** están activos (y, para push, si el empleado tiene algún dispositivo suscrito). Permite silenciar a un empleado concreto sin apagar el canal para todos.
- **Suscripción por usuario/dispositivo**: cada empleado (o admin) concede permiso en su navegador y registra su suscripción; puede activarla/desactivarla desde su perfil. Un mismo usuario puede tener varios dispositivos.
- **Gestión del ciclo de vida**: suscripciones muertas (respuesta 404/410 del push service) se **borran** automáticamente; caducadas/rotadas se re‑suscriben desde el cliente.
- **PWA mínima**: la app pasa a registrar un **Service Worker** (requisito de Web Push) + `manifest`, manejando `push` y `notificationclick` (deep‑link a la pantalla relevante).
- **Onboarding contextual en la home (sin auto‑prompt)**: NO se pide permiso automáticamente al detectar que las notificaciones no están activas. En su lugar, tarjetas llamativas y descartables en la página principal, según plataforma/estado: en **Android/escritorio compatible**, tarjeta explicativa + botón "Activar notificaciones" que dispara el prompt nativo; en **iOS sin instalar**, tarjeta que explica cómo **instalar la app** (Compartir → Añadir a pantalla de inicio) y luego activar; en **Android instalable**, además una tarjeta de **instalar la app** con botón que dispara el prompt (`beforeinstallprompt`). Las tarjetas se ocultan cuando no aplican (no soportado, ya activo, permiso denegado, ya instalado).
- **Sin datos sensibles en la carga**: el push muestra título+cuerpo cortos (p. ej. "Tu plaza P‑12 del 30/07 está confirmada"); el detalle vive en la app al abrir.

## Capabilities

### New Capabilities
- `push-notifications`: canal Web Push — claves VAPID, tabla de suscripciones, endpoints de alta/baja, envío de push a los dispositivos de un empleado, limpieza de suscripciones muertas, y el Service Worker + flujo de permiso/suscripción en el cliente.

### Modified Capabilities
- `notifications`: se generaliza de "email" a **multi‑canal**. Cada evento se entrega por los canales **habilitados globalmente**; el email deja de ser el único canal. Se añade el fan‑out a **admins** para solicitudes en modo `MANUAL`.
- `system-settings`: nuevos flags globales `emailNotificationsEnabled` (por defecto `true`, retrocompatible) y `pushNotificationsEnabled` (por defecto `true`), legibles/escribibles por `ADMIN`; gobiernan si cada canal envía.
- `employees`: cada empleado gana flags `emailNotificationsEnabled`/`pushNotificationsEnabled` (por defecto `true`), editables por el ADMIN en el formulario de empleado; la entrega respeta el AND de la preferencia global y la del empleado.
- `app-shell`: la SPA registra un Service Worker y un `manifest` (PWA instalable), condición necesaria para Web Push (y para iOS ≥16.4).

## Impact

- **Backend**: dependencia `nl.martijndwars:web-push` (o equivalente); config VAPID (`VAPID_PUBLIC_KEY`, `VAPID_PRIVATE_KEY`, `VAPID_SUBJECT`); migraciones: `push_subscription`, columnas de canal en `system_settings` **y en `employees`**; `PushNotificationListener` (AFTER_COMMIT) + `WebPushSenderPort`/adaptador; endpoints `POST/DELETE /push/subscriptions` y `GET /push/vapid-public-key`; extensión de `SystemSettings`, `Employee` y del render de contenido; los listeners aplican el AND (global ∧ empleado) antes de enviar. Fan‑out a admins vía `EmployeeRepository.findByRoleAndActiveTrue(ADMIN)`.
- **Frontend**: `vite-plugin-pwa` (o Service Worker a mano) + `manifest.webmanifest`; `VITE_VAPID_PUBLIC_KEY` en entorno; flujo de permiso/suscripción (`Notification.requestPermission` → `PushManager.subscribe`) → `settingsApi`/hooks; toggle de "Notificaciones push" en perfil, checkboxes de canal global en `SettingsPage` **y checkboxes email/push por empleado en `EmployeeFormModal`**; manejo de estados (no soportado, denegado, iOS sin instalar).
- **Docs**: `docs/openapi.yaml` (endpoints y schemas de suscripción + flags), `docs/data-model.md` (tabla `push_subscription` y columnas nuevas), `docs/security-design.md` (VAPID, autenticación de la suscripción, RGPD del endpoint).
- **Entorno/CI**: claves VAPID como secretos (mismo `.env` gitignored que SMTP); la privada nunca se commitea.

## Out of scope

- **Notificación nativa iOS/Android sin instalar** (APNs/FCM, app nativa): no es gratis ni es este stack. En iPhone, Web Push exige que el usuario **instale la PWA** (Añadir a pantalla de inicio, iOS ≥16.4).
- **Preferencias por‑usuario y por‑tipo de evento** (matriz canal×evento por empleado): aquí los toggles son **globales** por canal (decisión del ADMIN) + opt‑in de push por dispositivo. La granularidad fina queda para un change posterior.
- **Reintentos con outbox para push** (equivalente a `email_outbox`): en v1 el push es best‑effort con borrado de suscripciones muertas; el outbox de reintento transitorio se puede endurecer después.
- **Sustituir el email**: el email se mantiene como canal y fallback; este change lo complementa, no lo elimina.
