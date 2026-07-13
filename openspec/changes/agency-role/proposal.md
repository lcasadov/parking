## Why

El propietario necesita delegar en personal de una **agencia externa** una única función operativa: liberar plazas de garaje y puestos de oficina de cualquier empleado para una fecha, sin concederles ningún otro acceso administrativo. Hoy esa función (`POST /api/v1/releases/administrative`) está reservada al rol `ADMIN`, que además puede gestionar empleados, recursos, solicitudes, visitantes y auditoría. No existe un rol de privilegio mínimo para este caso.

## What Changes

Este es un cambio de **RBAC fail-closed**: se introduce un tercer rol con la mínima autoridad posible y se verifica explícitamente que queda excluido de todo lo demás.

- Se añade el rol **`AGENCIA`** al enum de roles (además de `ADMIN` y `EMPLOYEE`). El login emite `ROLE_AGENCIA` y la sesión funciona igual que para los demás roles.
- El endpoint de **liberación administrativa** (`POST /api/v1/releases/administrative`) pasa a permitir `hasAnyRole('ADMIN','AGENCIA')`. Un usuario `AGENCIA` puede liberar el recurso fijo (plaza de garaje o puesto de oficina) de **cualquier** empleado para una fecha presente o futura, con el mismo contrato (requiere `reason`, crea `Release` de tipo `ADMINISTRATIVE`) que hoy ejecuta el `ADMIN`.
- **Fail-closed:** todos los demás endpoints administrativos siguen exigiendo `hasRole('ADMIN')` — CRUD de empleados, plazas, puestos, visitantes; aprobar/rechazar solicitudes; auditoría; login-logs; calendario/festivos; asignaciones fijas. `AGENCIA` recibe `403` en todos ellos. Tampoco accede al portal de empleado (`/api/v1/releases`, `/api/v1/requests`, etc., que exigen `EMPLOYEE`).
- Frontend: al autenticarse un usuario `AGENCIA` se le enruta a un **shell mínimo** cuya única pantalla/ruta es la liberación administrativa (reutiliza `AdministrativeReleasesPage`). El guard `ProtectedRoute` bloquea cualquier otra ruta (admin o employee) y redirige.

## Capabilities

### New Capabilities
<!-- Ninguna. El comportamiento nuevo se expresa como modificación de capacidades existentes. -->

### Modified Capabilities
- `auth-local`: se amplía el conjunto de roles válidos con `AGENCIA`; el login redirige a un shell propio para ese rol.
- `releases`: la liberación administrativa deja de estar reservada a `ADMIN` y pasa a autorizar también a `AGENCIA`, manteniéndose fail-closed frente al resto de funciones.

## Impact

- **Backend:**
  - `employee/Role.java` — nuevo valor `AGENCIA` del enum.
  - `release/ReleaseController.java` — `@PreAuthorize` del método `createAdministrativeRelease` pasa de `hasRole('ADMIN')` a `hasAnyRole('ADMIN','AGENCIA')`.
  - Sin cambios en `AuthController`/`AuthService` (el authority `ROLE_<rol>` ya se emite de forma genérica) ni en el resto de controladores (siguen en `hasRole('ADMIN')`).
  - Migración/CHECK `CK_employees_role` (`docs/data-model.md` §3.1) debe admitir `AGENCIA`.
- **Frontend:**
  - `routes/paths.ts` — `type Role` incluye `AGENCIA`; `homePathForRole` mapea `AGENCIA` a su ruta.
  - `routes/AppRoutes.tsx` — nuevo shell/ruta mínima para `AGENCIA` protegido con `requiredRole="AGENCIA"`.
  - `auth/ProtectedRoute.tsx` — sin cambio funcional (ya bloquea por rol); se cubre `AGENCIA` en las pruebas RBAC.
- **Datos:** los usuarios de agencia se crean como empleados con `role = AGENCIA`.
- **Seguridad:** ampliación de superficie mínima; se exige verificación adversarial de que `AGENCIA` no accede a ninguna otra función.
