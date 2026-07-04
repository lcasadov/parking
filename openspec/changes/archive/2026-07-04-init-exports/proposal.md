# Proposal: init-exports

## Why
Inicializar la capability **exports** en OpenSpec: dejar documentado, como
contrato verificable, el mecanismo de exportación de datos a CSV/XLSX con su
RBAC por export. Incluye el derecho de acceso RGPD (`exportMyData`) que el
proyecto debe ofrecer al interesado, y las exportaciones operativas que los
administradores usan para reporting (empleados, histórico de solicitudes,
auditoría). Es transversal: sus botones aparecen en varias pantallas.

## What Changes
- Se añade la capability `exports` con sus Requirements y escenarios BDD.
- Endpoints (ver `docs/openapi.yaml`): `GET /employees/export` (`exportEmployees`),
  `GET /employees/me/export` (`exportMyData`), `GET /requests/export`
  (`exportRequests`), `GET /requests/mine/export` (`exportMyRequests`),
  `GET /audit/export` (`exportAuditLog`).
- Parámetro común `format` (enum `csv`/`xlsx`, por defecto `xlsx`).
- RBAC por export y comprobación de objeto en las exportaciones propias.

## Capabilities
- `exports` (ADDED)

## Impact
- **Entidades**: `Employee`, `Request`, `AuditLog` (orígenes de datos; sin
  cambios de esquema — se leen tablas existentes).
- **Seguridad**: RBAC por export (`docs/security-design.md` §3); comprobación de
  objeto en `exportMyData`/`exportMyRequests`; exclusión de campos sensibles de
  credenciales; límite de tasa 5/min; sanitización de fórmulas CSV.
- **UI**: botones de exportación en las pantallas de empleados, solicitudes,
  auditoría y "Mis datos"/"Mis solicitudes" (transversal, sin pantalla propia).

## Out of scope
- Exportación a **PDF** (no soportada).
- Informes agregados/analíticos o dashboards.
- Programación de exportaciones periódicas o envío por email.
- Supresión/anonimización RGPD (la cubre `employees`/retención).
