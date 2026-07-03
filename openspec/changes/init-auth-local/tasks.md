# Tasks: init-auth-local

> **Orden TDD estricto (Red → Green → Refactor).** Primero se escriben los tests (deben fallar), luego la implementación mínima para que pasen, luego el refactor. Ninguna tarea de implementación se aborda sin un test rojo previo. (Coherente con las oleadas 2-3 del flujo `apply` y `docs/TESTING-STRATEGY.md`.)

## 1. Tests primero — RED (deben fallar antes de implementar)
- [x] 1.1 Tests unitarios de `PasswordPolicy` y del cálculo de bloqueo (con `ClockPort` fijo).
- [x] 1.2 Tests de `AuthorizeUseCase` (login OK / inválido / bloqueado / inactivo), mockeando repos.
- [x] 1.3 `@WebMvcTest` de los endpoints de auth (estados HTTP + cookie).
- [x] 1.4 Cada scenario BDD del spec cubierto por ≥1 test (nombres `should..._when...`).

## 2. Implementación — GREEN (lo mínimo para que los tests pasen)
- [x] 2.1 Entidad `Employee` (campos de credenciales: `password_hash`, `password_must_change`, `failed_login_attempts`, `locked_until`, `last_password_change_at`) + repositorio (puerto + adaptador JPA).
- [x] 2.2 `PasswordPolicy` (dominio): ≥10, mayús/minús/dígito/símbolo, distinta de login/email.
- [x] 2.3 `AuthService`/`AuthorizeUseCase`: verificación BCrypt, contador de fallos, bloqueo 5/15 min, reinicio tras OK.
- [x] 2.4 `PasswordEncoder` BCrypt (coste 12) como bean.
- [x] 2.5 Sesión: Spring Session JDBC + cookie `parking_SESSION` (flags + TTL 60 min).
- [x] 2.6 Controllers: `POST /auth/login`, `POST /auth/logout`, `GET /auth/me`, `POST /auth/change-password`.
- [x] 2.7 `login_log`: registro en el filtro de autenticación (`result`, `phase=PHASE_1`).
- [x] 2.8 Manejo de errores uniforme (`ApiError { error, message, fields, timestamp }`).

## 3. Refactor
- [x] 3.1 Con los tests en verde: extraer métodos (complejidad < 15), eliminar duplicación y aplicar `docs/SONAR-STANDARDS.md`, sin cambiar comportamiento.

## 4. Frontend — mismo ciclo test-first (Vitest + RTL → implementación)
- [x] 4.1 (No aplica en este change — la pantalla de login llega en `frontend-bootstrap`.)
