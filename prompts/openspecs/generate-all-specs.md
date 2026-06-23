# PROMPT · Generar todas las specs de OpenSpec para parking

> Pégalo en una sesión del asistente con el repo `lcasadov/parking` abierto.
> Produce la estructura OpenSpec completa para **desarrollar el proyecto**.
> Ya existe como **referencia de oro** la capability `auth-local`
> (`openspec/specs/auth-local/spec.md` + `openspec/changes/init-auth-local/`):
> replica EXACTAMENTE ese formato para el resto.

---

## Rol y misión

Eres un analista de especificaciones (SDD/OpenSpec) del proyecto **parking** (ALEATICA).
Genera, para **las 15 capabilities** (12 del núcleo de parking + 3 del alcance ampliado de puestos):

1. `openspec/specs/<capability>/spec.md` — el spec canónico con Requirements y escenarios BDD.
2. `openspec/changes/init-<capability>/` — un change de inicialización con `proposal.md`, `design.md`, `tasks.md` y `specs/<capability>/spec.md` (delta `## ADDED Requirements`).

`openspec/config.yaml` ya existe (no lo toques salvo que falte).

## Convenciones OBLIGATORIAS (prevalecen sobre cualquier costumbre)

- **Idioma**: prosa en **español**; **todo identificador de código en INGLÉS** (entidades, enums, campos, endpoints, roles, estados). Autoridad: `README.md` §"Nomenclatura del código".
- **NO existe catálogo `RN-xx`**: referencia las reglas por **descripción** desde `README.md` §"Reglas de negocio". No inventes códigos.
- **Forma de error** (autoridad `docs/openapi.yaml`): `{ error, message, fields, timestamp }`. Códigos: **409** conflicto/unicidad/disponibilidad; **400** validación de campos/ventana; usa otros solo si `openapi.yaml` los declara.
- **Roles**: `ADMIN`, `EMPLOYEE`. **Estados**: `PENDING/APPROVED/REJECTED/CANCELLED`, etc. (ver `data-model.md`).
- **No copies código, DDL ni cuerpos JSON**: referencia `docs/data-model.md` (entidades/tablas) y `docs/openapi.yaml` (endpoints, por `operationId`).

## Entradas (leer antes de generar)

- `README.md` — capabilities (roadmap), reglas de negocio, fases, glosario, nomenclatura.
- `docs/PROJECT.md` — PRD + Anexo A (stack, dirs, base URL, roles).
- `docs/data-model.md` — entidades, tablas, enums, índices (en inglés).
- `docs/security-design.md` — RBAC por capability, OWASP, RGPD.
- `docs/openapi.yaml` — endpoints por capability (autoridad de rutas/operationId).
- `docs/TESTING-STRATEGY.md` — flujos críticos (para priorizar scenarios).
- **Referencia de formato**: `openspec/specs/auth-local/spec.md` y `openspec/changes/init-auth-local/`.

## Las 15 capabilities y su alcance

> 1-12 = núcleo de parking. 13-15 = **alcance ampliado de puestos de oficina** (ciclo posterior; sus endpoints **aún NO están en `docs/openapi.yaml`** → la spec los **propone** y los marca `_[no en openapi.yaml todavía]_`).

| # | Capability | Fase | Entidades clave | Endpoints clave (ver openapi.yaml) | Reglas/notas a cubrir |
|---|------------|------|-----------------|-------------------------------------|------------------------|
| 1 | `auth-local` | 🟢 F1 | Employee, LoginLog, SPRING_SESSION | `/auth/login,logout,me,change-password` | **YA GENERADA (referencia)**. No regenerar salvo que falte. |
| 2 | `auth-sso` | 🔵 F2 | Employee, LoginLog, SPRING_SESSION | `/ssocallback`, `/CloseSSOSessionID` | Landing autentica / parking autoriza; validar JWT (`iss=SSOTTS`, `aud=parking`, `exp`); rol desde `Employee.role` (ignora claim `roles`); `NO_ACCESS`/`INACTIVE`; Single Logout por `client_sid`; fallback de emergencia (rotación 90d). UI mínima (redirección + modal sesión expirada). |
| 3 | `employees` | 🟢🔵 | Employee | `listEmployees,createEmployee,updateEmployee,deactivateEmployee,reactivateEmployee,resetEmployeePassword,exportEmployees` | Solo `ADMIN`. Unicidad `login`/`email` → 409. Baja lógica (`active=false`). Reset → `password_must_change=true` (🟢 muestra temporal / 🔵 email). |
| 4 | `parking-spaces` | 🟢🔵 | ParkingSpace | `listParkingSpaces,createParkingSpace,updateParkingSpace,configureParkingSpaces` | Solo `ADMIN`. `label` único → 409. Activa/inactiva. Configuración masiva del total. |
| 5 | `fixed-assignments` | 🟢🔵 | FixedAssignment, Employee, ParkingSpace | `listFixedAssignments,getEmployeeFixedAssignments,setEmployeeFixedAssignments,revokeEmployeeFixedAssignment` | `ADMIN` gestiona; `EMPLOYEE` ve las propias. Unicidad recurso/día y empleado/día (filtered index) → 409. `day_of_week` 1-7. Revocación lógica (`active=false`, `revoked_*`). No afecta a días pasados. |
| 6 | `releases` | 🟢🔵 | Release, FixedAssignment, ParkingSpace | `listMyReleases,createRelease,cancelRelease,createAdministrativeRelease` | Voluntaria (`VOLUNTARY`, solo dueño, fecha ≥ hoy); administrativa (`ADMINISTRATIVE`, admin, `reason` obligatorio). Hace el recurso disponible esa fecha. Sin email. |
| 7 | `requests` | 🟢🔵 | Request, Employee, ParkingSpace, FixedAssignment, Release, VisitorReservation | `createRequest,listMyRequests,listPendingRequests,getRequest,cancelRequest,approveRequest,rejectRequest` | Ventana hoy..+14d → 400 `OUTSIDE_REQUEST_WINDOW`; unicidad `PENDING` por empleado/fecha → 409 `REQUEST_ALREADY_PENDING`; aprobar valida disponibilidad → 409; `approval_note` viaja en email; rechazo con `rejection_reason_code` (`NO_AVAILABILITY/OUTSIDE_POLICY/OTHER`) + `rejection_reason` libre (obligatorio si `OTHER`, ≥5). Concurrencia → 409. |
| 8 | `visitors` | 🟢🔵 | Visitor, VisitorReservation, ParkingSpace | `listVisitors,createVisitor,updateVisitor,getVisitor,listVisitorReservations,createVisitorReservation,cancelVisitorReservation` | Solo `ADMIN`. `national_id` único → 409. Reserva ocupa la plaza esa fecha (cuenta como no disponible). Sin email (el visitante no tiene cuenta). Editar ficha afecta solo a futuras reservas; anular solo futuras. |
| 9 | `availability-calendar` | 🟢🔵 | (consulta todas) | `getAvailability,getAdminCalendar,getMyWeek` | Disponibilidad para fecha F = activo + sin asignación fija ese día (o liberado) + sin `Request APPROVED` + (solo plazas) sin `VisitorReservation`. Calendario semanal solo `ADMIN`; "Mi Semana" no muestra nombres ajenos. |
| 10 | `audit-retention` | 🟢🔵 | AuditLog, LoginLog | `listAuditLog,listLoginLog` | `audit_log` vía `@Aspect` AOP sobre casos de uso anotados; `login_log` en el filtro de auth. Consulta/filtros solo `ADMIN`. Purga automática a 2 años (job `@Scheduled`, `DELETE TOP (1000)`); entidades vivas no se purgan. Transversal (poca UI). |
| 11 | `notifications` | 🟢🔵 | (plantillas Thymeleaf) | (sin endpoints; eventos de dominio) | Emails `AFTER_COMMIT`: nueva solicitud→admins activos; aprobada/rechazada→empleado; asignación revocada→empleado; 🔵 reset de contraseña→empleado. Fallo SMTP → log + reintento programado, **nunca** revierte la operación. No email al liberar voluntariamente ni al cancelar. Transversal (sin UI propia; efecto = toasts). |
| 12 | `exports` | 🟢🔵 | (histórico) | `exportEmployees,exportMyData,exportRequests,exportMyRequests,exportAuditLog` | Exportación **CSV/XLSX** (no PDF). RBAC por export (propias=`EMPLOYEE`; histórico/empleados/auditoría=`ADMIN`). Transversal (botones dispersos). |
| 13 | `generic-resource-refactor` | 🟢🔵 (refactor) | BookableResource (abstracción), ParkingSpace, Desk, Request, FixedAssignment, Release | (sin endpoints nuevos; refactor interno) | Generaliza `parking_space_id → resource_id` con `ResourceType` (`PARKING`/`DESK`) sobre `Request`, `FixedAssignment`, `Release` y disponibilidad. **Sin cambio de comportamiento visible**: los tests existentes deben seguir verdes (cubrir con tests de no-regresión). Incluye migración Flyway de datos. Prerrequisito de `desks`. |
| 14 | `desks` | 🟢🔵 | Desk (número 1-65, `DeskCategory` `STANDARD`/`EXECUTIVE`, `coord_x`/`coord_y`), FixedAssignment, Request, Release | `/desks` CRUD + asignación fija + solicitud de puesto _[no en openapi.yaml todavía → proponer]_ | 65 puestos numerados. Categorías `STANDARD` (cualquiera) / `EXECUTIVE` (habitual L-V de directivo, pero liberable igual). Un empleado puede tener plaza fija (`PARKING`) **y** puesto fijo (`DESK`) a la vez. Solo los 65 son reservables. Reutiliza `fixed-assignments`/`requests`/`releases`/`availability` vía `BookableResource`. Sin reserva de visitante en puestos. |
| 15 | `floor-plan` | 🟢🔵 | Desk (`coord_x`/`coord_y`) | `/floor-plan` (estado por fecha) + editor de posiciones _[no en openapi.yaml todavía → proponer]_ | Plano = imagen de planta + un marcador por puesto, coloreado por estado para la fecha (libre/asignado/solicitado/mi puesto/liberado/`EXECUTIVE`). El empleado pincha un puesto **libre** para solicitarlo directamente. El admin tiene **editor de arrastre** que persiste `coord_x`/`coord_y` (% de la imagen). El parking **no** tiene plano. |

> Dependencias típicas: casi todas dependen de `auth-local`/`auth-sso` (autenticación); `requests` consulta `availability-calendar` y dispara `notifications`; la mayoría dispara `audit-retention`. **Alcance ampliado**: `desks` depende de `generic-resource-refactor`; `floor-plan` depende de `desks`.

## Estructura y plantillas

### `spec.md` canónico (por capability)
```markdown
# Capability: <capability-en-inglés>

## Resumen
<2-3 líneas>

## Fase
🟢 Fase 1 | 🔵 Fase 2 | 🟢🔵 ambas

## Reglas de negocio implicadas
(README §"Reglas de negocio", por descripción — NO hay códigos RN-xx)
- <regla 1> · <regla 2> · ...

## Entidades implicadas
- <Entidad EN>, ...

## Endpoints
- <MÉTODO> /api/v1/... (operationId: <id>)

## Permisos
| Rol | Permisos |
|---|---|
| ADMIN | ... |
| EMPLOYEE | ... |

## Requirements
### Requirement 1: <nombre>
**El sistema DEBE <comportamiento>.**
#### Scenario: <caso feliz>
- **GIVEN** ... **WHEN** ... **THEN** ... **AND** ...
#### Scenario: <caso de error>
- **GIVEN** ... **WHEN** ... **THEN** ...
### Requirement 2 ... ### Requirement 3 ...

## Casos límite (edge cases)
- ...

## Dependencias con otras capabilities
- Depende de `auth-local` ...
```

### Change `init-<capability>/`
- **`proposal.md`**: `## Why` · `## What Changes` · `## Capabilities` (la capability como `ADDED`) · `## Impact` (entidades/seguridad/UI) · `## Out of scope`.
- **`design.md`**: `## Context` · `## Goals` · `## Decisions` (justificadas) · `## Risks` · `## Migration Plan` (Flyway/`data-model.md`).
- **`tasks.md`** — **orden TDD ESTRICTO (Red → Green → Refactor)**: `## 1. Tests primero — RED` (un test por cada Scenario, nombres `should..._when...`; deben fallar antes de implementar) → `## 2. Implementación — GREEN` (lo mínimo para que pasen) → `## 3. Refactor` → `## 4. Frontend` (mismo ciclo test-first). **Nunca** una tarea de implementación antes que su test. Empieza el fichero con una nota recordando el orden TDD.
- **`specs/<capability>/spec.md`**: delta con `## ADDED Requirements` (los mismos Requirements del spec canónico, en formato `### Requirement: <nombre>` + scenarios).

## Reglas de generación

- **Mínimos por capability**: ≥3 Requirements y ≥6 scenarios totales (camino feliz + error + autorización/validación). Para `requests`/`fixed-assignments`/`releases` cubre además **concurrencia** y **conflictos 409**.
- Cada scenario en **Given/When/Then estricto** (no narrativo).
- Cubre, donde aplique: validación de campos (400), autorización por rol (403), reglas de negocio, conflictos (409), y para capabilities transversales (`notifications`, `audit-retention`) los 3 casos de su naturaleza (p. ej. envío OK / fallo SMTP / reintento).
- **Coherencia**: entidades de `data-model.md`, endpoints/operationId de `openapi.yaml`, RBAC de `security-design.md`. Si detectas una contradicción con esos docs, **no la inventes**: márcala con `_[verificar con <doc>]_`.

## Salida y batching

Genera las **14 capabilities restantes** (todas menos `auth-local`, que ya existe) en este orden y por tandas (pide continuar entre tandas si el contexto se llena):

- **Tanda A**: `auth-sso`, `employees`, `parking-spaces`, `fixed-assignments`.
- **Tanda B**: `releases`, `requests`, `visitors`, `availability-calendar`.
- **Tanda C**: `audit-retention`, `notifications`, `exports`.
- **Tanda D** (alcance ampliado de puestos): `generic-resource-refactor`, `desks`, `floor-plan`.

Para cada capability escribe los **5 ficheros** (spec canónico + los 4 del change `init-`). Al terminar cada tanda, lista los ficheros creados.

## Restricciones finales

- Idempotente: no dupliques Requirements ni ficheros si ya existen.
- **Orden del alcance ampliado** (Tanda D): `generic-resource-refactor` **antes** que `desks`, y `desks` **antes** que `floor-plan`. En sus `proposal.md`/`design.md` deja claro que dependen del refactor de recurso genérico y que sus endpoints (`/desks`, `/floor-plan`) **aún no están en `docs/openapi.yaml`** (se proponen aquí; marcar `_[no en openapi.yaml todavía]_`).
- No crees capabilities fuera de las 15 listadas.
- No toques `openspec/config.yaml` ni la capability `auth-local` salvo que falten.
