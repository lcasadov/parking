# Capability: auth-sso

## Resumen
Autenticación de Fase 2 mediante SSO corporativo de ALEATICA: la landing
autentica (EntraID o credenciales locales de la landing) y parking autoriza.
parking valida el `id_token` (JWT), busca el `username` en su tabla `employees`
por `login`, decide acceso y rol desde `Employee.role`, crea sesión `parking_SESSION`
y redirige. Incluye Single Logout y un fallback de login local activable por config.

## Fase
🔵 Fase 2 (sustituye la pantalla de login de `auth-local`; el login local persiste como fallback de emergencia).

## Reglas de negocio implicadas
(README §"Autenticación — Fase 2: SSO ALEATICA"; NO hay códigos RN-xx)
- Reparto de responsabilidades: la landing autentica, parking autoriza. No hay provisioning automático.
- Validación del `id_token`: firma válida con la clave compartida con `SSOTTS`, `iss == SSOTTS`, `aud == parking`, `exp` no vencido; `username` y `client_sid` presentes. Cualquier fallo → fail closed (403).
- Búsqueda de `Employee` por `login = username`: si no existe → `NO_ACCESS`; si existe pero `active = false` → `INACTIVE`.
- El rol se toma de `Employee.role` (`ADMIN`/`EMPLOYEE`); el claim `roles` del JWT se ignora.
- Al autorizar se crea la sesión y se guardan atributos `employee_id`, `role`, `client_sid`, `slo_token`.
- Single Logout: localizar la sesión por el atributo `client_sid` en `SPRING_SESSION` e invalidarla.
- Fallback de emergencia: desactivado por defecto; activable por configuración; cualquier empleado con contraseña válida puede usarlo; rotación obligatoria cada 90 días.
- Cada intento queda en `login_log` con `phase` (`PHASE_2` o `FALLBACK`) y `result`.

## Entidades implicadas
- Employee (`login`, `role`, `active`, `password_hash`, `last_password_change_at`)
- LoginLog (`result`, `phase`, `login_attempted`, `employee_id`, `occurred_at`)
- SPRING_SESSION (gestionada por el framework; atributos `client_sid`, `slo_token`, `employee_id`, `role`)

## Endpoints
> Montados FUERA de `/api/v1` (servidor base `/parking-api`).
- GET /ssocallback (operationId: ssoCallback) — público (`security: []`)
- POST /CloseSSOSessionID (operationId: closeSsoSession) — autenticado por `slo_token` firmado, no por cookie (`security: []`)
- POST /api/v1/auth/login (operationId del `auth-local`) — solo responde con el fallback de emergencia activado; si no, se rechaza _[verificar con docs/openapi.yaml: el endpoint del fallback es el mismo de auth-local, sin operationId propio en Fase 2]_

## Permisos
| Rol | Permisos |
|---|---|
| ADMIN | Acceder vía SSO (rol tomado de `Employee.role`), cerrar sesión vía Single Logout, usar fallback de emergencia si está activo |
| EMPLOYEE | Acceder vía SSO (rol tomado de `Employee.role`), cerrar sesión vía Single Logout, usar fallback de emergencia si está activo |

## ADDED Requirements
### Requirement: Autorización SSO en `/ssocallback`
**El sistema DEBE validar el `id_token`, autorizar al empleado por `login = username`, tomar el rol de `Employee.role`, crear la sesión `parking_SESSION` y redirigir a `redirect_uri`.**

#### Scenario: Callback con `id_token` válido de empleado activo
- **GIVEN** un `Employee` activo (`active = true`) cuyo `login` coincide con el claim `username`
- **WHEN** la landing redirige a `GET /ssocallback?id_token=...&client_id=parking&redirect_uri=...` con firma válida, `iss = SSOTTS`, `aud = parking` y `exp` no vencido
- **THEN** el sistema crea la sesión y guarda los atributos `employee_id`, `role`, `client_sid`, `slo_token`
- **AND** toma el rol de `Employee.role` ignorando cualquier claim `roles` del JWT
- **AND** emite la cookie `parking_SESSION` (`HttpOnly`, `Secure`, `SameSite=Lax`, `Path=/parking-api`) y responde 302 a `redirect_uri`
- **AND** registra `login_log` con `result = OK`, `phase = PHASE_2`

#### Scenario: Callback con firma o claims inválidos
- **GIVEN** un `id_token` con firma inválida, o `iss != SSOTTS`, o `aud != parking`, o `exp` vencido
- **WHEN** se invoca `GET /ssocallback`
- **THEN** el sistema responde 403 (fail closed) con la forma de error `{ error, message, fields, timestamp }`
- **AND** no crea sesión

### Requirement: Control de acceso por existencia y estado del empleado
**El sistema DEBE denegar el acceso si el `username` no corresponde a ningún `Employee` o si el `Employee` está inactivo, sin hacer provisioning automático.**

#### Scenario: `username` sin empleado correspondiente
- **GIVEN** un `id_token` válido cuyo claim `username` no coincide con ningún `Employee.login`
- **WHEN** se invoca `GET /ssocallback`
- **THEN** el sistema responde 403 "Sin acceso."
- **AND** no crea ningún empleado (sin provisioning automático)
- **AND** registra `login_log` con `result = NO_ACCESS`, `phase = PHASE_2`

#### Scenario: Empleado existente pero inactivo
- **GIVEN** un `Employee` con `active = false` cuyo `login` coincide con el claim `username`
- **WHEN** se invoca `GET /ssocallback` con un `id_token` válido
- **THEN** el sistema responde 403 "Cuenta inactiva."
- **AND** no crea sesión
- **AND** registra `login_log` con `result = INACTIVE`, `phase = PHASE_2`

### Requirement: Single Logout por `client_sid`
**El sistema DEBE invalidar la sesión de parking localizándola por el atributo `client_sid` en `SPRING_SESSION` cuando la landing lo solicita con un `slo_token` válido.**

#### Scenario: Single Logout de una sesión existente
- **GIVEN** una sesión activa con atributo `client_sid` conocido
- **WHEN** la landing envía `POST /CloseSSOSessionID` con `{ slo_token, client_sid }` válidos
- **THEN** el sistema valida el `slo_token`, localiza la sesión por `client_sid` y la invalida
- **AND** responde 200

#### Scenario: Single Logout de una sesión inexistente
- **GIVEN** un `client_sid` que no corresponde a ninguna sesión activa
- **WHEN** la landing envía `POST /CloseSSOSessionID` con `{ slo_token, client_sid }` válidos
- **THEN** el sistema responde 404 con la forma de error `{ error, message, fields, timestamp }`

### Requirement: Fallback de login local con rotación obligatoria
**El sistema DEBE permitir el login local solo cuando el fallback de emergencia está activado por configuración, y obligar a rotar la contraseña cuando han pasado más de 90 días desde el último cambio.**

#### Scenario: Login local con fallback desactivado
- **GIVEN** la configuración `parking.auth.local-fallback.enabled = false`
- **WHEN** un empleado envía `POST /auth/login` con credenciales correctas
- **THEN** el sistema rechaza la petición (no autoriza vía fallback)
- **AND** no crea sesión vía login local

#### Scenario: Login local con fallback activado y contraseña vigente
- **GIVEN** la configuración `parking.auth.local-fallback.enabled = true` y un `Employee` activo con `last_password_change_at` dentro de los últimos 90 días
- **WHEN** el empleado envía `POST /auth/login` con credenciales correctas
- **THEN** el sistema crea la sesión y emite la cookie `parking_SESSION`
- **AND** registra `login_log` con `result = FALLBACK_OK`, `phase = FALLBACK`

#### Scenario: Login local con contraseña caducada (>90 días)
- **GIVEN** la configuración `parking.auth.local-fallback.enabled = true` y un `Employee` cuyo `last_password_change_at` es de hace más de 90 días
- **WHEN** el empleado envía `POST /auth/login` con credenciales correctas
- **THEN** el sistema obliga a cambiar la contraseña antes de continuar y no completa la autorización hasta el cambio

## Casos límite (edge cases)
- `id_token` válido pero sin claim `client_sid` o sin `username` → 403 (claims obligatorios ausentes).
- Empleados creados en Fase 2 nacen con `password_hash = NULL`: no pueden usar el fallback hasta que el admin ejecute un reset administrativo (capability `employees`).
- El `login` debe coincidir **exactamente** con el `username` emitido por la landing (sin normalización implícita asumida).
- Single Logout con `slo_token` inválido o no correlacionado → no invalida ninguna sesión ajena (fail closed); se trata como entrada no confiable.
- Cualquier 401 posterior en el frontend ya autenticado → modal "Sesión expirada" y retorno a la landing (UI mínima de Fase 2).
- La cookie usa `SameSite=Lax` (no `Strict`) precisamente para permitir el redirect top-level por GET de vuelta desde la landing.

## Dependencias con otras capabilities
- Comparte el modelo de sesión (`parking_SESSION`, `SPRING_SESSION`) y el fallback con `auth-local` (Fase 1).
- Es base de autenticación/autorización para **todas** las demás capabilities en Fase 2.
- El alta y la activación del login local (reset administrativo → contraseña por email) los aporta `employees`.
- Registra eventos en `login_log`, consultable vía `audit-retention`.
