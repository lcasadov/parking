# Capability: audit-retention

## Resumen
Trazabilidad y retención de datos: registro automático de acciones funcionales
(`audit_log`, vía `@Aspect` Spring AOP) y de intentos de autenticación
(`login_log`, vía el filtro de auth), su consulta paginada y filtrable solo por
`ADMIN`, y la purga automática diaria de datos históricos a 2 años por lotes.
Es una capability transversal: aporta poca UI propia y se nutre del resto.

## Fase
🟢🔵 ambas (la auditoría y la retención aplican en Fase 1 y Fase 2 por igual).

## Reglas de negocio implicadas
(README §"Auditoría" y §"Retención y purga"; `data-model.md` §9; NO hay códigos RN-xx)
- `audit_log` se rellena automáticamente vía `@Aspect` Spring AOP sobre los casos de uso anotados (`@Auditable`), fuera de la lógica de negocio.
- `login_log` se rellena en el filtro de autenticación; cubre Fase 1, Fase 2 y fallback; separado de `audit_log` para no contaminar la auditoría funcional.
- Consulta de auditoría y de logs de login restringida a `ADMIN`.
- Las entidades vivas (`Employee`, `ParkingSpace`, `Desk`, `Visitor`, `FixedAssignment` activa) **no se purgan**.
- Datos históricos (`audit_log`, `login_log`, `Request` cerradas, `Release`, `VisitorReservation`) se purgan automáticamente a 2 años desde su `occurred_at`/fecha.
- Purga diaria por job `@Scheduled`, borrado por lotes `DELETE TOP (1000)` hasta drenar.
- Ventana de retención configurable por entorno (`parking.retention.years`, default 2).

## Entidades implicadas
- AuditLog (`audit_log`: `actor_employee_id` nullable, `action`, `entity_type`, `entity_id` nullable, `details` JSON, `occurred_at`)
- LoginLog (`login_log`: `login_attempted`, `employee_id` nullable, `result`, `phase`, `ip_address`, `user_agent`, `occurred_at`)
- Datos históricos purgables: Request (cerradas), Release, VisitorReservation

## Endpoints
- GET /api/v1/audit (operationId: `listAuditLog`) — ADMIN
- GET /api/v1/login-logs (operationId: `listLoginLog`) — ADMIN

> El registro en `audit_log` y `login_log` no tiene endpoint de escritura: se produce automáticamente (AOP / filtro de auth). La purga es un job `@Scheduled` interno, sin endpoint.

## Permisos
| Rol | Permisos |
|---|---|
| ADMIN | Consultar `audit_log` y `login_log` con filtros y paginación |
| EMPLOYEE | Ninguno sobre auditoría (sus acciones se registran, pero no puede consultarla) |

## ADDED Requirements
### Requirement: Consulta de auditoría funcional con filtros
**El sistema DEBE (MUST) permitir a un `ADMIN` consultar `audit_log` de forma paginada, filtrando por `actorEmployeeId`, `action` y ventana temporal `from`/`to`.**

#### Scenario: Consulta paginada con filtros válidos
- **GIVEN** un usuario autenticado con rol `ADMIN`
- **AND** existen entradas en `audit_log` con distintos `actor_employee_id` y `action`
- **WHEN** envía `GET /audit?actorEmployeeId=7&action=approveRequest&from=2026-01-01T00:00:00Z&to=2026-06-01T00:00:00Z&page=0&size=20`
- **THEN** el sistema responde 200 con una página de entradas que cumplen todos los filtros
- **AND** cada entrada expone `action`, `entity_type`, `entity_id`, `occurred_at` y el actor (sin datos sensibles)

#### Scenario: Empleado sin privilegios intenta consultar la auditoría
- **GIVEN** un usuario autenticado con rol `EMPLOYEE`
- **WHEN** envía `GET /audit`
- **THEN** el sistema responde 403 con `{ error, message, fields, timestamp }`
- **AND** no devuelve ninguna entrada de auditoría

#### Scenario: Ventana temporal inválida (`from` posterior a `to`)
- **GIVEN** un usuario autenticado con rol `ADMIN`
- **WHEN** envía `GET /audit?from=2026-06-01T00:00:00Z&to=2026-01-01T00:00:00Z`
- **THEN** el sistema responde 400 con `error` de validación y `fields` indicando la ventana inválida

### Requirement: Consulta de logs de login con filtros
**El sistema DEBE (MUST) permitir a un `ADMIN` consultar `login_log` de forma paginada, filtrando por `result` y ventana temporal `from`/`to`.**

#### Scenario: Consulta de intentos fallidos en una ventana
- **GIVEN** un usuario autenticado con rol `ADMIN`
- **AND** existen entradas en `login_log` con `result` `OK` e `INVALID_CREDENTIALS`
- **WHEN** envía `GET /login-logs?result=INVALID_CREDENTIALS&from=2026-06-01T00:00:00Z&to=2026-06-23T00:00:00Z`
- **THEN** el sistema responde 200 con solo las entradas cuyo `result = INVALID_CREDENTIALS` en la ventana
- **AND** cada entrada expone `login_attempted`, `result`, `phase`, `ip_address`, `occurred_at`

#### Scenario: Valor de `result` fuera del enum permitido
- **GIVEN** un usuario autenticado con rol `ADMIN`
- **WHEN** envía `GET /login-logs?result=UNKNOWN`
- **THEN** el sistema responde 400 con `error` de validación y `fields` indicando `result`
- **AND** no devuelve ninguna página

#### Scenario: Empleado sin privilegios intenta consultar los logs de login
- **GIVEN** un usuario autenticado con rol `EMPLOYEE`
- **WHEN** envía `GET /login-logs`
- **THEN** el sistema responde 403 con `{ error, message, fields, timestamp }`

### Requirement: Registro automático de acciones funcionales (AOP)
**El sistema DEBE (MUST) registrar en `audit_log`, de forma transparente vía `@Aspect` Spring AOP, cada caso de uso anotado como auditable, sin que el fallo del registro revierta la operación de negocio.**

#### Scenario: Una acción auditable genera una entrada de auditoría
- **GIVEN** un caso de uso anotado como auditable (p. ej. `approveRequest`) ejecutado por un `ADMIN`
- **WHEN** el caso de uso completa con éxito
- **THEN** el aspecto inserta una entrada en `audit_log` con `actor_employee_id`, `action`, `entity_type`, `entity_id` y `occurred_at`
- **AND** los atributos enriquecidos (`actor_login`, `ip`, `user_agent`, snapshot antes/después) se serializan como JSON en `details`

#### Scenario: Acción del sistema sin actor identificado
- **GIVEN** una acción originada por el propio sistema (p. ej. la purga programada)
- **WHEN** se registra en `audit_log`
- **THEN** la entrada se crea con `actor_employee_id = NULL`
- **AND** el `action` y `entity_type` identifican la operación del sistema

### Requirement: Purga automática de datos históricos por lotes
**El sistema DEBE (MUST) ejecutar un job `@Scheduled` diario que borre los datos históricos con antigüedad mayor que `parking.retention.years` (default 2) mediante borrado por lotes, sin tocar las entidades vivas.**

#### Scenario: Purga de entradas de auditoría antiguas por lotes
- **GIVEN** `parking.retention.years = 2`
- **AND** existen 2500 filas en `audit_log` con `occurred_at` anterior al `cutoff` (ahora − 2 años)
- **WHEN** se ejecuta el job diario de purga
- **THEN** el sistema borra las 2500 filas en lotes de `TOP (1000)` hasta que `@@ROWCOUNT < 1000`
- **AND** no borra ninguna fila con `occurred_at >= cutoff`

#### Scenario: La purga nunca elimina entidades vivas
- **GIVEN** existen `Employee`, `ParkingSpace`, `Visitor` y `FixedAssignment` activas con fecha de creación anterior al `cutoff`
- **WHEN** se ejecuta el job diario de purga
- **THEN** el sistema no borra ninguna de esas entidades vivas
- **AND** solo afecta a `audit_log`, `login_log`, `Request` cerradas, `Release` y `VisitorReservation` por su criterio de fecha

#### Scenario: Ventana de retención reconfigurada por entorno
- **GIVEN** `parking.retention.years = 5` en la configuración del entorno
- **WHEN** se ejecuta el job diario de purga
- **THEN** el `cutoff` se calcula como ahora − 5 años (no el default de 2)
- **AND** solo se borran las filas anteriores a ese `cutoff`

## Casos límite (edge cases)
- Filtros vacíos en `GET /audit`/`GET /login-logs`: devuelve la página completa ordenada por `occurred_at` descendente, respetando la paginación por defecto.
- `actor_employee_id`/`employee_id` nulos (acciones del sistema o login de usuario inexistente): la entrada se devuelve igualmente con el actor vacío.
- Sin filas que purgar: el job termina inmediatamente (`@@ROWCOUNT < 1000` en la primera iteración) sin error.
- El borrado por lotes evita bloqueos largos de tabla; cada lote es una transacción corta.
- `login_attempted` registra el `login` tecleado aunque el empleado no exista (enumeración controlada: solo visible para `ADMIN`).
- Para tablas basadas en fecha (`releases.release_date`, `visitor_reservations.reservation_date`) el `cutoff` se compara contra la columna de fecha, no contra `occurred_at`.

## Dependencias con otras capabilities
- Depende de `auth-local`/`auth-sso` para la sesión y el rol `ADMIN` que autoriza la consulta.
- `login_log` lo alimenta el filtro de autenticación de `auth-local`/`auth-sso`.
- `audit_log` lo alimentan, vía AOP, casi todas las capabilities con acciones sensibles (`employees`, `parking-spaces`, `fixed-assignments`, `releases`, `requests`, `visitors`).
- `exports` reutiliza la consulta de auditoría (`exportAuditLog`).
- La purga afecta a datos históricos de `requests`, `releases` y `visitors`.
