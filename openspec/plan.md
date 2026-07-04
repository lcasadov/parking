# Plan de ejecución — parking (ALEATICA)

> Tracker vivo del desarrollo. Actualiza el **estado** y el **%** de cada change al avanzar.
> Última actualización: **2026-07-03**.

## Dashboard

| Stream | Estado | Avance |
|--------|--------|--------|
| 📄 Documentación (README, PROJECT, data-model, architecture, security, openapi, testing, sonar, ui-screens, ux-flows, pull-requests, mockups, **design-system**) | ✅ completa | `██████████` **100%** |
| 📐 Especificación OpenSpec (config + 15 changes `init-`) | ✅ completa | `██████████` **100%** |
| ⚙️ Implementación (código backend + frontend) | 🔄 en curso — Fase A + Fase B completas + **C1 refactor genérico** ✅ (+ fix #43) · Puerta 3 e2e ✅ 4/4 · pendiente C2 desks, C3 floor-plan | `███████▌░░` **~75%** |
| **Avance global del proyecto** | 🔄 implementación | `████████░░` **~80%** |

> Ponderación del avance global: **preparación (docs+specs) ≈ 20%** del esfuerzo · **implementación ≈ 80%**. La preparación está hecha; el grueso (implementar las 15 capabilities + arranque) está por delante.

## Ciclo de vida de cada capability (= cómo se calcula su %)

> **Desarrollo en TDD estricto**: por cada capability se escriben primero los tests (rojo), luego la implementación mínima (verde), luego refactor. Las `tasks.md` están ordenadas así.

| Etapa | Peso | Qué significa |
|-------|------|---------------|
| 1. Propuesta OpenSpec | **15%** | `openspec/changes/init-<cap>/` con proposal/design/tasks/delta ✅ |
| 2. Tests primero — RED | **25%** | Un test por cada Scenario BDD; deben fallar. Cobertura objetivo ≥80% líneas / ≥75% ramas · 100% flujos críticos |
| 3. Implementación — GREEN | **40%** | Dominio (hexagonal), persistencia, migraciones Flyway, controllers; hasta que los tests pasen. + Refactor |
| 4. Verificación QA | **12%** | `verification-specialist` PASS + `reality-checker` READY |
| 5. Archivado | **8%** | `archive init-<cap>` → vuelca el delta a `openspec/specs/<cap>/` |

**Hoy todas las capabilities están en la etapa 1 (propuesta ✅) → 15% cada una.**

Leyenda de estado: ✅ hecho · 🔄 en curso · ⬜ pendiente.

---

## Fase A — Arranque (infraestructura, prerrequisito)

> Changes de infraestructura (no añaden capability propia; tocan `auth-local`/`audit-retention` con deltas mínimos).

| # | Change | Tipo | Depende de | Estado | % |
|---|--------|------|------------|--------|---|
| A1 | `bootstrap-mvp` | Backend infra (Spring Boot 3.3 · Java 21, Flyway base, Security mínima, Session JDBC, `/health`, error handler, AOP audit, Docker SQL Server, CI) | — | ✅ **Archivado** (PR #4 mergeado, CI verde, verificado PASS 96.5%/100%, specs sincronizadas) → `changes/archive/2026-06-27-bootstrap-mvp` | `██████████` 100% |
| A2 | `frontend-bootstrap` | Frontend infra (Vite + React 18, design system propio, TanStack Query + Context, auth flows, layouts, i18n, tema, interceptor 401, CI) | A1 (`/auth/*`) | ✅ **Archivado** (PR #14 mergeada, CI verde, verification-specialist PASS round 2 — 36 tests, 97.7%/91.1%, bugs #9 #10 #12 resueltos; Puerta 3 e2e ✅) → `changes/archive/2026-07-03-frontend-bootstrap` | `██████████` 100% |

---

## Fase B — Núcleo de parking

| # | Change | Fase | Depende de | Etapa | % |
|---|--------|------|------------|-------|---|
| B1 | `init-auth-local` | 🟢 | A1 | ✅ **Archivado** (PR #13 mergeada, CI verde, verification-specialist PASS round 2 — 74 tests, 95.4%/84.3%, auth 100%, bug #11 seed-dev resuelto; Puerta 3 e2e ✅) → `changes/archive/2026-07-03-init-auth-local` | `██████████` 100% |
| B2 | `init-employees` | 🟢🔵 | B1 | ✅ **Archivado** (PR #22 mergeada, CI verde, verification-specialist PASS — BE 107 tests + FE 54, EmployeeController 100/100, ~96.8%/85.4%; bug de contrato #21 `isCorporate` resuelto) → `changes/archive/2026-07-03-init-employees` | `██████████` 100% |
| B3 | `init-parking-spaces` | 🟢🔵 | A1 | ✅ **Archivado** (PR #25 mergeada, CI verde, verification-specialist PASS — BE 116 unit + 33 IT + FE 69, ParkingSpaceController 100/100, bundle 97.7%/87.1%; refactor FieldConflictException sin regresión de empleados) → `changes/archive/2026-07-03-init-parking-spaces` | `██████████` 100% |
| B4 | `init-fixed-assignments` | 🟢🔵 | B2, B3 | ✅ **Archivado** (PR #28 mergeada, CI verde tras hotfix #30, verification-specialist PASS — BE 151 unit + 48 IT + FE 95, BOLA sin agujeros, índices filtrados; hotfix #29 de aislamiento de ITs) → `changes/archive/2026-07-03-init-fixed-assignments` | `██████████` 100% |
| B5 | `init-requests` | 🟢🔵 | B3, B2 | ✅ **Archivado** (PR #34 mergeada, CI verde, verification-specialist PASS 8/8 — state machine, BOLA, concurrencia, IT order-independent en 3 órdenes; disponibilidad inline + notifications stub) → `changes/archive/2026-07-04-init-requests` | `██████████` 100% |
| B6 | `init-releases` | 🟢🔵 | B4 | ✅ **Archivado** (PR #37 mergeada, CI verde, verification-specialist PASS 7/7 — BOLA, unicidad/concurrencia, cancel-then-rerelease, IT order-independent 3 órdenes; auditoría stub) → `changes/archive/2026-07-04-init-releases` | `██████████` 100% |
| B7 | `init-availability-calendar` | 🟢🔵 | B5, B6, B8 | ✅ **Archivado** (PR #44 mergeada, CI verde, verification-specialist PASS 9/9 — disponibilidad consolidada, N+1 descartado, privacidad mi-semana, IT order-independent 3 órdenes) → `changes/archive/2026-07-04-init-availability-calendar`. Destapó #43 (divergencia de RequestService.approve → fix dirigido). | `██████████` 100% |
| B8 | `init-visitors` | 🟢🔵 | B3 | ✅ **Archivado** (PR #40 mergeada, CI verde, verification-specialist PASS 8/8 — disponibilidad inline 4 condiciones, concurrencia, RBAC ADMIN, IT order-independent 3 órdenes; auditoría stub) → `changes/archive/2026-07-04-init-visitors`. _Adelantado antes de B7 para que availability-calendar consuma reservas reales._ | `██████████` 100% |
| B9 | `init-notifications` | 🟢 | B5 | ✅ **Archivado** (PR #48 mergeada, CI verde, verification-specialist PASS 8/8 — email real SMTP+Thymeleaf, outbox V11 + reintentos idempotentes, 3 exclusiones con IT, rollback AFTER_COMMIT, sin regresión al eliminar el stub; IT order-independent 3 órdenes) → `changes/archive/2026-07-04-init-notifications`. Backend-only. | `██████████` 100% |
| B10 | `init-audit-retention` | 🟢🔵 | B1 | ✅ **Archivado** (PR #51 mergeada, CI verde, verification-specialist PASS — auditoría AOP persistente consolidando stubs, purga RGPD 2 años **leída SQL: solo histórico**, best-effort aislado, IT order-independent 3 órdenes) → `changes/archive/2026-07-04-init-audit-retention` | `██████████` 100% |
| B11 | `init-exports` | 🟢🔵 | B2, B5, B10 | ✅ **Archivado** (PR #54 mergeada, CI verde, verification-specialist PASS — CSV/XLSX, sanitización de fórmulas sin bypass, rate-limit 5/min per-user, BOLA, sin fugas de credenciales, exportMyData RGPD con datos propios; IT order-independent 3 órdenes) → `changes/archive/2026-07-04-init-exports` | `██████████` 100% |

**Subtotal núcleo (11 capabilities):** ✅ **100% — Fase B completa** (B1–B11 archivadas + fix #43) · `██████████`

---

## Fase C — Puestos de oficina (alcance ampliado)

| # | Change | Fase | Depende de | Etapa | % |
|---|--------|------|------------|-------|---|
| C1 | `init-generic-resource-refactor` | 🟢 | Fase B completa | ✅ **Archivado** (PR #58 mergeada, CI verde, verification-specialist PASS — refactor a resource_id+resource_type sin cambio de contrato, migración V12 con rollback, índices+nombres preservados, disponibilidad idéntica, ddl-auto=validate limpio, no-regresión 3 órdenes) → `changes/archive/2026-07-04-init-generic-resource-refactor` | `██████████` 100% |
| C2 | `init-desks` | 🟢🔵 | C1 | Propuesta ✅ | `█▌░░░░░░░░` 15% |
| C3 | `init-floor-plan` | 🟢🔵 | C2 | Propuesta ✅ | `█▌░░░░░░░░` 15% |

> ⚠️ `desks` y `floor-plan` requieren **ampliar `docs/openapi.yaml`** con `/desks` y `/floor-plan` (sus endpoints están marcados `_[no en openapi.yaml todavía]_`).

**Subtotal puestos (3 capabilities):** 15% medio · `█▌░░░░░░░░`

---

## Fase 2 — SSO ALEATICA (🔵 futura)

| # | Change | Depende de | Estado | % |
|---|--------|------------|--------|---|
| D1 | `init-auth-sso` | B1, datos de ALEATICA (clave JWT, URLs, spec `consultaporlogin`) | Propuesta ✅ · **bloqueada por inputs externos** | `█▌░░░░░░░░` 15% |

---

## Orden de ejecución recomendado

```
A1 bootstrap-mvp ──► A2 frontend-bootstrap
        │
        ▼
B1 auth-local ─► B2 employees ─► B3 parking-spaces
        │                              │
        ▼                              ▼
B4 fixed-assignments ◄───────────► B5 requests ─► B9 notifications
        │                              │
        ▼                              ▼
B6 releases ─────────────► B7 availability-calendar
                                       │
B8 visitors ───────────────────────────┤
B10 audit-retention · B11 exports ─────┘
        │
        ▼
C1 generic-resource-refactor ─► C2 desks ─► C3 floor-plan
        │
        ▼
D1 auth-sso (cuando lleguen los datos de ALEATICA)
```

## Flujo por capability (CLAUDE.md)

Para cada `init-<cap>`: `apply` (implementar guiándose por su `tasks.md`) → QA (`verification-specialist` + `reality-checker`) → `archive init-<cap>` (vuelca el delta a `openspec/specs/<cap>/`). Actualiza aquí su **etapa** y **%** en cada hito.

> ⚠️ **El `archive` de OpenSpec solo sincroniza el delta a `openspec/specs/<cap>/` y mueve el change a `openspec/changes/archive/`.** NO actualiza este plan ni la documentación. Es **responsabilidad del orquestador** (CLAUDE.md Phase 6) hacer, tras cada `archive`:
>
> 1. **`openspec/plan.md`** (este fichero) — pasar la capability a `100%` / etapa "Archivado ✅" y recalcular el Dashboard.
> 2. **`docs/openapi.yaml`** — si la capability añadió/cambió endpoints (p. ej. `desks` y `floor-plan` deben añadir `/desks` y `/floor-plan`).
> 3. **`docs/data-model.md`** — si añadió/cambió tablas o columnas (mantener el esquema objetivo consolidado al día).
> 4. **`docs/architecture.md` / `README.md`** — solo si cambió una decisión arquitectónica o el alcance funcional.
> 5. **`docs/tasks.md`** — estado + timestamps de la tarea (tracker de progreso, sobre todo si `gh` está offline).
>
> Regla práctica: si el cambio tocó el **contrato** (endpoints, esquema, comportamiento), la doc afectada se actualiza en el **mismo** paso de cierre; si solo tocó implementación interna, basta con el plan.

## Pendientes de preparación (antes de implementar)

- ✅ `docs/design-system.md` (CSS propio + Tabler, tokens de `styles.css`) — lo consumen `frontend-bootstrap` y `frontend-engineer`.
- ✅ **Keyword normativo MUST/SHALL**: barrido completado (commit `40ed924`, 56 reqs en 15 changes) + parche final en el delta de `frontend-bootstrap`. `openspec validate --all` → **16/16 passed**. Los archives con sync de `init-auth-local` y `frontend-bootstrap` pasaron sin incidencias.
- ⬜ Confirmar `GITHUB_PROJECT_NUMBER` y, si se usa Projects v2, `gh auth refresh -s project`.
- ⬜ Inputs externos de ALEATICA para Fase 2: clave de firma JWT, URLs PRE/PRO, spec `consultaporlogin`, SMTP de PRO, hash BCrypt del admin bootstrap.
