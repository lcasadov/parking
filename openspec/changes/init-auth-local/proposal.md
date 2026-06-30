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

## Seed admin de desarrollo

Para poder probar el login real en LOCAL/DES y en los tests e2e (Puerta 3), la
migración `V5__seed_dev_admin.sql` inserta de forma **idempotente** un único
administrador local:

| Campo | Valor |
|---|---|
| `login` | `admin` |
| Contraseña | `Admin#Parking2026` |
| `email` | `admin.dev@aleatica.local` |
| `role` | `ADMIN` |
| `auth_origin` | `LOCAL` |
| `password_must_change` | `false` (es admin de desarrollo, no de reset) |

> ⚠️ **Solo desarrollo.** No es el administrador de PRODUCCIÓN. La contraseña
> cumple la política (≥10, mayúscula+minúscula+dígito+símbolo, distinta de
> `login`/`email`). El `password_hash` se genera con BCrypt(coste 12) y se
> embebe en la migración.

## Out of scope
- SSO de Fase 2 (`auth-sso`).
- Provisioning automático de empleados.
- Reset administrativo de contraseña (lo aporta `employees`).
