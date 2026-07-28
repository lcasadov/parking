## Context

El backend ya tiene un pipeline de notificaciones dirigido por eventos: los servicios publican eventos de dominio (`RequestApprovedEvent`, `RequestRejectedEvent`, `RequestAdminAssignedEvent`, `RequestCancelledEvent`, `RequestCreatedEvent`, `WaitlistAvailableEvent`, `FixedAssignmentRevokedEvent`) y un `EmailNotificationListener` (`@TransactionalEventListener(AFTER_COMMIT)`) los transforma en emails, con `email_outbox` + `EmailRetryJob` para reintentos. El frontend es una SPA Vite/React **sin Service Worker ni manifest** (no es PWA todavía). Web Push (VAPID) es el estándar del navegador, gratuito, y encaja como un **segundo canal** sobre los mismos eventos.

## Goals / Non-Goals

**Goals:**
- Avisos en tiempo real, sin coste, reutilizando los eventos existentes sin tocar la lógica de negocio.
- Empleado avisado de: confirmación (aprobada/auto/asignación puntual), rechazo, cambio (reasignación/swap) y cancelación admin de una aprobada.
- Admin avisado de nuevas solicitudes en modo `MANUAL` (fan-out a todos los admins activos).
- Dos toggles de canal **independientes** en Configuración (email, push), por defecto ambos `true` (retrocompatible).
- Opt-in de push por usuario/dispositivo, con múltiples dispositivos por usuario y limpieza de suscripciones muertas.
- Email intacto como canal y fallback.

**Non-Goals:**
- Notificación nativa iOS/Android (APNs/FCM). En iPhone, Web Push requiere PWA instalada (iOS ≥16.4).
- Preferencias por-usuario y por-tipo (matriz canal×evento por empleado): aquí es global por canal + opt-in de push.
- Outbox de reintentos para push en v1 (best-effort + borrado de muertas).

## Decisions

### D1 — Segundo listener en paralelo, no reescribir el de email
`PushNotificationListener` (`@TransactionalEventListener(AFTER_COMMIT)`) consume los mismos eventos que `EmailNotificationListener`. Ambos son independientes: si push falla, el email sigue, y viceversa. Se evita acoplar canales en un único listener. La resolución de destinatario(s) por evento se factoriza a un colaborador reutilizable (`NotificationRecipientResolver`) que ambos listeners consultan, para no duplicar la lógica de "a quién va cada evento".

### D2 — Toggles de canal en `system_settings` (global), no por usuario
Dos columnas `email_notifications_enabled BIT NOT NULL DEFAULT 1` y `push_notifications_enabled BIT NOT NULL DEFAULT 1`. Antes de enviar, cada listener comprueba su flag; si está `false`, no envía. Son **independientes**: (on,on)=ambos, (on,off)/(off,on)=uno, (off,off)=ninguno (permitido; el admin asume el silencio). Se resuelven desde `SystemSettings` (fila única, con default retrocompatible si la fila no existe).

### D3 — Push condicionado a suscripción del usuario
Un push a un empleado se envía a **todas** sus suscripciones activas. Si el empleado no tiene suscripción (no concedió permiso, navegador no soportado, o se dio de baja), simplemente no recibe push — el email cubre el aviso si está habilitado. No es un error.

### D4 — Tabla `push_subscription`
`id`, `employee_id` (FK), `endpoint` (único), `p256dh`, `auth`, `user_agent` (opcional, para que el usuario reconozca el dispositivo), `created_at`. Unicidad por `endpoint` (idempotente: re-suscribir el mismo navegador actualiza, no duplica). Índice por `employee_id` (fan-out y borrado en cascada al desactivar el empleado).

### D5 — Ciclo de vida de suscripciones
- **Alta:** `POST /push/subscriptions` (autenticado; el empleado solo puede registrar la suya) con `{endpoint, keys:{p256dh, auth}}`. Upsert por `endpoint`.
- **Baja:** `DELETE /push/subscriptions` (por endpoint del propio usuario) al desactivar el toggle o cerrar sesión.
- **Muerta:** si el push service responde **404/410 (Gone)**, el sender borra esa suscripción. Un 413/429/5xx transitorio se ignora en v1 (best-effort; log a nivel WARN).
- **Rotación/caducidad:** el cliente detecta `pushsubscriptionchange` (o un 410 al usar) y re-suscribe; el backend hace upsert.

### D6 — Fan-out a admins solo en `MANUAL`
`onRequestCreated`: si `SystemSettings.approvalMode == MANUAL`, resolver **todos** los admins activos (`EmployeeRepository.findByRoleAndActiveTrue(ADMIN)`) y notificarlos (push a sus suscripciones + email si habilitado). En `AUTOMATIC` no se avisa al admin (la solicitud nace aprobada; el empleado recibe su confirmación). No se auto-notifica al empleado creador por este evento (ya tiene su feedback en la app; el email de "creada" se mantiene como está).

### D7 — Contenido del push: mínimo y sin datos sensibles de más
El payload Web Push va cifrado extremo-a-extremo, pero el texto se muestra en pantalla de bloqueo. Se envía **título + cuerpo cortos** por tipo de evento (p. ej. "Reserva confirmada" · "Tu plaza P·12 del 30/07 está confirmada") + un `data.url` para el deep-link. Se reutiliza `NotificationRenderer`/i18n; el push no incluye adjuntos ni PII innecesaria. Idioma: el del empleado destino (mismo criterio que el email).

### D8 — Service Worker y PWA en el frontend
Se añade `vite-plugin-pwa` (o SW a mano) que registra un Service Worker con handlers `push` (muestra `showNotification` con título/cuerpo/icon/data) y `notificationclick` (abre/enfoca la pestaña y navega a `data.url`). `manifest.webmanifest` mínimo (nombre, iconos, `display: standalone`) — necesario para instalar en iOS. El registro del SW es a nivel `app-shell`.

### D9 — Flujo de permiso/suscripción (cliente)
Toggle "Notificaciones push" en el perfil del usuario: al activar → `Notification.requestPermission()` → si `granted`, `registration.pushManager.subscribe({userVisibleOnly:true, applicationServerKey: <VAPID pública>})` → `POST /push/subscriptions`. Estados de UI: **no soportado** (sin `PushManager`), **denegado** (permiso bloqueado → guiar a ajustes del navegador), **iOS sin instalar** (mostrar "Añade la app a tu pantalla de inicio para activar push"), **activo** (mostrar dispositivo y opción de baja).

### D10 — VAPID
Un único par por instalación (por entorno). Pública expuesta al cliente (`GET /push/vapid-public-key` o `VITE_VAPID_PUBLIC_KEY`), privada + subject secretas en backend (`.env` gitignored, junto a SMTP). Rotar la privada invalida todas las suscripciones (se documenta como operación excepcional).

### D11 — Eventos cubiertos (matriz)
| Evento | Destinatario | Condición |
|---|---|---|
| `RequestApprovedEvent` | empleado | siempre |
| `RequestRejectedEvent` | empleado | siempre |
| `RequestAdminAssignedEvent` | empleado | siempre (reasignación/swap/asignación puntual) |
| `RequestCancelledEvent` | empleado | cancelación admin de aprobada |
| `WaitlistAvailableEvent` | empleado | se libera hueco de su lista de espera |
| `RequestCreatedEvent` | **admins activos** | solo si `approvalMode == MANUAL` |
| `FixedAssignmentRevokedEvent` | empleado | (opcional; ya tiene email — se puede incluir) |

## Risks / Trade-offs

- **iOS/Safari:** Web Push solo desde iOS ≥16.4 y **con la PWA instalada**. Riesgo de expectativa: se mitiga con UI que detecta y guía; el email cubre a quien no pueda/quiera push.
- **Best-effort sin outbox (v1):** un push perdido por caída transitoria del push service no se reintenta. Aceptable porque el email (si habilitado) es el canal fiable; el outbox de push queda como endurecimiento futuro.
- **Doble notificación:** con ambos canales on, el usuario recibe email + push. Es intencional (push inmediato, email como registro); si molesta, el admin apaga uno.
- **(off,off):** el admin puede dejar el sistema sin avisos. Se permite (decisión suya); la UI puede mostrar un aviso informativo, sin bloquear.
- **Privacidad/RGPD:** `push_subscription.endpoint` es un identificador de dispositivo ligado al empleado → se borra al dar de baja, al cerrar sesión y **en cascada al desactivar el empleado**; fuera del purgado histórico de 2 años (es estado vivo, no auditoría).
- **Seguridad:** el alta de suscripción exige sesión y solo permite registrar la del propio usuario (no la de otro `employee_id`). La pública VAPID no es secreta; la privada sí.
- **Dependencia nueva:** `web-push` en backend (revisar CVEs); `vite-plugin-pwa` en frontend (aumenta el bundle con el SW, pero es marginal).

## Migration Plan

1. Migración BD: crear `push_subscription`; añadir `email_notifications_enabled`/`push_notifications_enabled` a `system_settings` (default 1 → retrocompatible: comportamiento actual = ambos on).
2. Desplegar backend con VAPID configurado (si falta la privada, push se desactiva de facto y solo va email — arranque tolerante).
3. Desplegar frontend PWA; los usuarios existentes siguen sin push hasta que opten in (no intrusivo).
4. Sin backfill: no hay suscripciones previas; el email sigue exactamente como hoy.
