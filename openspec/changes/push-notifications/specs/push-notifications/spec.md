## ADDED Requirements

### Requirement: Suscripción Web Push por usuario y dispositivo

El sistema SHALL permitir a cualquier usuario autenticado (`EMPLOYEE`, `AGENCIA`, `ADMIN`) registrar la suscripción Web Push de su navegador (`endpoint` + claves `p256dh`/`auth`), persistida en `push_subscription` y ligada a su `employee_id`. Un usuario SHALL poder tener varias suscripciones (varios dispositivos). El alta SHALL ser idempotente por `endpoint` (re-registrar el mismo navegador actualiza, no duplica). Un usuario SHALL poder registrar únicamente su propia suscripción, nunca la de otro empleado.

#### Scenario: Alta de suscripción

- **GIVEN** un usuario autenticado que concede el permiso de notificaciones en su navegador
- **WHEN** envía `POST /push/subscriptions` con `{endpoint, keys:{p256dh, auth}}`
- **THEN** el sistema persiste la suscripción ligada a su `employee_id` y responde 201/200

#### Scenario: Re-suscripción idempotente

- **GIVEN** un navegador ya suscrito
- **WHEN** el mismo `endpoint` se registra de nuevo
- **THEN** el sistema actualiza la suscripción existente sin crear un duplicado

#### Scenario: Baja de suscripción

- **WHEN** el usuario desactiva las notificaciones push o cierra sesión y su cliente envía `DELETE /push/subscriptions` con su `endpoint`
- **THEN** el sistema elimina esa suscripción

### Requirement: Clave pública VAPID disponible para el cliente

El sistema SHALL exponer la clave pública VAPID al cliente (endpoint dedicado o variable de build) para que el navegador pueda suscribirse. La clave privada y el subject VAPID SHALL permanecer secretos en el backend y NUNCA exponerse al cliente.

#### Scenario: El cliente obtiene la clave pública

- **WHEN** el cliente solicita la clave pública VAPID
- **THEN** el sistema responde con la clave pública en base64url; la privada nunca sale del backend

### Requirement: Envío de push a los dispositivos de un usuario

El sistema SHALL enviar una notificación Web Push cifrada a **todas** las suscripciones activas del usuario destino cuando un evento de notificación lo requiera y el canal push esté habilitado globalmente. Si el usuario no tiene suscripciones, el sistema NO SHALL tratarlo como error (el email cubre el aviso si está habilitado). El envío SHALL ser best-effort e independiente del canal email.

#### Scenario: Usuario con varios dispositivos

- **GIVEN** un usuario con dos suscripciones activas y el canal push habilitado
- **WHEN** se dispara una notificación dirigida a ese usuario
- **THEN** el sistema envía el push a las dos suscripciones

#### Scenario: Usuario sin suscripción

- **GIVEN** un usuario destino sin ninguna suscripción push
- **WHEN** se dispara una notificación dirigida a ese usuario
- **THEN** el sistema no envía push y no produce error; el email se envía si su canal está habilitado

### Requirement: Limpieza de suscripciones muertas

El sistema SHALL eliminar automáticamente una suscripción cuando el servicio de push responda `404` o `410 (Gone)` al intentar entregar. Un fallo transitorio (`5xx`/red) NO SHALL borrar la suscripción; en v1 el push es best-effort sin reintento con outbox.

#### Scenario: Suscripción caducada

- **GIVEN** una suscripción cuyo `endpoint` ya no es válido
- **WHEN** el envío recibe `410 Gone` del servicio de push
- **THEN** el sistema borra esa suscripción y continúa con las demás

### Requirement: Contenido mínimo y sin datos sensibles innecesarios

La notificación push SHALL contener un título y un cuerpo breves (por tipo de evento, en el idioma del usuario destino) y un `data.url` para el deep-link, sin adjuntos ni datos personales más allá de lo imprescindible para identificar la reserva.

#### Scenario: Deep-link al abrir

- **WHEN** el usuario pulsa una notificación push
- **THEN** el Service Worker abre o enfoca la app y navega a la pantalla relevante (`data.url`)

### Requirement: RGPD y borrado en cascada

El sistema SHALL eliminar las suscripciones push de un empleado cuando este se desactiva, además de al darse de baja o cerrar sesión. `push_subscription` es estado vivo (no auditoría) y queda fuera del purgado histórico de 2 años.

#### Scenario: Empleado desactivado

- **WHEN** un ADMIN desactiva a un empleado
- **THEN** el sistema elimina todas las suscripciones push de ese empleado
