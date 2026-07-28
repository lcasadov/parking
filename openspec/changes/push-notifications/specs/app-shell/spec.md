## ADDED Requirements

### Requirement: PWA con Service Worker para Web Push

La SPA SHALL registrar un Service Worker y servir un `manifest.webmanifest` (app instalable, `display: standalone`), condición necesaria para Web Push (y para iOS ≥16.4, que exige la PWA instalada). El Service Worker SHALL manejar el evento `push` (mostrando la notificación con título, cuerpo, icono y `data`) y el evento `notificationclick` (abriendo o enfocando la app y navegando al `data.url`).

#### Scenario: Recepción de push con la pestaña cerrada

- **GIVEN** un usuario suscrito con la pestaña de la app cerrada
- **WHEN** el backend envía un push a su suscripción
- **THEN** el Service Worker despierta y muestra la notificación del sistema

#### Scenario: Clic en la notificación

- **WHEN** el usuario pulsa la notificación
- **THEN** el Service Worker abre o enfoca la app y navega a la pantalla indicada en `data.url`

### Requirement: Gestión del permiso y estado de push en el cliente

La app SHALL ofrecer al usuario un control para activar/desactivar las notificaciones push y SHALL reflejar el estado real del navegador: no soportado (sin `PushManager`), permiso denegado, iOS sin instalar (guiar a "Añadir a pantalla de inicio"), o activo. Activar SHALL solicitar el permiso y, si se concede, suscribir con la clave pública VAPID y registrar la suscripción en el backend.

#### Scenario: Activar push concediendo permiso

- **GIVEN** un navegador compatible sin suscripción
- **WHEN** el usuario activa las notificaciones push y concede el permiso
- **THEN** la app suscribe con la clave pública VAPID y registra la suscripción en el backend

#### Scenario: Permiso denegado

- **GIVEN** un usuario que ha bloqueado las notificaciones en el navegador
- **WHEN** intenta activarlas
- **THEN** la app no suscribe y muestra cómo reactivar el permiso en los ajustes del navegador

#### Scenario: Navegador o plataforma no compatible

- **GIVEN** un navegador sin `PushManager` o un iPhone con la web sin instalar
- **THEN** la app muestra el estado correspondiente y no ofrece la suscripción (el email cubre el aviso)
