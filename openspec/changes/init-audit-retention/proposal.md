# Proposal: init-audit-retention

## Why
Inicializar la capability **audit-retention** en OpenSpec: dejar documentado,
como contrato verificable antes de implementarlo, cómo se registra la actividad
del sistema (`audit_log` vía AOP, `login_log` vía el filtro de auth), cómo se
consulta (solo `ADMIN`, paginada y filtrable) y cómo se purgan los datos
históricos a 2 años. Es la base de trazabilidad (rendición de cuentas RGPD) y de
retención de la que se nutre el resto de capabilities.

## What Changes
- Se añade la capability `audit-retention` con sus Requirements y escenarios BDD.
- Endpoints de consulta: `GET /audit` (operationId `listAuditLog`) y
  `GET /login-logs` (operationId `listLoginLog`), ambos solo `ADMIN`
  (ver `docs/openapi.yaml`).
- Registro automático en `audit_log` vía `@Aspect` Spring AOP sobre casos de uso
  anotados `@Auditable`; registro en `login_log` vía el filtro de autenticación.
- Job `@Scheduled` diario de purga por lotes (`DELETE TOP (1000)`) sobre datos
  históricos, con ventana configurable `parking.retention.years` (default 2).

## Capabilities
- `audit-retention` (ADDED)

## Impact
- **Entidades**: `AuditLog` (`audit_log`), `LoginLog` (`login_log`); afecta por
  purga a `Request` cerradas, `Release`, `VisitorReservation` (ver `docs/data-model.md` §8, §9).
- **Seguridad**: consulta restringida a `ADMIN` (`@PreAuthorize("hasRole('ADMIN')")`);
  los DTO de salida no exponen datos sensibles (ver `docs/security-design.md` §8).
- **UI mínima**: paneles de consulta de auditoría y de login solo para `ADMIN`;
  la purga no tiene UI.

## Out of scope
- La emisión de eventos de notificación por email (la cubre `notifications`).
- La exportación CSV/XLSX de la auditoría (`exportAuditLog`, la cubre `exports`).
- El alta de columnas nuevas en `audit_log` (los atributos enriquecidos van en `details` JSON).
- Alertas de seguridad sobre `login_log` (>50 fallos/5 min) — son del plan de respuesta a incidentes, fuera de esta capability.
