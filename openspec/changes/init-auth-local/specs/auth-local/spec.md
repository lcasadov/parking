# Capability: auth-local

## Resumen
Autenticación local de Fase 1: login con usuario y contraseña (BCrypt),
sesión por cookie `parking_SESSION`, bloqueo por intentos fallidos,
política de contraseña y cambio obligatorio tras reset administrativo.

## Fase
🟢 Fase 1 (sustituida por `auth-sso` en Fase 2; el login local persiste como fallback de emergencia).

## Reglas de negocio implicadas
(README §"Autenticación — Fase 1"; NO hay códigos RN-xx)
- Hash BCrypt (coste 12).
- Bloqueo de cuenta tras 5 intentos fallidos consecutivos durante 15 minutos.
- Política de contraseña: ≥10 caracteres, mayúscula + minúscula + dígito + símbolo, distinta de `login` y `email`.
- Cambio obligatorio en el primer acceso tras un reset administrativo (`password_must_change = true`).

## Entidades implicadas
- Employee (credenciales: `login`, `password_hash`, `password_must_change`, `failed_login_attempts`, `locked_until`)
- LoginLog
- SPRING_SESSION (gestionada por el framework)

## Endpoints
- POST /api/v1/auth/login (público)
- POST /api/v1/auth/logout (sesión)
- GET /api/v1/auth/me (sesión)
- POST /api/v1/auth/change-password (sesión)

## Permisos
| Rol | Permisos |
|---|---|
| ADMIN | Login, logout, ver identidad propia, cambiar contraseña propia |
| EMPLOYEE | Login, logout, ver identidad propia, cambiar contraseña propia |

## ADDED Requirements
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

## Casos límite (edge cases)
- Empleado con `active = false` o `enabled = false`: el login responde 401 genérico (resultado `INACTIVE`), sin distinguirlo de credenciales inválidas.
- En Fase 2, `POST /auth/login` solo responde si el fallback de emergencia está activado; si no, devuelve 404.
- El contador `failed_login_attempts` se reinicia a 0 tras un login correcto.

## Dependencias con otras capabilities
- Es base de autenticación para **todas** las demás capabilities.
- El reset administrativo que pone `password_must_change = true` lo dispara `employees`.
- Comparte el modelo de sesión con `auth-sso` (Fase 2).
