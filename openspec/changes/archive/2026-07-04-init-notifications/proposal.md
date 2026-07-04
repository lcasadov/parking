# Proposal: init-notifications

## Why
Inicializar la capability **notifications** en OpenSpec: dejar documentado, como
contrato verificable, el comportamiento de las notificaciones por email
transversales del sistema. Es una capability sin endpoints propios que reacciona
a eventos de dominio (`requests`, `fixed-assignments`, `employees`), y cuya
corrección depende de garantías sutiles (envío `AFTER_COMMIT`, fallo SMTP que
no revierte la operación, exclusiones) que conviene fijar antes de implementarlas.

## What Changes
- Se añade la capability `notifications` con sus Requirements y escenarios BDD.
- Envío de emails `AFTER_COMMIT` con plantillas Thymeleaf para los eventos:
  nueva solicitud → admins activos; solicitud aprobada/rechazada → empleado;
  asignación fija revocada → empleado; 🔵 reset de contraseña → empleado.
- Política de resiliencia: fallo SMTP → log + reintento por job programado,
  sin revertir nunca la operación funcional.
- Exclusiones explícitas: liberación voluntaria y cancelación de la propia
  solicitud NO generan email; las reservas de visitante tampoco.

## Capabilities
- `notifications` (ADDED)

## Impact
- **Entidades**: `Employee` (resolución de destinatarios y admins activos),
  `Request` (`approval_note`, `rejection_reason`), `FixedAssignment` (revocación).
  Almacén de reintento de emails fallidos _[verificar con docs/data-model.md: no
  existe tabla explícita; se asume almacén de reintento]_.
- **Seguridad/RGPD**: minimización en logs (preferir `employee_id` sobre nombre/email,
  ver `docs/security-design.md`).
- **UI**: ninguna pantalla propia; el efecto observable son los toasts de las
  pantallas que disparan los eventos.

## Out of scope
- Endpoints de gestión o consulta de notificaciones (no existen).
- Notificaciones por canales distintos al email (SMS, push, in-app).
- Reserva/edición de visitante (no notifica; ver `visitors`).
- Liberación voluntaria y cancelación de la propia solicitud (excluidas por diseño).
