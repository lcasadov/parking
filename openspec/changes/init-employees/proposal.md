# Proposal: init-employees

## Why
Inicializar la capability **employees** en OpenSpec: dejar documentado como
contrato verificable el CRUD de personal corporativo (alta, edición, baja
lógica, reactivación, reset de contraseña y exportación), reservado a `ADMIN`.
Es la fuente de identidades y roles de la que dependen el resto de
capabilities (asignaciones, solicitudes, auditoría).

## What Changes
- Se añade la capability `employees` con sus Requirements y escenarios BDD.
- Endpoints (ver `docs/openapi.yaml`): `listEmployees`, `createEmployee`,
  `updateEmployee`, `deactivateEmployee`, `reactivateEmployee`,
  `resetEmployeePassword`, `exportEmployees`.
- Unicidad de `login` y `email` → 409 en colisión.
- Baja lógica (`active = false`); reset → `password_must_change = true`
  (🟢 contraseña temporal en pantalla / 🔵 email).

## Capabilities
- `employees` (ADDED)

## Impact
- **Entidades**: `Employee` (todos los campos del modelo; ver `docs/data-model.md` §3.1).
- **Seguridad**: todos los endpoints `@PreAuthorize("hasRole('ADMIN')")`
  (ver `docs/security-design.md`); el reset reutiliza la política de
  contraseña de `auth-local`.
- **UI**: pantalla de gestión de empleados (tabla + alta/edición + acciones
  de baja/reactivación/reset/exportación), solo visible para `ADMIN`.

## Out of scope
- Provisioning automático de empleados desde SSO (no existe; alta manual).
- Detalle del formato de exportación CSV/XLSX (capability `exports`).
- Lógica del email de reset (capability `notifications`).
- Cambio de contraseña por el propio empleado (capability `auth-local`).
