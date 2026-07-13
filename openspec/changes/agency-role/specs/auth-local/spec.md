## ADDED Requirements

### Requirement: Rol AGENCIA de privilegio mínimo
**El sistema DEBE (MUST) reconocer un tercer rol funcional `AGENCIA`, además de `ADMIN` y `EMPLOYEE`, cuya ÚNICA autoridad es realizar la liberación administrativa de recursos; el rol DEBE emitirse en la sesión igual que los demás y NO DEBE conceder ninguna otra capacidad (fail-closed).**

#### Scenario: Login de un usuario AGENCIA emite el rol y la sesión
- **GIVEN** un `Employee` activo con `role = AGENCIA` y credenciales válidas
- **WHEN** envía `POST /auth/login` con `{ login, password }` correctos
- **THEN** el sistema responde 200 con la identidad incluyendo `role = AGENCIA`
- **AND** emite la cookie `parking_SESSION` y la autoridad de seguridad `ROLE_AGENCIA`

#### Scenario: AGENCIA queda excluida de las funciones de ADMIN
- **GIVEN** un usuario autenticado con `role = AGENCIA`
- **WHEN** invoca cualquier endpoint reservado a `ADMIN` (empleados, plazas, puestos, visitantes, solicitudes, auditoría, login-logs, calendario)
- **THEN** el sistema responde 403
- **AND** no ejecuta la acción solicitada

#### Scenario: AGENCIA queda excluida del portal de empleado
- **GIVEN** un usuario autenticado con `role = AGENCIA`
- **WHEN** invoca cualquier endpoint reservado a `EMPLOYEE` (liberación voluntaria propia, solicitudes propias)
- **THEN** el sistema responde 403
- **AND** no ejecuta la acción solicitada

#### Scenario: Añadir el rol no concede acceso por defecto a nuevos endpoints
- **GIVEN** el conjunto de endpoints protegidos con `hasRole('ADMIN')`
- **WHEN** se introduce el rol `AGENCIA` en el sistema
- **THEN** dichos endpoints siguen respondiendo 403 a `AGENCIA`
- **AND** solo el endpoint de liberación administrativa autoriza explícitamente a `AGENCIA`

### Requirement: Enrutado del shell mínimo de AGENCIA en el frontend
**La SPA DEBE (MUST) redirigir a un usuario `AGENCIA` autenticado a un shell mínimo cuya única pantalla es la liberación administrativa, y el guard `ProtectedRoute` DEBE bloquear cualquier otra ruta (admin o empleado) para ese rol.**

#### Scenario: Login de AGENCIA redirige a su shell mínimo
- **GIVEN** un usuario en `/login`
- **WHEN** envía credenciales válidas de un usuario con `role = AGENCIA`
- **THEN** la SPA lo redirige a la ruta base del shell de agencia (liberación administrativa)
- **AND** no lo lleva a `/admin` ni a `/employee`

#### Scenario: AGENCIA no puede acceder a rutas de ADMIN o EMPLOYEE
- **GIVEN** un usuario `AGENCIA` autenticado
- **WHEN** navega a una ruta bajo `/admin` o `/employee`
- **THEN** el guard `ProtectedRoute` deniega el acceso por rol y lo redirige fuera de esa ruta
- **AND** no monta la pantalla protegida

#### Scenario: ADMIN y EMPLOYEE no pueden acceder al shell de AGENCIA
- **GIVEN** un usuario autenticado con `role = ADMIN` o `role = EMPLOYEE`
- **WHEN** navega a la ruta base del shell de agencia
- **THEN** el guard `ProtectedRoute` deniega el acceso por rol y lo redirige fuera de esa ruta
- **AND** no monta la pantalla de liberación administrativa del shell de agencia
