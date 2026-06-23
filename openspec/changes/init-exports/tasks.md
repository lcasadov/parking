# Tasks: init-exports

> **Orden TDD estricto (Red → Green → Refactor).** Primero se escriben los tests (deben fallar), luego la implementación mínima para que pasen, luego el refactor. Ninguna tarea de implementación se aborda sin un test rojo previo. (Coherente con las oleadas 2-3 del flujo `apply` y `docs/TESTING-STRATEGY.md`.)

## 1. Tests primero — RED (deben fallar antes de implementar)
- [ ] 1.1 `should_export_employees_xlsx_when_admin_requests` (Req 1, scenario admin XLSX; verifica ausencia de campos de credenciales).
- [ ] 1.2 `should_export_audit_csv_when_admin_requests` (Req 1, scenario auditoría CSV).
- [ ] 1.3 `should_return_403_when_employee_exports_requests_history` (Req 1, scenario empleado → histórico).
- [ ] 1.4 `should_export_only_own_personal_data_when_employee_requests_my_data` (Req 2, scenario datos propios).
- [ ] 1.5 `should_export_only_own_requests_when_employee_requests_my_requests` (Req 2, scenario solicitudes propias).
- [ ] 1.6 `should_return_401_when_unauthenticated_exports_my_data` (Req 2, scenario sin sesión).
- [ ] 1.7 `should_return_400_when_format_is_unsupported` (Req 3, scenario `format=pdf`).
- [ ] 1.8 `should_default_to_xlsx_when_format_omitted` (Req 3, scenario sin `format`).
- [ ] 1.9 `should_return_429_when_sixth_export_within_one_minute` (Req 4, scenario límite de tasa).
- [ ] 1.10 `should_emit_header_only_file_when_dataset_is_empty` (edge case sin filas).
- [ ] 1.11 `should_sanitize_formula_prefixes_when_writing_csv` (edge case inyección CSV).
- [ ] 1.12 Cada scenario BDD del spec cubierto por ≥1 test (nombres `should..._when...`).

## 2. Implementación — GREEN (lo mínimo para que los tests pasen)
- [ ] 2.1 Puerto de serialización `ExportWriterPort` con adaptadores `csv` y `xlsx` (escritura en streaming sobre el `OutputStream`).
- [ ] 2.2 Sanitización de fórmulas CSV (prefijar valores que empiezan por `=`, `+`, `-`, `@`).
- [ ] 2.3 Validación del parámetro `format` (enum `csv`/`xlsx`, por defecto `xlsx`); 400 con `fields` si es inválido.
- [ ] 2.4 Caso de uso `ExportEmployees` (solo `ADMIN`): proyección sin `password_hash`/`failed_login_attempts`/`locked_until`.
- [ ] 2.5 Caso de uso `ExportMyData` (RGPD): filtra por `employee_id == session.employee_id`; sin campos "Solo admins".
- [ ] 2.6 Caso de uso `ExportRequests` (solo `ADMIN`) y `ExportMyRequests` (filtra por sujeto de sesión).
- [ ] 2.7 Caso de uso `ExportAuditLog` (solo `ADMIN`).
- [ ] 2.8 Controllers: `GET /employees/export`, `GET /employees/me/export`, `GET /requests/export`, `GET /requests/mine/export`, `GET /audit/export` con `Content-Type` y `Content-Disposition` (nombre con timestamp).
- [ ] 2.9 RBAC por export (`@PreAuthorize`/política) según `docs/security-design.md` §3.
- [ ] 2.10 Límite de tasa 5/min por usuario en los endpoints de export.
- [ ] 2.11 Registro del evento de exportación en `audit_log`.
- [ ] 2.12 Manejo de errores uniforme (`ApiError { error, message, fields, timestamp }`).

## 3. Refactor
- [ ] 3.1 Con los tests en verde: extraer métodos (complejidad < 15), eliminar duplicación y aplicar `docs/SONAR-STANDARDS.md`, sin cambiar comportamiento.

## 4. Frontend — mismo ciclo test-first (Vitest + RTL → implementación)
- [ ] 4.1 Componente reutilizable "Exportar" con selector de formato (`csv`/`xlsx`) y descarga del fichero binario.
- [ ] 4.2 Botón de exportación en la pantalla de empleados (solo `ADMIN`).
- [ ] 4.3 Botón de exportación en histórico de solicitudes (solo `ADMIN`) y en "Mis solicitudes" (`EMPLOYEE`).
- [ ] 4.4 Botón de exportación en auditoría (solo `ADMIN`).
- [ ] 4.5 Acción "Exportar mis datos" (RGPD) accesible al usuario autenticado.
- [ ] 4.6 Ocultar/deshabilitar botones administrativos para `EMPLOYEE` (defensa en profundidad; el backend es la autoridad).
