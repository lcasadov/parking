## ADDED Requirements

### Requirement: Preferencias globales de canal de notificación (email y push independientes)

El sistema SHALL mantener en `system_settings` dos flags booleanos independientes: `emailNotificationsEnabled` y `pushNotificationsEnabled`, ambos con valor por defecto `true` (retrocompatible con el comportamiento actual). El `ADMIN` SHALL poder leer y cambiar cada flag por separado desde Configuración mediante checkboxes independientes. Cada flag gobierna si el canal correspondiente envía; las cuatro combinaciones SHALL ser válidas.

#### Scenario: Valores por defecto tras la migración

- **GIVEN** una instalación migrada sin que el ADMIN haya tocado las preferencias
- **WHEN** el sistema resuelve los canales habilitados
- **THEN** `emailNotificationsEnabled` y `pushNotificationsEnabled` valen `true` (comportamiento actual: email activo; push activo en cuanto haya suscripciones)

#### Scenario: El admin apaga solo el email

- **GIVEN** un ADMIN en Configuración
- **WHEN** desmarca el checkbox de email y guarda
- **THEN** el sistema persiste `emailNotificationsEnabled = false` sin alterar `pushNotificationsEnabled`

#### Scenario: Un empleado no puede cambiar las preferencias de canal

- **GIVEN** un usuario con rol `EMPLOYEE`
- **WHEN** intenta cambiar los flags de canal
- **THEN** el sistema responde 403 (la configuración global es solo de `ADMIN`)
