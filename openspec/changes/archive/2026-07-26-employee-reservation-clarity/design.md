## Context

`approvalMode` (`MANUAL`/`AUTOMATIC`) determina si una `Request` nace `PENDING` o `APPROVED`, pero solo era legible por `GET /admin/settings` (`ADMIN`-only, 403 para `EMPLOYEE`). El empleado no podía saber de antemano el resultado de su solicitud, y una vez `PENDING` no tenía ninguna acción salvo esperar a que un admin la resolviera. En paralelo, "Mi Semana" mostraba la tira de días pero sin un punto de entrada rápido para HOY/MAÑANA ni una acción de reserva siempre accesible.

Restricciones del proyecto: prosa de negocio en español, identificadores en inglés; backend Spring Boot 3.3 / Java 21, frontend React 18 + Vite; SQL Server 2022 con Flyway como dueño del esquema; Quality Gate obligatorio (cobertura ≥80%/≥75%, 0 violations Sonar nuevas).

## Goals / Non-Goals

**Goals:**
- Exponer `approvalMode` a cualquier autenticado sin exponer la trazabilidad reservada a `ADMIN`.
- Comunicar en el modal de solicitud, antes de enviar, si el resultado será inmediato o pendiente.
- Dar al empleado una acción para reavisar a los admins de una `PENDING` propia estancada, con un límite de frecuencia razonable.
- Rediseñar "Mi Semana" con un resumen HOY/MAÑANA accionable en 1 toque y una acción principal de reserva siempre visible.

**Non-Goals:**
- Lista de espera (`waitlist-requests`, change separado y ya existente).
- Cambiar la semántica de `approvalMode` (sigue siendo un único parámetro global) o quién puede modificarlo (sigue siendo `ADMIN`-only vía `PUT /admin/settings`).
- Cambiar el flujo de aprobación/rechazo del admin.

## Decisions

**D1. Endpoint de lectura nuevo y separado, no relajar el rol del existente.**
`GET /settings/approval-mode` es un adaptador propio (`ApprovalModeController`), no una relajación de `@PreAuthorize` sobre `GET /admin/settings`. Devuelve un DTO propio (`ApprovalModeResponse`) que solo lleva `approvalMode`, nunca `updatedById`/`updatedAt`. Así el endpoint `ADMIN`-only conserva su payload completo (trazabilidad) y su restricción de rol, mientras el nuevo endpoint es mínimo por diseño.

**D2. El reenvío reutiliza el evento de creación, no crea una plantilla de email nueva.**
`resend` reutiliza `RequestCreatedEvent` (mismo tipo `REQUEST_CREATED`, misma plantilla `request-created.html`, mismos destinatarios: admins activos). Alternativa descartada: un evento/plantilla `RequestResendEvent` dedicado — el aviso al admin es idéntico en contenido al de la creación (misma solicitud, mismos datos), así que duplicar la plantilla no aporta valor y añade superficie de mantenimiento.

**D3. Cooldown de 24h calculado sobre la referencia más reciente entre creación y último reenvío.**
`Request.canBeResent(now, cooldown)` compara `now` contra `lastRemindedAt` si existe, o contra `createdAt` en su defecto. Cada reenvío exitoso actualiza `lastRemindedAt` (`markReminded`), de modo que el siguiente reenvío exige de nuevo el período completo. Evita reenvíos en ráfaga sin necesitar un contador ni una tabla aparte.

**D4. El reenvío solo aplica a una `PENDING` propia; nunca cambia estado.**
`resend` no transiciona la `Request`: solo actualiza `lastRemindedAt` y dispara la notificación. Verificación de pertenencia (BOLA) en el servicio, no solo en el rol del controlador (`@PreAuthorize("hasRole('EMPLOYEE')")` es necesario pero no suficiente).

**D5. El héroe HOY/MAÑANA es una vista derivada de `GET /calendar/my-week`, no un endpoint nuevo.**
`MyWeekPage` consulta la semana actual y la siguiente (para cubrir el borde domingo→lunes, en que "mañana" cae en la semana siguiente) y deriva `todayDay`/`tomorrowDay` por fecha; react-query deduplica cuando coincide con la semana navegable. Alternativa descartada: un endpoint `GET /calendar/today-tomorrow` — habría duplicado la lógica de estado por recurso que `MyWeekDay` ya resuelve.

**D6. Reserva en 1 toque = preselección, no un flujo distinto.**
El botón "Reservar" de una tarjeta del héroe abre el mismo `CreateRequestModal` con `presetDate`/`presetResource` ya fijados a partir del recurso libre pulsado, en vez de crear un camino de envío alternativo. Mantiene una única superficie de validación (ventana, disponibilidad, auto-asignación) para toda creación de solicitud.

## Risks / Trade-offs

- **Cooldown de 24h fijo sin ajuste por negocio** → Mitigación: el valor vive en una única constante (`RequestService.RESEND_COOLDOWN`); cambiarlo no requiere migración.
- **El héroe consulta dos semanas (dobla las llamadas a `GET /calendar/my-week`)** → Mitigación: react-query cachea por `weekStart`; cuando la semana navegable coincide con la actual, no hay petición duplicada.
- **Confundir "reenviar aviso" con "reenviar solicitud" (no cambia nada del contenido de la solicitud)** → Mitigación: el texto de la UI ("Reenviar solicitud"/`requests.pendingBanner.resend`) y el aviso 409 dejan claro que solo re-notifica; el backend no permite reenviar una solicitud ya resuelta (`REQUEST_NOT_PENDING`).

## Migration Plan

1. Migración `V26__request_last_reminded_at.sql`: `ALTER TABLE dbo.requests ADD last_reminded_at DATETIME2(3) NULL` — retrocompatible, sin backfill (`NULL` se interpreta como "nunca reenviada", usando `createdAt` como referencia).
2. Sin cambios de rutas ni de contratos existentes: `GET /admin/settings` y `PUT /admin/settings` no cambian.
3. Rollback: revertir el merge; la columna `last_reminded_at` puede quedar sin usar sin romper nada (nullable).
