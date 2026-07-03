# auth-local Specification

## Purpose
TBD - created by archiving change bootstrap-mvp. Update Purpose after archive.
## Requirements
### Requirement: Frontend de autenticación local
**El sistema DEBE (MUST) ofrecer una interfaz de inicio de sesión de Fase 1 que autentique contra `POST /auth/login`, redirija según el rol y gestione el cambio obligatorio de contraseña y la expiración de sesión.**

#### Scenario: Login correcto redirige según rol
- **GIVEN** un usuario en `/login`
- **WHEN** envía credenciales válidas
- **THEN** el backend crea la sesión (cookie `parking_SESSION`)
- **AND** la SPA redirige a `/admin` si el rol es `ADMIN` o a `/employee` si es `EMPLOYEE`

#### Scenario: Login incorrecto muestra error inline
- **GIVEN** un usuario en `/login`
- **WHEN** envía credenciales inválidas (`401`)
- **THEN** la SPA muestra un mensaje de error inline genérico
- **AND** no navega fuera de `/login`

#### Scenario: Cambio obligatorio tras reset administrativo
- **GIVEN** un empleado autenticado cuyo `GET /auth/me` indica `passwordMustChange = true`
- **WHEN** intenta acceder a cualquier ruta protegida
- **THEN** la SPA lo redirige a `/change-password` y no le permite continuar hasta cambiarla

#### Scenario: Sesión expirada
- **GIVEN** una SPA con una sesión que ha caducado en el backend
- **WHEN** una llamada a la API responde `401`
- **THEN** el interceptor muestra el modal "Sesión expirada"
- **AND** al cerrarlo redirige a `/login`

### Requirement: Login local
**El sistema DEBE (MUST) autenticar a un empleado activo con `login` + contraseña válida, crear sesión y emitir la cookie `parking_SESSION`.**

#### Scenario: Login con credenciales válidas
- **GIVEN** un `Employee` activo (`active = true`, `enabled = true`) con `password_hash` y contraseña conocida
- **WHEN** envía `POST /auth/login` con `{ login, password }` correctos
- **THEN** el sistema responde 200 con la identidad (`employeeId`, `login`, `role`)
- **AND** emite la cookie `parking_SESSION` (`HttpOnly`, `Secure`, `SameSite=Lax`)
- **AND** registra `login_log` con `result = OK`, `phase = PHASE_1`

#### Scenario: Login con contraseña incorrecta
- **GIVEN** un `Employee` activo
- **WHEN** envía `POST /auth/login` con contraseña incorrecta
- **THEN** el sistema responde 401 (mensaje genérico, sin revelar si el `login` existe)
- **AND** incrementa `failed_login_attempts`
- **AND** registra `login_log` con `result = INVALID_CREDENTIALS`

### Requirement: Bloqueo por intentos fallidos
**El sistema DEBE (MUST) bloquear la cuenta durante 15 minutos tras 5 intentos fallidos consecutivos.**

#### Scenario: Quinto intento fallido bloquea la cuenta
- **GIVEN** un `Employee` con `failed_login_attempts = 4`
- **WHEN** falla un quinto intento de login
- **THEN** el sistema fija `locked_until = ahora + 15 min`
- **AND** responde 401 y registra `login_log` con `result = LOCKED`

#### Scenario: Intento durante el bloqueo
- **GIVEN** un `Employee` con `locked_until > ahora`
- **WHEN** intenta hacer login (aunque la contraseña sea correcta)
- **THEN** el sistema responde 401 "cuenta temporalmente bloqueada"
- **AND** registra `login_log` con `result = LOCKED`

### Requirement: Cambio de contraseña con política
**El sistema DEBE (MUST) permitir al empleado cambiar su contraseña cumpliendo la política, y exigir el cambio en el primer acceso tras un reset.**

#### Scenario: Cambio de contraseña válido
- **GIVEN** un empleado autenticado
- **WHEN** envía `POST /auth/change-password` con `currentPassword` correcta y `newPassword` que cumple la política
- **THEN** el sistema actualiza `password_hash`, pone `password_must_change = false` y `last_password_change_at = ahora`
- **AND** responde 204

#### Scenario: Nueva contraseña que incumple la política
- **GIVEN** un empleado autenticado
- **WHEN** envía una `newPassword` sin símbolo (o <10 caracteres, o igual al `login`/`email`)
- **THEN** el sistema responde 400 con `error` de validación y `fields` indicando el incumplimiento
- **AND** no modifica la contraseña

