# Tasks: init-auth-sso

> **Orden TDD estricto (Red → Green → Refactor).** Primero se escriben los tests (deben fallar), luego la implementación mínima para que pasen, luego el refactor. Ninguna tarea de implementación se aborda sin un test rojo previo. (Coherente con las oleadas 2-3 del flujo `apply` y `docs/TESTING-STRATEGY.md`.)

## 1. Tests primero — RED (deben fallar antes de implementar)
- [ ] 1.1 `should_authorize_and_redirect_when_id_token_valid_and_employee_active` (Req 1, camino feliz).
- [ ] 1.2 `should_return_403_when_signature_or_claims_invalid` (Req 1, error/fail closed).
- [ ] 1.3 `should_use_employee_role_when_jwt_roles_claim_present` (Req 1, rol interno autoritativo).
- [ ] 1.4 `should_return_403_no_access_when_username_has_no_employee` (Req 2, `NO_ACCESS`, sin provisioning).
- [ ] 1.5 `should_return_403_inactive_when_employee_active_false` (Req 2, `INACTIVE`).
- [ ] 1.6 `should_invalidate_session_when_close_sso_with_existing_client_sid` (Req 3, camino feliz).
- [ ] 1.7 `should_return_404_when_close_sso_with_unknown_client_sid` (Req 3, 404).
- [ ] 1.8 `should_reject_local_login_when_fallback_disabled` (Req 4, fallback off).
- [ ] 1.9 `should_create_session_when_fallback_enabled_and_password_fresh` (Req 4, `FALLBACK_OK`).
- [ ] 1.10 `should_force_password_change_when_last_change_older_than_90_days` (Req 4, rotación, `ClockPort` fijo).
- [ ] 1.11 `@WebMvcTest` de `/ssocallback` y `/CloseSSOSessionID` (estados HTTP + cookie + Location).

## 2. Implementación — GREEN (lo mínimo para que los tests pasen)
- [ ] 2.1 `JwtValidator` (dominio): validar firma con la clave del entorno, `iss == SSOTTS`, `aud == parking`, `exp` no vencido, presencia de `username` y `client_sid`; fail closed.
- [ ] 2.2 `SsoAuthorizeUseCase`: buscar `Employee` por `login = username`; resolver `NO_ACCESS` (no existe) / `INACTIVE` (`active = false`) / OK; tomar rol de `Employee.role` ignorando claim `roles`.
- [ ] 2.3 Creación de sesión: guardar atributos `employee_id`, `role`, `client_sid`, `slo_token`; emitir cookie `parking_SESSION` (`HttpOnly`, `Secure`, `SameSite=Lax`, `Path=/parking-api`).
- [ ] 2.4 Controller `GET /ssocallback` (operationId `ssoCallback`, fuera de `/api/v1`): orquesta validación + autorización + 302 a `redirect_uri`; 403 en fallo.
- [ ] 2.5 `CloseSsoSessionUseCase` + controller `POST /CloseSSOSessionID` (operationId `closeSsoSession`): validar `slo_token`, localizar sesión por `client_sid` en `SPRING_SESSION`, invalidar; 200 o 404.
- [ ] 2.6 Fallback de login local: flag `parking.auth.local-fallback.enabled` (desactivado por defecto); rechazar login local si desactivado; permitir si activo.
- [ ] 2.7 Rotación obligatoria: si `last_password_change_at` > 90 días, forzar cambio antes de completar la autorización por fallback (`ClockPort` inyectable).
- [ ] 2.8 `login_log`: registrar `result` (`OK`/`NO_ACCESS`/`INACTIVE`/`FALLBACK_OK`) y `phase` (`PHASE_2`/`FALLBACK`).
- [ ] 2.9 Manejo de errores uniforme (`ApiError { error, message, fields, timestamp }`).

## 3. Refactor
- [ ] 3.1 Con los tests en verde: extraer métodos (complejidad < 15), eliminar duplicación y aplicar `docs/SONAR-STANDARDS.md`, sin cambiar comportamiento.

## 4. Frontend — mismo ciclo test-first (Vitest + RTL → implementación)
- [ ] 4.1 Redirección a la landing cuando no hay sesión y manejo del retorno desde `/ssocallback`.
- [ ] 4.2 Modal "Sesión expirada" ante cualquier 401 posterior → retorno a la landing.
