## Why

El empleado enviaba una solicitud sin saber si iba a quedar `APPROVED` al instante o `PENDING` a la espera de un admin: el modo de aprobación global (`approvalMode`) solo era legible por el `ADMIN` (`GET /admin/settings`, 403 para `EMPLOYEE`), así que el modal de solicitud no podía anticipar el resultado. Una vez creada una `PENDING`, si el admin tardaba, el empleado no tenía forma de reavisar salvo esperar. Y "Mi Semana" mostraba la plaza y el puesto por separado, sin una acción de reserva/liberación inmediata ni un punto único que respondiera "¿qué tengo hoy y mañana?" de un vistazo.

## What Changes

- **Lectura del modo de aprobación por cualquier autenticado.** Nuevo endpoint `GET /api/v1/settings/approval-mode`, sin restricción de rol (`EMPLOYEE`/`AGENCIA`/`ADMIN`), que expone solo `approvalMode` (sin `updatedById`/`updatedAt`, que siguen reservados a `GET /admin/settings`). Adaptador propio (`ApprovalModeController` + `ApprovalModeResponse`), sin tocar el endpoint ADMIN existente.
- **Modal de solicitud con aviso explícito según el modo vigente.** Consultando el nuevo endpoint, el modal muestra un aviso verde ("se confirma al instante") en modo `AUTOMATIC` o un aviso ámbar ("quedará pendiente de aprobación; tu plaza o puesto exacto puede cambiar") en modo `MANUAL`.
- **Reenvío de una solicitud `PENDING` propia estancada.** Nuevo `POST /requests/{id}/resend`: solo el dueño, solo si sigue `PENDING`, solo tras un cooldown de 24h desde la creación (o el último reenvío). Reutiliza el evento `RequestCreatedEvent` para re-notificar a todos los admins activos exactamente igual que en la creación. Persiste el instante del último reenvío (`last_reminded_at`, migración `V26`). Nuevos códigos de error 409 `REQUEST_NOT_PENDING` y `RESEND_TOO_SOON`.
- **Banner de solicitud pendiente con reenvío en un toque.** `PendingConfirmationBanner` (ámbar) integrado en "Mis solicitudes": mientras no se cumple el cooldown muestra las horas restantes; al cumplirse, ofrece el botón "Reenviar solicitud".
- **Rediseño de "Mi Semana" con héroe HOY/MAÑANA.** Encima de la tira semanal navegable, dos tarjetas (HOY, MAÑANA) muestran de un vistazo el estado de la plaza y del puesto del empleado; un recurso libre ofrece reservar en 1 toque (preselecciona fecha y tipo de recurso en el modal); un recurso con solicitud `PENDING` muestra el aviso de pendiente en línea. Una acción principal "Reservar" queda siempre visible (fija en móvil) debajo de la tira semanal.

## Capabilities

### Modified Capabilities
- `system-settings`: el modo de aprobación global gana una vía de lectura sin restricción de rol (`GET /settings/approval-mode`), distinta y más restringida en payload que la lectura `ADMIN` existente (`GET /admin/settings`).
- `requests`: nueva operación `POST /requests/{id}/resend` (reenvío de aviso de una `PENDING` propia con cooldown de 24h) y nuevos códigos de error 409 asociados.
- `employee-portal`: "Mi Semana" incorpora un héroe HOY/MAÑANA con reserva/liberación en 1 toque y una acción principal de reserva siempre visible; el modal de solicitud unificada comunica el resultado esperado (automático/pendiente) antes del envío.
- `notifications`: el reenvío de aviso (`resend`) reutiliza el mismo evento y plantilla que la creación de solicitud (`REQUEST_CREATED`/`request-created.html`), re-notificando a los mismos destinatarios (admins activos) sin plantilla nueva.

## Impact

- **Backend:** `systemsettings/ApprovalModeController.java` + `dto/ApprovalModeResponse.java` (endpoint nuevo, sin nuevo rol); `request/application/RequestService.java` (método `resend`, `RESEND_COOLDOWN` = 24h), `request/domain/Request.java` (`lastRemindedAt`, `canBeResent`, `markReminded`), `request/RequestController.java` (`POST /requests/{id}/resend`, solo `EMPLOYEE`); `RequestNotPendingException`/`ResendTooSoonException` + `GlobalExceptionHandler` (409 `REQUEST_NOT_PENDING`/`RESEND_TOO_SOON`); migración `V26__request_last_reminded_at.sql` (`last_reminded_at DATETIME2(3) NULL`); `RequestResponse`/`RequestEntity`/`RequestMapper` extendidos con `lastRemindedAt`. `docs/openapi.yaml` actualizado con ambos endpoints.
- **Frontend:** `hooks/useSettings.ts` (`useApprovalModeQuery`) + `api/settingsApi.ts` (`getApprovalMode` sobre el endpoint nuevo); `components/CreateRequestModal.tsx` (`ApprovalModeNotice`: aviso automatic/manual); `components/PendingConfirmationBanner.tsx` (nuevo) + `hooks/useRequests.ts` (`useResendRequest`) + `api/requestsApi.ts` (`resendRequest`) + `utils/requests.ts` (`canResendRequest`, `resendCooldownRemainingMs`); `pages/MyRequestsPage.tsx` (integra el banner); `pages/MyWeekPage.tsx` (héroe HOY/MAÑANA, `renderHeroCard`/`renderHeroResource`, acción "Reservar" fija); `styles/components.css` (estilos del héroe y del banner); i18n `es`/`en`.
- **Datos:** migración `V26` añade `last_reminded_at` a `requests` (nullable, retrocompatible).
- **Fuera de alcance:** lista de espera (capability `waitlist-requests`, change ya existente y separado).
