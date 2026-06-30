# Delta spec: auth-local (change bootstrap-mvp)

> Delta infraestructural: añade los endpoints placeholder de Fase 1. La
> autenticación real la implementa el change funcional de `auth-local`.

## ADDED Requirements

### Requirement: Endpoints de autenticación expuestos como placeholder
**El sistema DEBE (MUST) exponer los endpoints de autenticación de Fase 1 (`POST /auth/login`, `POST /auth/logout`, `GET /auth/me`, `POST /auth/change-password`) devolviendo `501 Not Implemented` hasta que el change funcional de `auth-local` los implemente.**

#### Scenario: Login placeholder devuelve 501
- **GIVEN** el backend arrancado con la configuración base (sin lógica de login)
- **WHEN** se llama a `POST /api/v1/auth/login`
- **THEN** el sistema responde `501` con el cuerpo de error uniforme `ApiError { error, message, fields, timestamp }`

#### Scenario: Rutas de auth son públicas
- **GIVEN** la cadena de filtros de seguridad mínima
- **WHEN** se accede a cualquier `/api/v1/auth/**` sin sesión
- **THEN** el sistema NO responde `401` (la ruta es pública), sino el `501` placeholder
