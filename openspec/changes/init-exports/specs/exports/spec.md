# Capability: exports

## Resumen
Exportaciones de datos en formato **CSV/XLSX** (nunca PDF). Cada export aplica
su propio RBAC: las exportaciones propias las puede pedir cualquier usuario
autenticado (`EMPLOYEE`/`ADMIN`); las exportaciones de histórico, empleados y
auditoría son exclusivas de `ADMIN`. `exportMyData` materializa el **derecho de
acceso RGPD** del interesado sobre sus propios datos personales. Es una
capability transversal (botones repartidos por varias pantallas).

## Fase
🟢🔵 ambas fases (los datos a exportar crecen con cada capability, pero el
mecanismo de exportación es estable desde Fase 1).

## Reglas de negocio implicadas
(README §"Exportaciones" y §"Reglas de negocio"; `docs/security-design.md` §3, §12; NO hay códigos RN-xx)
- Formato de salida limitado a `csv` o `xlsx` (por defecto `xlsx`); nunca PDF.
- RBAC por export: propias = `EMPLOYEE`/`ADMIN`; histórico de solicitudes, empleados y auditoría = solo `ADMIN`.
- `exportMyData` es el derecho de acceso RGPD: el usuario solo exporta sus propios datos personales (comprobación de objeto `employee_id == session.employee_id`).
- Un `EMPLOYEE` solo exporta sus propias solicitudes (`exportMyRequests`), nunca el histórico completo.
- Los ficheros exportados nunca incluyen campos sensibles de credenciales (`password_hash`, `failed_login_attempts`, `locked_until`).
- Límite de tasa de 5 exportaciones por minuto y usuario en los endpoints de exportación.

## Entidades implicadas
- Employee (origen de `exportEmployees` y `exportMyData`)
- Request (origen de `exportRequests` y `exportMyRequests`)
- AuditLog (origen de `exportAuditLog`)
- FixedAssignment, Release, ParkingSpace (datos personales relacionados incluidos en `exportMyData`)

## Endpoints
- GET /api/v1/employees/export (operationId: exportEmployees)
- GET /api/v1/employees/me/export (operationId: exportMyData)
- GET /api/v1/requests/export (operationId: exportRequests)
- GET /api/v1/requests/mine/export (operationId: exportMyRequests)
- GET /api/v1/audit/export (operationId: exportAuditLog)

> Parámetro común `format` (query, enum `csv`/`xlsx`, por defecto `xlsx`).
> Respuesta 200 = fichero binario (`text/csv` o
> `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`).

## Permisos
| Rol | Permisos |
|---|---|
| ADMIN | Todas las exportaciones: `exportEmployees`, `exportRequests`, `exportAuditLog`, `exportMyData`, `exportMyRequests` |
| EMPLOYEE | Solo exportaciones propias: `exportMyData` (datos personales propios), `exportMyRequests` (solicitudes propias) |

## ADDED Requirements
### Requirement: Exportación administrativa de histórico
**El sistema DEBE (MUST) permitir a un `ADMIN` exportar empleados, histórico de solicitudes y auditoría en CSV/XLSX, y denegar el acceso a `EMPLOYEE`.**

#### Scenario: Admin exporta empleados en XLSX
- **GIVEN** un usuario autenticado con rol `ADMIN`
- **WHEN** envía `GET /employees/export?format=xlsx`
- **THEN** el sistema responde 200 con un fichero XLSX (`Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`)
- **AND** el fichero no contiene `password_hash`, `failed_login_attempts` ni `locked_until`

#### Scenario: Admin exporta auditoría en CSV
- **GIVEN** un usuario autenticado con rol `ADMIN`
- **WHEN** envía `GET /audit/export?format=csv`
- **THEN** el sistema responde 200 con un fichero CSV (`Content-Type: text/csv`)

#### Scenario: Empleado intenta exportar el histórico de solicitudes
- **GIVEN** un usuario autenticado con rol `EMPLOYEE`
- **WHEN** envía `GET /requests/export`
- **THEN** el sistema responde 403 con `{ error, message, fields, timestamp }`
- **AND** no genera ningún fichero

### Requirement: Exportación de datos propios (derecho de acceso RGPD)
**El sistema DEBE (MUST) permitir a cualquier usuario autenticado exportar sus propios datos personales y sus propias solicitudes, limitándose siempre al sujeto de la sesión.**

#### Scenario: Empleado exporta sus propios datos personales
- **GIVEN** un usuario autenticado con rol `EMPLOYEE` y `employee_id = E1`
- **WHEN** envía `GET /employees/me/export?format=xlsx`
- **THEN** el sistema responde 200 con un fichero que contiene únicamente los datos personales del empleado `E1`
- **AND** no incluye datos de otros empleados

#### Scenario: Empleado exporta sus propias solicitudes
- **GIVEN** un usuario autenticado con rol `EMPLOYEE` y `employee_id = E1`
- **WHEN** envía `GET /requests/mine/export?format=csv`
- **THEN** el sistema responde 200 con un fichero CSV que contiene solo las solicitudes cuyo `employee_id == E1`

#### Scenario: Usuario no autenticado intenta exportar datos propios
- **GIVEN** una petición sin sesión válida (sin cookie `parking_SESSION`)
- **WHEN** envía `GET /employees/me/export`
- **THEN** el sistema responde 401 con `{ error, message, fields, timestamp }`

### Requirement: Validación del formato de exportación
**El sistema DEBE (MUST) aceptar solo los formatos `csv` y `xlsx`, usar `xlsx` por defecto y rechazar cualquier otro valor.**

#### Scenario: Formato no soportado
- **GIVEN** un usuario autenticado con rol `ADMIN`
- **WHEN** envía `GET /employees/export?format=pdf`
- **THEN** el sistema responde 400 con `error` de validación y `fields` indicando `format` como valor no permitido
- **AND** no genera ningún fichero

#### Scenario: Formato omitido usa XLSX por defecto
- **GIVEN** un usuario autenticado con rol `ADMIN`
- **WHEN** envía `GET /employees/export` sin parámetro `format`
- **THEN** el sistema responde 200 con un fichero XLSX

### Requirement: Límite de tasa de exportaciones
**El sistema DEBE (MUST) limitar las exportaciones a 5 por minuto y usuario para evitar exfiltración masiva.**

#### Scenario: Sexta exportación en un minuto
- **GIVEN** un usuario autenticado que ya ha realizado 5 exportaciones en el último minuto
- **WHEN** envía una sexta petición de exportación dentro de la misma ventana
- **THEN** el sistema responde 429 con `{ error, message, fields, timestamp }` _[verificar con docs/openapi.yaml: 429 no declarado en las respuestas de los endpoints de export]_
- **AND** no genera el fichero

## Casos límite (edge cases)
- Exportación sin filas (p. ej. un empleado sin solicitudes): responde 200 con un fichero válido que contiene solo la cabecera de columnas.
- `exportMyData` por un `ADMIN`: exporta sus propios datos personales (los suyos como sujeto), no los de toda la organización.
- Campos sensibles ("Solo admins" en `docs/security-design.md` §13, como `mobile_phone`/`license_plate`) no se incluyen en exportaciones pedidas por `EMPLOYEE`.
- Fichero grande: la generación se realiza en streaming para no agotar memoria; el `Content-Disposition` sugiere un nombre con timestamp.
- Inyección de fórmulas CSV: los valores que empiezan por `=`, `+`, `-` o `@` se sanitizan (prefijo) para evitar ejecución en la hoja de cálculo del destinatario.

## Dependencias con otras capabilities
- Depende de `auth-local`/`auth-sso` para la autenticación y la identidad de la sesión.
- Consume datos de `employees`, `requests` y `audit-retention` (origen de cada export).
- `exportMyData` materializa el derecho de acceso RGPD descrito en `docs/security-design.md` §12.
- La acción de exportar puede registrarse como evento sensible en `audit-retention`.
