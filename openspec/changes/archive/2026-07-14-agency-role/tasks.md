## 1. Backend — datos y rol

- [ ] 1.1 Añadir el valor `AGENCIA` al enum `com.aleatica.parking.employee.Role` con su Javadoc (mínimo privilegio: solo liberación administrativa)
- [ ] 1.2 Ampliar el CHECK `CK_employees_role` (migración de esquema) para admitir `AGENCIA`, alineado con `docs/data-model.md` §3.1
- [ ] 1.3 Verificar que `AuthController.establishSession` emite `ROLE_AGENCIA` sin cambios (autoridad genérica `ROLE_<name>`) y que `AuthService` devuelve `role = AGENCIA` en la identidad

## 2. Backend — autorización (TDD: tests primero)

- [ ] 2.1 (RED) Test de autorización: un usuario `AGENCIA` que hace `POST /api/v1/releases/administrative` con cuerpo válido obtiene 2xx (falla mientras el endpoint exija solo ADMIN)
- [ ] 2.2 (GREEN) Cambiar el `@PreAuthorize` de `ReleaseController.createAdministrativeRelease` de `hasRole('ADMIN')` a `hasAnyRole('ADMIN','AGENCIA')`
- [ ] 2.3 (RED→GREEN) Test de regresión: `ADMIN` sigue pudiendo liberar (201) y `EMPLOYEE` sigue recibiendo 403 en `POST /releases/administrative`
- [ ] 2.4 Matriz de exclusión fail-closed: tests que verifican que un usuario `AGENCIA` recibe 403 en endpoints representativos de cada área admin — empleados, plazas de garaje, puestos (desks), visitantes, solicitudes (aprobar/rechazar), auditoría, login-logs, calendario/festivos, asignaciones fijas
- [ ] 2.5 Matriz de exclusión del portal de empleado: tests que verifican que `AGENCIA` recibe 403 en `POST /releases`, `GET /releases/mine` y `DELETE /releases/{id}`
- [ ] 2.6 Ejecutar `mvn clean verify`: 0 failures, cobertura ≥80% líneas / ≥75% branches, 0 violations Sonar nuevas

## 3. Frontend — rol y enrutado (TDD: tests primero)

- [ ] 3.1 Añadir `'AGENCIA'` a `type Role` en `routes/paths.ts` y una ruta base del shell de agencia en `ROUTES`
- [ ] 3.2 Extender `homePathForRole` para mapear `AGENCIA` a la ruta base del shell de agencia
- [ ] 3.3 (RED) Test RBAC: un usuario `AGENCIA` que llega autenticado es enrutado a la liberación administrativa y NO a `/admin` ni `/employee`
- [ ] 3.4 (GREEN) En `routes/AppRoutes.tsx` montar un shell mínimo para `AGENCIA` cuya única ruta renderiza `AdministrativeReleasesPage`, protegido con `ProtectedRoute requiredRole="AGENCIA"`
- [ ] 3.5 Test RBAC de exclusión: `AGENCIA` navegando a rutas bajo `/admin` o `/employee` es redirigido por `ProtectedRoute`; y `ADMIN`/`EMPLOYEE` navegando al shell de agencia también son redirigidos
- [ ] 3.6 Ejecutar `npm run lint && npm test && npm run build`: 0 errores, cobertura ≥80%

## 4. Verificación y cierre

- [ ] 4.1 Verificación adversarial (verification-specialist): confirmar que `AGENCIA` SÍ libera y NO accede a nada más (backend 403 + frontend redirige)
- [ ] 4.2 Actualizar `openspec/specs/` vía archivado del change tras merge (delta → specs principales de `auth-local` y `releases`)
