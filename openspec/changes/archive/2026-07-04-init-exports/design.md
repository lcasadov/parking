# Design: init-exports

## Context
La exportación es transversal: cada capability con datos consultables ofrece un
botón "Exportar". El comportamiento (formato, RBAC, comprobación de objeto) debe
ser uniforme para no repetir lógica ni introducir fugas. `exportMyData` tiene
además relevancia legal: materializa el derecho de acceso RGPD
(`docs/security-design.md` §12), por lo que debe limitarse estrictamente al
sujeto de la sesión. Arquitectura hexagonal: el dominio decide *qué* datos y
*para quién*; un adaptador de serialización produce el `csv`/`xlsx`.

## Goals
- Un único mecanismo de exportación reutilizable por todas las capabilities.
- RBAC por export coherente con `docs/security-design.md` §3 (tabla RBAC).
- Cumplir el derecho de acceso RGPD sin exponer datos ajenos ni campos sensibles.
- Generación eficiente (streaming) y segura (sin inyección de fórmulas CSV).

## Decisions
- **Formatos**: solo `csv` y `xlsx`; `xlsx` por defecto. *Por qué:* cubre tanto
  el análisis en hoja de cálculo como la interoperabilidad simple; PDF queda
  fuera por no ser un formato de datos reutilizable.
- **RBAC por export**: cada operationId declara su rol. Las exportaciones
  "propias" (`exportMyData`, `exportMyRequests`) las puede pedir cualquier
  usuario autenticado, pero filtran por `employee_id == session.employee_id`
  (comprobación de objeto). Las administrativas exigen `ADMIN`.
- **Exclusión de campos sensibles**: los serializadores nunca emiten
  `password_hash`, `failed_login_attempts`, `locked_until`; en exportaciones de
  `EMPLOYEE` se omiten además los campos "Solo admins" (`docs/security-design.md` §13).
- **Streaming**: se serializa fila a fila (writer sobre el `OutputStream` de la
  respuesta) para no cargar todo el dataset en memoria.
- **Sanitización CSV**: valores que empiezan por `=`, `+`, `-`, `@` se prefijan
  para neutralizar inyección de fórmulas en la hoja del destinatario.
- **Límite de tasa**: 5 exportaciones/min por usuario (`docs/security-design.md` §"Rate limiting").
- **Reloj inyectable** (`ClockPort`) para nombrar el fichero con timestamp y para
  la ventana del límite de tasa, facilitando los tests.

## Risks
- **Exfiltración masiva** de datos personales → mitigada con RBAC por export,
  límite de tasa y registro del evento en `audit_log`.
- **Fuga de datos ajenos** en exportaciones "propias" → mitigada con la
  comprobación de objeto `employee_id == session.employee_id` (fail closed).
- **Inyección de fórmulas CSV** en el equipo del destinatario → mitigada con la
  sanitización de prefijos.
- **OOM con datasets grandes** → mitigado con serialización en streaming.

## Migration Plan
- Sin migración de esquema: los exports leen tablas ya existentes (`employees`,
  `requests`, `audit_log`) definidas en `docs/data-model.md`.
- Sin migración de datos (capability de solo lectura).
- Flyway no requiere cambios para esta capability.
