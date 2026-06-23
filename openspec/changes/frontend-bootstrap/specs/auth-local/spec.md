# Delta spec: auth-local (change frontend-bootstrap)

> Delta de UI: añade el frontend de autenticación de Fase 1 (login, cambio
> obligatorio, sesión expirada). El backend de auth lo provee su change funcional.

## ADDED Requirements

### Requirement: Frontend de autenticación local
**El sistema DEBE ofrecer una interfaz de inicio de sesión de Fase 1 que autentique contra `POST /auth/login`, redirija según el rol y gestione el cambio obligatorio de contraseña y la expiración de sesión.**

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
