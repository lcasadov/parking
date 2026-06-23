# Proposal: init-auth-local

## Why
Inicializar la capability **auth-local** (Fase 1) en OpenSpec: dejar
documentado el comportamiento de autenticación local (login, bloqueo,
política de contraseña, cambio obligatorio) como contrato verificable
antes de implementarlo. Es la base de autenticación de la que dependen
el resto de capabilities.

## What Changes
- Se añade la capability `auth-local` con sus Requirements y escenarios BDD.
- Endpoints: `POST /auth/login`, `POST /auth/logout`, `GET /auth/me`,
  `POST /auth/change-password` (ver `docs/openapi.yaml`).
- Sesión por cookie `parking_SESSION` (Spring Session JDBC).

## Capabilities
- `auth-local` (ADDED)

## Impact
- **Entidades**: `Employee` (campos de credenciales y bloqueo), `LoginLog`,
  `SPRING_SESSION`.
- **Seguridad**: BCrypt (coste 12), bloqueo 5/15 min, política de contraseña
  (ver `docs/security-design.md`).
- **Sin frontend** en este change inicial (la pantalla de login llega en
  `frontend-bootstrap`).

## Out of scope
- SSO de Fase 2 (`auth-sso`).
- Provisioning automático de empleados.
- Reset administrativo de contraseña (lo aporta `employees`).
