# Tasks — employee-reservation-clarity

Reconstrucción as-built (commit `25f4939`): todas las tareas ya están implementadas y verificadas (`mvn verify` BUILD SUCCESS, 232 ITs verdes incluyendo `RequestResendIT` 6/6, cobertura JaCoCo cumplida; frontend lint + build OK, tests de los ficheros tocados verdes).

## 1. Lectura del modo de aprobación por cualquier autenticado

- [x] 1.1 `ApprovalModeResponse` (DTO mínimo: solo `approvalMode`, sin trazabilidad)
- [x] 1.2 `ApprovalModeController` — `GET /api/v1/settings/approval-mode`, sin restricción de rol (cualquier autenticado)
- [x] 1.3 Documentar el endpoint en `docs/openapi.yaml`
- [x] 1.4 Tests backend (`ApprovalModeControllerTest`): 200 para `EMPLOYEE`/`AGENCIA`/`ADMIN`, 401 sin sesión

## 2. Reenvío de solicitud pendiente (backend)

- [x] 2.1 Migración `V26__request_last_reminded_at.sql` (`last_reminded_at DATETIME2(3) NULL`)
- [x] 2.2 `Request.lastRemindedAt` + `canBeResent(now, cooldown)` + `markReminded(now)` (dominio)
- [x] 2.3 `RequestEntity`/`RequestMapper`/`RequestResponse` extendidos con `lastRemindedAt`
- [x] 2.4 `RequestNotPendingException` / `ResendTooSoonException`
- [x] 2.5 `RequestService.resend(id, requesterLogin)`: verificación de pertenencia (BOLA), solo `PENDING`, cooldown 24h (`RESEND_COOLDOWN`), reutiliza `RequestCreatedEvent`
- [x] 2.6 `RequestController` — `POST /requests/{id}/resend` (`@PreAuthorize("hasRole('EMPLOYEE')")`)
- [x] 2.7 `GlobalExceptionHandler` — 409 `REQUEST_NOT_PENDING` / `RESEND_TOO_SOON`
- [x] 2.8 Documentar el endpoint y los códigos de error en `docs/openapi.yaml`
- [x] 2.9 Tests backend (`RequestServiceTest`, `RequestControllerTest`, `RequestResendIT` 6/6): happy path, no-dueño (403), no-`PENDING` (409), cooldown no cumplido (409), cooldown cumplido tras reenvío previo

## 3. Modal de solicitud — aviso automático/pendiente

- [x] 3.1 `api/settingsApi.ts` — `getApprovalMode()` sobre `GET /settings/approval-mode`
- [x] 3.2 `hooks/useSettings.ts` — `useApprovalModeQuery()`
- [x] 3.3 `CreateRequestModal` — `ApprovalModeNotice` (aviso verde "se confirma al instante" en `AUTOMATIC`; aviso ámbar "quedará pendiente… puede cambiar" en `MANUAL`)
- [x] 3.4 i18n `es`/`en` (`requests.create.automaticNotice` / `manualNotice`)

## 4. Banner de reenvío en "Mis solicitudes"

- [x] 4.1 `utils/requests.ts` — `canResendRequest`, `resendCooldownRemainingMs` (cooldown 24h sobre `max(createdAt, lastRemindedAt)`)
- [x] 4.2 `api/requestsApi.ts` — `resendRequest(id)` (`POST /requests/{id}/resend`)
- [x] 4.3 `hooks/useRequests.ts` — `useResendRequest()`
- [x] 4.4 `components/PendingConfirmationBanner.tsx` — banner ámbar reutilizable: horas restantes o botón "Reenviar solicitud"; toasts diferenciados (403 ajena, 409 `RESEND_TOO_SOON`, 409 ya resuelta)
- [x] 4.5 Integrar el banner en `pages/MyRequestsPage.tsx`
- [x] 4.6 Tests frontend (`PendingConfirmationBanner.test.tsx`, `utils/requests.test.ts`)
- [x] 4.7 i18n `es`/`en` (`requests.pendingBanner.*`, `requests.errors.resendForbidden`/`resendTooSoon`)

## 5. Rediseño de "Mi Semana" — héroe HOY/MAÑANA

- [x] 5.1 Consultar la semana actual y la siguiente (`useMyWeekQuery` ×2) para derivar `todayDay`/`tomorrowDay` sin importar el borde domingo→lunes
- [x] 5.2 `renderHeroCard`/`renderHeroResource`: tarjetas HOY/MAÑANA con plaza y puesto por separado, aviso de pendiente en línea (con `WaitlistBadge` cuando aplica)
- [x] 5.3 Reserva en 1 toque desde un recurso libre del héroe: preselecciona fecha y tipo de recurso (`ReservePreset`) al abrir `CreateRequestModal`
- [x] 5.4 Liberación en 1 toque desde el héroe: mismo mecanismo que la tira semanal (`resolveReleaseAction` — cancelar solicitud propia o liberar asignación fija)
- [x] 5.5 Acción principal "Reservar" siempre visible debajo de la tira semanal (fija en móvil, `mw-cta`)
- [x] 5.6 Estilos del héroe y del CTA fijo (`styles/components.css`)
- [x] 5.7 i18n `es`/`en` (`calendar.myWeek.hero.*`)

## 6. Cierre

- [x] 6.1 `mvn clean verify` (JDK 21): BUILD SUCCESS, 232 ITs verdes, cobertura JaCoCo cumplida
- [x] 6.2 Frontend: `npm run lint && npm run build` sin errores; tests de los ficheros tocados verdes
- [x] 6.3 `docs/openapi.yaml` actualizado con `GET /settings/approval-mode` y `POST /requests/{id}/resend`
