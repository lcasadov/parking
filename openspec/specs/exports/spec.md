# exports Specification

## Purpose
TBD - created by archiving change init-exports. Update Purpose after archive.
## Requirements
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
- **THEN** el sistema responde 429 con `{ error, message, fields, timestamp }` (declarado en `docs/openapi.yaml` como `TooManyRequests` en los cinco endpoints de export)
- **AND** no genera el fichero

