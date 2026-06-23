---
name: project-context-parking
description: Contexto general del proyecto parking (ALEATICA) — dominio, stack, gestor de trabajo, agentes, identidad git, branching
type: project
---

# parking (ALEATICA) — Contexto del proyecto

## Dominio funcional

Gestión y asignación de **recursos reservables** corporativos de ALEATICA: **plazas de parking** y **puestos de oficina** (escritorios). Modelo común: asignación fija indefinida por día de la semana, solicitud puntual para una fecha concreta, liberación voluntaria o administrativa, y reservas para visitantes externos. Dos roles: `ADMIN` y `EMPLOYEE`.

Dos fases de autenticación:
- 🟢 **Fase 1 (actual):** login local (usuario + contraseña, BCrypt).
- 🔵 **Fase 2 (futura):** SSO con la landing corporativa ALEATICA (la landing **autentica**, parking **autoriza** vía JWT). Fallback de emergencia con login local desactivado por defecto.

Alcance ampliado: puestos de oficina (1-65) con plano interactivo y editor de posiciones.

## Stack técnico (de docs/PROJECT.md Anexo A)

- **Backend:** Java 21 LTS · Spring Boot 3.3 · WAR sobre Tomcat 10.1 · arquitectura **hexagonal por módulo**.
- **Persistencia:** Spring Data JPA · Hibernate 6.5 · Flyway 10 · **SQL Server 2022**.
- **Frontend:** React 18 · Vite · Vitest + RTL · **react-i18next** (ES/EN) · **TanStack Query + Context** · **design system propio** (CSS + Tabler Icons, sin MUI).
- **E2E:** Playwright.
- **Despliegue:** Tomcat sirve SPA + API en el **mismo origen** (sin reverse proxy dedicado). Entornos DES/PRE/PRO. Correo: Ethereal en LOCAL/DES/PRE, SMTP corporativo en PRO.

## Convención de nomenclatura (CRÍTICA)

Prosa de negocio en **español**; **todos los identificadores de código** (entidades, enums, campos, tablas) en **inglés**. Autoridad: sección "Nomenclatura del código" del `README.md`. Ver [[project_language_convention]].

## Documentación autoritativa

| Documento | Contenido |
|---|---|
| `README.md` | Especificación funcional y de negocio + nomenclatura ES→EN. |
| `docs/PROJECT.md` | PRD ejecutivo + **Anexo A** (config técnica para agentes). |
| `docs/data-model.md` | Modelo de datos canónico SQL Server. |
| `docs/architecture.md` | Arquitectura (hexagonal, C4, diagramas Mermaid). |
| `docs/security-design.md` | Seguridad, RBAC, OWASP, RGPD. |
| `docs/openapi.yaml` | Contrato API OpenAPI 3.1. |
| `docs/TESTING-STRATEGY.md` | Estrategia y umbrales de testing. |
| `docs/SONAR-STANDARDS.md` | Reglas de calidad / Quality Gate. |
| `docs/ui-screens.md`, `docs/ux-flows.md` | Catálogo de pantallas y flujos (de `docs/mockups/`). |
| `docs/pull-requests.md` | Proceso de PR. |

## Gestión de trabajo — GitHub Projects (de momento)

- Repo en **GitHub** (`lcasadov/parking`). Gestión vía **GitHub Issues + Project v2** (sincronizado con `backlog.md` por `gh-projects-sync`). `backlog.md` aún **no existe**.
- ⚠️ El README/PROJECT prevén **migrar a Azure DevOps** (Repos + Pipelines YAML) al cerrar el arranque; SonarCloud se mantiene. (Distinto de otros proyectos: aquí se migra HACIA ADO, no se descarta.)
- Variables (`GITHUB_ORG=lcasadov`, `GITHUB_REPO=parking`, `GITHUB_PROJECT_NUMBER` ⚠️ pendiente, `BASE_BRANCH=develop`, `REPO_ROOT=c:\proyectos\parking`) en `docs/PROJECT.md` Anexo A.

## Identidad git — `lcasadov`

- Todas las operaciones `git`/`gh` con el usuario **`lcasadov`** (propietario de `lcasadov/parking`, ADMIN, ya autenticado en `gh` vía keyring). **No se usa bot** (`orquestadoria` descartado; su `.env` eliminado). ⚠️ Para Projects v2 falta el scope `project` en el token de `lcasadov` → `gh auth refresh -s project`. Verificar `gh auth status | grep lcasadov`.

## Agentes

El **orquestador es la sesión principal** (guiada por `CLAUDE.md`), no un subagente — no hay `orchestrator.md`. Subagentes especializados en `.claude/agents/` (12): `backend-architect` · `frontend-engineer` · `devops-engineer` · `database-optimizer` · `tester-tdd` · `test-strategist` · `test-runner` · `verification-specialist` · `reality-checker` · `api-tester` · `security-auditor` · `gh-projects-sync`. (El proceso de PR vive en `docs/pull-requests.md`, no como agente.)

## Branching

Base `develop` (`main` = releases). Prefijos `feat/<area>/<issue>-<slug>`, `fix/…`, `chore/…`, `test/…` (áreas: backend/frontend/devops/security/tester). Toda tarea empieza creando rama desde `develop`.

## OpenSpec

Path `openspec/`. Cada cambio funcional → `openspec/changes/<slug>/` con `proposal.md`, `design.md`, `tasks.md`, `specs/<capability>/spec.md`. Task IDs referencian Issues de GitHub.
