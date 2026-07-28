# Tasks — push-notifications

Orden sugerido: BD → dominio/config → envío → listener → endpoints → frontend PWA → UI → docs → tests → gates. Backend con `JAVA_HOME=<jdk21>`.

## 1. Datos y configuración (backend)

- [x] 1.1 Migración `V__push_subscription.sql`: tabla `push_subscription` (`id`, `employee_id` FK→employees, `endpoint` UNIQUE, `p256dh`, `auth`, `user_agent` NULL, `created_at`), índice por `employee_id`; FK con borrado en cascada al desactivar/eliminar empleado (o borrado explícito en el servicio).
- [x] 1.2 Migración `V__system_settings_notification_channels.sql`: añadir `email_notifications_enabled BIT NOT NULL DEFAULT 1` y `push_notifications_enabled BIT NOT NULL DEFAULT 1`.
- [x] 1.2b Migración `V__employees_notification_prefs.sql`: añadir `email_notifications_enabled BIT NOT NULL DEFAULT 1` y `push_notifications_enabled BIT NOT NULL DEFAULT 1` a `employees` (por defecto activos).
- [x] 1.3 Config VAPID: `VAPID_PUBLIC_KEY`, `VAPID_PRIVATE_KEY`, `VAPID_SUBJECT` vía entorno (`.env` gitignored, junto a SMTP); arranque tolerante si falta la privada (push desactivado de facto, solo email). Documentar en `.env.example`.
- [x] 1.4 Dependencia `nl.martijndwars:web-push` (o equivalente) en `backend/pom.xml`.

## 2. Dominio y persistencia (backend)

- [x] 2.1 `SystemSettings`: campos `emailNotificationsEnabled`/`pushNotificationsEnabled` (+ restore/defaults/getters/método de cambio), entidad JPA, mapper, DTOs `SystemSettingsResponse`/nuevo `UpdateNotificationChannelsRequest`.
- [x] 2.2 `PushSubscription` (dominio + puerto `PushSubscriptionRepositoryPort`) + entidad JPA + adaptador + mapper. Upsert por `endpoint`.
- [x] 2.3 `NotificationRecipientResolver` reutilizable: resuelve destinatario(s) por evento (empleado del evento; admins activos para `RequestCreated` en modo MANUAL) — consumido por ambos listeners. Aplica la **regla de entrega efectiva** por canal y destinatario: `global.<canal> ∧ empleado.<canal>` (y suscripción para push).
- [x] 2.4 `Employee`: campos `emailNotificationsEnabled`/`pushNotificationsEnabled` (default `true`) en dominio, entidad JPA, mapper y DTOs de empleado (respuesta + create/update); RBAC: solo `ADMIN` los cambia.

## 3. Envío push (backend)

- [x] 3.1 `WebPushSenderPort` + adaptador con la librería web-push (firma VAPID, cifrado del payload).
- [x] 3.2 Borrado de suscripciones muertas ante `404/410`; log WARN (sin borrar) ante `5xx`/red.
- [x] 3.3 `PushContentRenderer` (título+cuerpo cortos por tipo de evento, i18n del destino, `data.url` de deep-link). Reutilizar `NotificationRenderer` donde aplique.

## 4. Orquestación por eventos (backend)

- [x] 4.1 `PushNotificationListener` (`@TransactionalEventListener(AFTER_COMMIT)`) para `RequestApproved`, `RequestRejected`, `RequestAdminAssigned`, `RequestCancelled`, `WaitlistAvailable`; comprueba `pushNotificationsEnabled` antes de enviar.
- [x] 4.2 `onRequestCreated`: si `approvalMode == MANUAL`, fan-out a admins activos (push + email según flags); si `AUTOMATIC`, no avisar al admin.
- [x] 4.3 El `EmailNotificationListener` pasa a comprobar `emailNotificationsEnabled`; añadir el fan-out a admins en modo MANUAL para email también (coherencia de canal).
- [x] 4.4 **Aviso al empleado en cancelación admin** (design D12): `adminCancel` publica un evento dedicado (p. ej. `RequestAdminCancelledEvent`) dirigido al **empleado afectado**; ambos listeners (email + push) lo notifican al empleado. Se mantiene el fan-out a admins de `RequestCancelled` para la liberación del recurso; la cancelación del propio empleado NO se auto-notifica.

## 5. Endpoints (backend)

- [ ] 5.1 `POST /push/subscriptions` (autenticado; solo la propia) — upsert por endpoint.
- [ ] 5.2 `DELETE /push/subscriptions` (por endpoint del propio usuario).
- [ ] 5.3 `GET /push/vapid-public-key` (o exponer por build).
- [ ] 5.4 `PUT /admin/settings/notification-channels` (ADMIN) — cambia los dos flags; `GET /admin/settings` los devuelve.
- [ ] 5.5 Borrado de suscripciones al desactivar un empleado (cascada o en `EmployeeService`).

## 6. Frontend — PWA y suscripción

- [ ] 6.1 `vite-plugin-pwa` (o SW a mano) + `manifest.webmanifest` (iconos, `display: standalone`).
- [ ] 6.2 Service Worker: handlers `push` (`showNotification`) y `notificationclick` (focus/navegación a `data.url`).
- [ ] 6.3 `VITE_VAPID_PUBLIC_KEY` en entorno; helper de suscripción (`requestPermission` → `pushManager.subscribe` → `POST /push/subscriptions`).
- [ ] 6.4 Manejo de `pushsubscriptionchange` / 410 → re-suscribir (upsert en backend).
- [ ] 6.5 `settingsApi`/hooks para suscripción y para los flags de canal.
- [ ] 6.6 Detección de estado/plataforma (`standalone`, iOS vs Android, `pushSupported`, `Notification.permission`) y captura del evento `beforeinstallprompt` (guardar el `deferredPrompt`).

## 7. Frontend — UI

- [ ] 7.1 `SettingsPage` (ADMIN): dos checkboxes independientes **Email** y **Push** globales (guardado por `PUT /admin/settings/notification-channels`).
- [ ] 7.1b `EmployeeFormModal` (ADMIN): dos checkboxes independientes **Email** y **Push** por empleado (activos por defecto), guardados con el resto del formulario del empleado.
- [ ] 7.2 Perfil de usuario: toggle "Notificaciones push" con estados (no soportado / denegado / iOS sin instalar / activo + dispositivo) y baja.
- [ ] 7.3 **Tarjetas de onboarding en la página principal** (sin auto-prompt), llamativas y descartables, según D14: "Activar notificaciones" (botón → prompt nativo), "Instalar la app" iOS (instrucciones), "Instalar la app" Android (botón → `deferredPrompt.prompt()`); ocultar cuando no aplican; recordar el descarte.
- [ ] 7.4 i18n (es/en) de checkboxes, toggle, tarjetas de onboarding, estados y textos de las notificaciones.

## 8. Docs

- [ ] 8.1 `docs/openapi.yaml`: endpoints de suscripción + VAPID + `notification-channels`; schemas `PushSubscriptionRequest`, flags en `SystemSettings` y en el schema de `Employee` (respuesta + create/update).
- [ ] 8.2 `docs/data-model.md`: tabla `push_subscription`, columnas nuevas de `system_settings` y de `employees`.
- [ ] 8.3 `docs/security-design.md`: VAPID (secretos), autenticación del alta de suscripción, RGPD del `endpoint`.

## 9. Tests

- [ ] 9.1 Backend unit: resolver de destinatarios (empleado / fan-out admins en MANUAL / no-admin en AUTOMATIC); regla de entrega efectiva `global ∧ empleado` por canal (incl. empleado silenciado); aviso al empleado en cancelación admin y no-autoaviso en cancelación propia; borrado ante 410.
- [ ] 9.2 Backend web (`@WebMvcTest`): RBAC de los endpoints (401/403), alta idempotente, baja, cambio de flags globales solo ADMIN, flags por empleado en el formulario solo ADMIN.
- [ ] 9.3 Backend IT (Testcontainers): persistencia y upsert de `push_subscription`; borrado en cascada al desactivar empleado.
- [ ] 9.4 Frontend: flujo de permiso (mock `Notification`/`PushManager`), estados de la UI, checkboxes de canal global (guardan por separado), checkboxes por empleado en `EmployeeFormModal`, toggle push, y **tarjetas de onboarding** por plataforma/estado (mock de `standalone`/UA/`beforeinstallprompt`; verificar que no hay auto-prompt).
- [ ] 9.5 Cobertura ≥ umbrales (líneas ≥80 / branches ≥75 / funciones ≥80) y sin regresión.

## 10. Gates (verificación final)

- [ ] 10.1 Backend (JDK 21): `mvn clean verify` verde; ArchUnit/Sonar sin violations nuevas.
- [ ] 10.2 Frontend: `npm run lint && npm test && npm run build` sin errores.
- [ ] 10.3 Prueba manual en Chrome/Firefox (suscribir, recibir push, deep-link) y verificación de fallback email con push apagado.
- [ ] 10.4 Actualizar este `tasks.md` y archivar el change tras merge.
