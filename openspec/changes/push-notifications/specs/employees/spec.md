## ADDED Requirements

### Requirement: Preferencias de notificación por empleado editables por el admin

Cada empleado SHALL tener dos flags de notificación, `emailNotificationsEnabled` y `pushNotificationsEnabled`, con valor por defecto `true` (activos → retrocompatible con el comportamiento actual). El `ADMIN` SHALL poder consultarlos y cambiarlos de forma **independiente** desde el **formulario de empleado**. Estos flags actúan como segunda capa sobre las preferencias globales: un aviso llega por un canal solo si el flag global del canal y el del empleado están activos. El propio empleado NO gestiona estos flags (su opt‑in de push es la suscripción de dispositivo).

#### Scenario: Alta/edición con notificaciones por defecto activas

- **GIVEN** un ADMIN que crea o abre el formulario de un empleado
- **WHEN** no toca las preferencias de notificación
- **THEN** el empleado queda con `emailNotificationsEnabled = true` y `pushNotificationsEnabled = true`

#### Scenario: El admin silencia el push de un empleado

- **GIVEN** un ADMIN en el formulario de un empleado
- **WHEN** desmarca el checkbox de push del empleado y guarda
- **THEN** el sistema persiste `pushNotificationsEnabled = false` para ese empleado sin alterar su flag de email ni el de otros empleados

#### Scenario: Un empleado no puede cambiar sus flags de notificación

- **GIVEN** un usuario con rol `EMPLOYEE`
- **WHEN** intenta modificar sus flags de notificación de empleado
- **THEN** el sistema lo impide (la gestión de estos flags es del `ADMIN`, vía el formulario de empleado)
