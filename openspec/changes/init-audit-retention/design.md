# Design: init-audit-retention

## Context
La auditoría es transversal: debe cubrir todas las acciones sensibles sin
ensuciar la lógica de dominio (arquitectura hexagonal). Por eso `audit_log` se
puebla con un `@Aspect` Spring AOP en la capa de aplicación, y `login_log` en el
filtro de autenticación (donde ya pasa todo intento de login). La retención
responde a una obligación RGPD: conservar el histórico el tiempo necesario y
purgarlo después, sin comprometer la integridad referencial de las entidades
vivas.

## Goals
- Trazabilidad uniforme y completa sin acoplar el dominio a la auditoría.
- Consulta segura (solo `ADMIN`), paginada y filtrable, sin fugar datos sensibles.
- Purga periódica robusta que no bloquee la base de datos ni toque datos vivos.
- Ventana de retención ajustable por entorno sin recompilar.

## Decisions
- **Auditoría vía AOP**: `@Aspect` sobre métodos anotados `@Auditable`. *Por qué:* centraliza la auditoría fuera de la lógica de negocio y garantiza cobertura uniforme; añadir auditoría a un caso de uso es solo una anotación.
- **`details` como JSON (`NVARCHAR(MAX)`)**: los atributos enriquecidos (`actor_login`, `ip`, `user_agent`, snapshot antes/después) se serializan dentro de `details`, no como columnas propias. *Por qué:* mantiene el esquema estable y flexible sin proliferar columnas.
- **`login_log` separado de `audit_log`**: el ruido de autenticación no contamina la auditoría funcional; cada uno tiene su tabla, su índice `occurred_at` y su criterio de purga.
- **Registro no transaccional con el negocio (best-effort)**: un fallo al insertar en `audit_log` se loguea pero **no** revierte la operación de negocio. *Por qué:* la auditoría no debe convertirse en punto único de fallo de la funcionalidad.
- **Purga por lotes**: job `@Scheduled` diario que ejecuta `DELETE TOP (1000)` en bucle hasta `@@ROWCOUNT < 1000`. *Por qué:* evita bloqueos largos de tabla y crecimiento del log de transacciones; cada lote es una transacción corta.
- **Cutoff configurable**: el job lee `parking.retention.years` (default 2) en lugar de un `-2` hard-codeado. *Por qué:* permite distintas ventanas por entorno (p. ej. preproducción vs. producción).
- **Solo datos históricos**: la purga toca `audit_log`, `login_log`, `Request` cerradas (`status <> 'PENDING'`), `Release` (`release_date`) y `VisitorReservation` (`reservation_date`); nunca entidades vivas. *Por qué:* preserva la integridad referencial y los datos operativos.
- **Reloj inyectable** (`ClockPort`) para testear el cálculo del `cutoff` de forma determinista.

## Risks
- **Pérdida de auditoría por excepción silenciosa** → mitigado loguendo todo fallo de inserción y monitorizando; nunca se traga sin rastro.
- **Purga demasiado agresiva borrando datos vivos** → mitigado con criterios de fecha explícitos por tabla y tests de no-regresión que verifican que las entidades vivas sobreviven.
- **Bloqueo/timeout en tablas grandes** → mitigado con borrado por lotes de 1000 e índices `IX_audit_log_occurred_at` / `IX_login_log_occurred_at`.
- **Enumeración de usuarios vía `login_attempted`** → mitigado restringiendo la consulta a `ADMIN`.

## Migration Plan
- Flyway: tablas `audit_log` y `login_log` con sus índices `IX_audit_log_occurred_at` / `IX_login_log_occurred_at` (ver `docs/data-model.md` §8, §9 y la sección de índices).
- Clave de configuración `parking.retention.years` (default 2) en `application.yml` por perfil.
- Sin migración de datos (capability nueva; las tablas se rellenan en runtime).
