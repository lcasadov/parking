# Proposal: init-auth-sso

## Why
Inicializar la capability **auth-sso** (Fase 2) en OpenSpec: dejar documentado
como contrato verificable el comportamiento de autenticación delegada al SSO
corporativo de ALEATICA (la landing autentica, parking autoriza), el Single
Logout y el fallback de login local. En Fase 2 parking deja de tener pantalla
de login propia, por lo que la autorización por `id_token` se convierte en la
puerta de entrada de la que dependen el resto de capabilities.

## What Changes
- Se añade la capability `auth-sso` con sus Requirements y escenarios BDD.
- Endpoints (FUERA de `/api/v1`): `GET /ssocallback` (operationId `ssoCallback`),
  `POST /CloseSSOSessionID` (operationId `closeSsoSession`) (ver `docs/openapi.yaml`).
- Validación del `id_token` (firma, `iss = SSOTTS`, `aud = parking`, `exp`).
- Autorización por `login = username` con rol tomado de `Employee.role`
  (el claim `roles` del JWT se ignora); resultados `NO_ACCESS` / `INACTIVE`.
- Single Logout localizando la sesión por `client_sid` en `SPRING_SESSION`.
- Fallback de login local activable por config con rotación obligatoria a 90 días.
- UI mínima: redirección al/desde la landing + modal "Sesión expirada".

## Capabilities
- `auth-sso` (ADDED)

## Impact
- **Entidades**: `Employee` (`login`, `role`, `active`, `password_hash`,
  `last_password_change_at`), `LoginLog` (`result`, `phase`), `SPRING_SESSION`
  (atributos `client_sid`, `slo_token`, `employee_id`, `role`).
- **Seguridad**: validación estricta del JWT como entrada no confiable (API10);
  fail closed (403) ante firma/claims inválidos; rol interno autoritativo;
  `SameSite=Lax` para permitir el redirect del SSO (ver `docs/security-design.md`).
- **UI**: mínima (redirección + modal de sesión expirada).

## Out of scope
- Login local de Fase 1 como flujo principal (`auth-local`).
- Provisioning automático de empleados (el alta sigue siendo manual por el admin).
- Reset administrativo de contraseña y su email (lo aporta `employees`).
- Especificación del WS `consultaporlogin` y la gestión de la clave de firma por
  entorno (pendientes externos de ALEATICA — `docs/security-design.md`).
