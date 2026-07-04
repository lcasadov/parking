# Tasks: init-audit-retention

> **Orden TDD estricto (Red → Green → Refactor).** Primero se escriben los tests (deben fallar), luego la implementación mínima para que pasen, luego el refactor. Ninguna tarea de implementación se aborda sin un test rojo previo. (Coherente con las oleadas 2-3 del flujo `apply` y `docs/TESTING-STRATEGY.md`.)

## 1. Tests primero — RED (deben fallar antes de implementar)
- [x] 1.1 `should_return_filtered_audit_page_when_admin_queries_with_valid_filters` (Requirement 1, camino feliz).
- [x] 1.2 `should_return_403_when_employee_queries_audit_log` (Requirement 1, autorización).
- [x] 1.3 `should_return_400_when_audit_window_from_after_to` (Requirement 1, validación de ventana).
- [x] 1.4 `should_return_only_invalid_credentials_when_login_log_filtered_by_result` (Requirement 2, camino feliz).
- [x] 1.5 `should_return_400_when_login_log_result_outside_enum` (Requirement 2, validación de enum).
- [x] 1.6 `should_return_403_when_employee_queries_login_log` (Requirement 2, autorización).
- [x] 1.7 `should_insert_audit_entry_when_auditable_use_case_succeeds` (Requirement 3, AOP camino feliz).
- [x] 1.8 `should_insert_audit_entry_with_null_actor_when_system_action` (Requirement 3, actor del sistema).
- [x] 1.9 `should_delete_old_audit_rows_in_batches_when_purge_runs` (Requirement 4, purga por lotes con `ClockPort` fijo).
- [x] 1.10 `should_not_delete_live_entities_when_purge_runs` (Requirement 4, entidades vivas intactas).
- [x] 1.11 `should_compute_cutoff_from_configured_years_when_retention_reconfigured` (Requirement 4, ventana configurable).
- [x] 1.12 Cada scenario BDD del spec cubierto por ≥1 test (nombres `should..._when...`).

## 2. Implementación — GREEN (lo mínimo para que los tests pasen)
- [x] 2.1 Entidades `AuditLog` (`audit_log`) y `LoginLog` (`login_log`) + repositorios (puerto + adaptador JPA), con `occurred_at` indexado.
- [x] 2.2 `@Auditable` + `@Aspect` Spring AOP que inserta en `audit_log` (`actor_employee_id`, `action`, `entity_type`, `entity_id`, `details` JSON, `occurred_at`).
- [x] 2.3 Serialización de atributos enriquecidos (`actor_login`, `ip`, `user_agent`, snapshot antes/después) como JSON en `details`.
- [x] 2.4 Registro best-effort: el fallo de inserción en `audit_log` se loguea y NO revierte la operación de negocio.
- [x] 2.5 Registro en `login_log` desde el filtro de autenticación (reutiliza el de `auth-local`/`auth-sso`).
- [x] 2.6 `AuditQueryService`: consulta paginada de `audit_log` con filtros `actorEmployeeId`, `action`, `from`, `to`; validación de ventana (`from <= to`).
- [x] 2.7 `LoginLogQueryService`: consulta paginada de `login_log` con filtros `result` (enum), `from`, `to`.
- [x] 2.8 Controllers: `GET /audit` (`listAuditLog`) y `GET /login-logs` (`listLoginLog`), ambos `@PreAuthorize("hasRole('ADMIN')")`; DTO sin datos sensibles.
- [x] 2.9 `RetentionPurgeJob` `@Scheduled` diario: `DELETE TOP (1000)` en bucle por `audit_log`, `login_log`, `requests` cerradas, `releases`, `visitor_reservations`; cutoff desde `parking.retention.years` (default 2) vía `ClockPort`.
- [x] 2.10 Manejo de errores uniforme (`ApiError { error, message, fields, timestamp }`); 400 ventana/enum inválidos, 403 rol.

## 3. Refactor
- [x] 3.1 Con los tests en verde: extraer métodos (complejidad < 15), eliminar duplicación y aplicar `docs/SONAR-STANDARDS.md`, sin cambiar comportamiento.

## 4. Frontend — mismo ciclo test-first (Vitest + RTL → implementación)
- [x] 4.1 Panel `ADMIN` de consulta de auditoría: tabla paginada con filtros `actorEmployeeId`, `action`, `from`/`to`.
- [x] 4.2 Panel `ADMIN` de consulta de logs de login: tabla paginada con filtros `result`, `from`/`to`.
- [x] 4.3 Ocultar ambos paneles a `EMPLOYEE` (RBAC en UI) y manejar 403.
