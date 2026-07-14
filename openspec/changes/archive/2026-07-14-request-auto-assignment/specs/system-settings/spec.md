## ADDED Requirements

### Requirement: Parámetro global de modo de aprobación
**El sistema DEBE (MUST) mantener un único parámetro global `approvalMode` con valores `MANUAL` o `AUTOMATIC`, persistido en una tabla de fila única (`system_settings`), con valor por defecto `MANUAL`. El parámetro DEBE (MUST) tener alcance global (un único ajuste para toda la instalación); no existen ajustes por departamento ni por empleado.**

#### Scenario: Valor por defecto tras la instalación
- **GIVEN** una instalación recién migrada sin que el ADMIN haya cambiado el parámetro
- **WHEN** el sistema resuelve el modo de aprobación
- **THEN** el valor efectivo es `MANUAL`

#### Scenario: Existe una única fila de configuración
- **GIVEN** la tabla `system_settings`
- **WHEN** se intenta insertar una segunda fila de configuración
- **THEN** la restricción de fila única (`id = 1`) impide crear un segundo ajuste global

### Requirement: Lectura del parámetro global por el ADMIN
**El sistema DEBE (MUST) permitir al `ADMIN` consultar el valor actual de `approvalMode` y DEBE denegar el acceso a usuarios `EMPLOYEE`.**

#### Scenario: El admin consulta el modo actual
- **GIVEN** un `Employee` autenticado con rol `ADMIN`
- **WHEN** envía `GET /admin/settings`
- **THEN** el sistema responde 200 con `{ approvalMode }` reflejando el valor persistido

#### Scenario: Un empleado intenta leer la configuración global
- **GIVEN** un `Employee` autenticado con rol `EMPLOYEE`
- **WHEN** envía `GET /admin/settings`
- **THEN** el sistema responde 403 sin revelar el valor

### Requirement: Actualización del parámetro global por el ADMIN
**El sistema DEBE (MUST) permitir al `ADMIN` cambiar `approvalMode` a `MANUAL` o `AUTOMATIC`, validando el valor recibido, registrando quién y cuándo lo cambió (`updated_by_id`, `updated_at`), y DEBE denegar la operación a usuarios `EMPLOYEE`. El cambio DEBE afectar solo a las solicitudes nuevas; no reprocesa solicitudes existentes.**

#### Scenario: El admin activa el modo automático
- **GIVEN** un `Employee` autenticado con rol `ADMIN` y `approvalMode = MANUAL`
- **WHEN** envía `PUT /admin/settings` con `{ approvalMode: "AUTOMATIC" }`
- **THEN** el sistema responde 200 con `approvalMode = AUTOMATIC`
- **AND** registra `updated_by_id` con el admin y `updated_at` con la marca temporal actual

#### Scenario: Valor de modo inválido
- **GIVEN** un `Employee` autenticado con rol `ADMIN`
- **WHEN** envía `PUT /admin/settings` con `{ approvalMode: "SEMI" }`
- **THEN** el sistema responde 400 con `error` de validación y `fields` indicando `approvalMode`
- **AND** no modifica el valor persistido

#### Scenario: Un empleado intenta cambiar la configuración global
- **GIVEN** un `Employee` autenticado con rol `EMPLOYEE`
- **WHEN** envía `PUT /admin/settings` con `{ approvalMode: "AUTOMATIC" }`
- **THEN** el sistema responde 403
- **AND** no modifica el valor persistido
