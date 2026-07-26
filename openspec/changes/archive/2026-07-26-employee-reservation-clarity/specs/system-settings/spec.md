## ADDED Requirements

### Requirement: Lectura del modo de aprobación por cualquier autenticado
El sistema DEBE (MUST) exponer un endpoint de lectura del `approvalMode` global accesible a **cualquier** `Employee` autenticado (`EMPLOYEE`, `AGENCIA` o `ADMIN`), distinto del endpoint reservado a `ADMIN` (`GET /admin/settings`). Este endpoint DEBE (MUST) devolver **solo** el valor vigente (`approvalMode`) y NO DEBE (MUST NOT) exponer la trazabilidad del último cambio (`updatedById`/`updatedAt`), que sigue reservada al endpoint `ADMIN`-only.

#### Scenario: Un empleado consulta el modo vigente
- **GIVEN** un `Employee` autenticado con rol `EMPLOYEE`
- **WHEN** envía `GET /api/v1/settings/approval-mode`
- **THEN** el sistema responde 200 con `{ approvalMode }` reflejando el valor persistido
- **AND** la respuesta no incluye `updatedById` ni `updatedAt`

#### Scenario: Un usuario AGENCIA consulta el modo vigente
- **GIVEN** un `Employee` autenticado con rol `AGENCIA`
- **WHEN** envía `GET /api/v1/settings/approval-mode`
- **THEN** el sistema responde 200 con `{ approvalMode }`

#### Scenario: Sin sesión no se puede consultar el modo
- **GIVEN** una petición sin sesión autenticada
- **WHEN** se envía `GET /api/v1/settings/approval-mode`
- **THEN** el sistema responde 401

#### Scenario: El endpoint ADMIN-only conserva su restricción y su payload completo
- **GIVEN** un `Employee` autenticado con rol `EMPLOYEE`
- **WHEN** envía `GET /admin/settings`
- **THEN** el sistema responde 403 (el endpoint de trazabilidad completa sigue reservado a `ADMIN`)
