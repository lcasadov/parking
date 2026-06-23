# Design: init-notifications

## Context
Las notificaciones son un efecto lateral de eventos de dominio, no una API. El
requisito clave es la coherencia transaccional: el email solo debe salir cuando
la operación que lo motiva está confirmada en base de datos, y un fallo de SMTP
nunca puede arrastrar la operación funcional a un rollback. Arquitectura
hexagonal: el dominio publica eventos; un adaptador de salida (`EmailSenderPort`)
y un listener `AFTER_COMMIT` los traducen en envíos.

## Goals
- Enviar el email correcto, a los destinatarios correctos, solo tras commit.
- Garantizar que el fallo SMTP no revierte la operación de negocio.
- Reintentar de forma fiable los envíos fallidos mediante un job programado.
- Excluir explícitamente los eventos que por diseño no notifican.

## Decisions
- **`AFTER_COMMIT`**: el envío se engancha a la fase post-commit de la transacción
  (`TransactionSynchronization` / `@TransactionalEventListener(phase = AFTER_COMMIT)`).
  *Por qué:* evita enviar emails de operaciones que luego se revierten y desacopla
  el envío del camino crítico de la transacción.
- **Resolución de destinatarios en el listener**: para "nueva solicitud" se consultan
  los `Employee` con `role = ADMIN` y `active = true` en el momento del envío.
  *Por qué:* refleja el estado actual de admins activos, no una lista cacheada.
- **Resiliencia SMTP**: try/catch alrededor del envío; en fallo se registra en log y
  se persiste el email como pendiente de reintento (no se relanza excepción al flujo
  origen). *Por qué:* la operación funcional ya está confirmada; el email es best-effort.
- **Reintento por job `@Scheduled`**: relee los pendientes y reintenta; marca enviados
  los que tienen éxito; idempotente. *Por qué:* recuperación ante caídas temporales del
  relay SMTP sin acoplar el reintento a la petición original.
- **Plantillas Thymeleaf** server-side (`request-created.html`, `request-approved.html`,
  `request-rejected.html`, `assignment-revoked.html`, `password-reset.html`).
  *Por qué:* separación contenido/lógica y soporte i18n del cuerpo del email.
- **Reloj inyectable** (`ClockPort`) para testear marcas de tiempo y ventanas de reintento.

## Risks
- **Doble envío** si el job reintenta un email ya enviado → mitigado con marca de estado
  enviado/pendiente e idempotencia del reintento.
- **Destinatario sin email válido** → se registra el fallo y se descarta tras agotar la
  política de reintentos (evita bucle infinito). _[verificar con docs/data-model.md:
  máximo de reintentos no definido]_.
- **Fuga de datos personales en logs** → preferir `employee_id` sobre nombre/email
  (`docs/security-design.md`, minimización RGPD).
- **Sin admins activos** → no hay destinatarios; el flujo no debe fallar.

## Migration Plan
- Flyway: tabla de cola/almacén de emails fallidos pendientes de reintento (estado,
  plantilla, destinatario, payload, intentos, timestamps) _[verificar con
  docs/data-model.md: definir esquema; no existe aún]_.
- Sin migración de datos (capability nueva).
- Plantillas Thymeleaf añadidas como recursos de la aplicación (no requieren Flyway).
