---
name: project-progress-parking
description: Estado actual del proyecto parking (ALEATICA) — fase, documentación, agentes, pendientes
type: project
---

# parking — Progreso del proyecto

> Actualizar al cierre/apertura de fases o hitos.

## Fase actual

**Fase de documentación / setup** — documentación completa, **aún sin código** (no hay `backend/`, `frontend/`, `pom.xml`, `package.json`).

Última actualización: **2026-06-20**.

## Hecho (2026-06-20)

- [x] `README.md` reducido a especificación funcional + tabla de nomenclatura ES→EN.
- [x] `docs/PROJECT.md` (PRD ejecutivo) + **Anexo A** (config técnica para agentes: REPO_ROOT, GITHUB_* reales, dirs, stack, base URLs, roles).
- [x] `docs/data-model.md` (DDL SQL Server, enums, Flyway, JPA, retención).
- [x] `docs/architecture.md` (hexagonal, C4 + secuencia + despliegue en Mermaid, ADRs, análisis de stack).
- [x] `docs/security-design.md` (auth, RBAC, OWASP API 2023, CORS, RGPD).
- [x] `docs/openapi.yaml` (OpenAPI 3.1, validado, 48 ops).
- [x] `docs/TESTING-STRATEGY.md` + `docs/SONAR-STANDARDS.md` alineados (Java 21, design system propio, Playwright).
- [x] `docs/ui-screens.md` + `docs/ux-flows.md` (catálogo de 24 pantallas + flujos, desde mockups).
- [x] `docs/mockups/` ampliado: creadas pantallas 8-19 (plazas, visitantes, auditoría, login, cambiar/reset contraseña, liberar móvil, plaza form, preferencias, modo oscuro, sesión expirada); `shell.js`/`styles.css`/`index.html` unificados.
- [x] `docs/pull-requests.md` (proceso de PR adaptado a parking).
- [x] `.env`/`.env.example`/`.gitignore` (SMTP Ethereal local/dev/pre, secretos fuera de git).
- [x] Agentes saneados: arreglado frontmatter de `backend-architect` y `tester-tdd`; creado `test-strategist`; `pull-requests` movido a `docs/`; limpiados restos de PadelPro/Telegram.
- [x] Eliminado worktree huérfano de PadelPro; memory del orquestador reseteado a parking.

## Próximos pasos sugeridos

1. **Confirmar `GITHUB_PROJECT_NUMBER`** (crear Project v2 si no existe) y rellenarlo en `docs/PROJECT.md`.
2. **Crear `backlog.md`** (épicas/features/historias por módulo del roadmap) cuando se arranque la gestión de sprints.
3. **Primer change OpenSpec** + scaffolding: `bootstrap-mvp` (Spring Boot + Flyway + seguridad + CI) vía `devops-engineer`/`backend-architect`.
4. **`test-strategist`** ya generó `TESTING-STRATEGY.md`; al implementar, usar `tester-tdd` (TDD) y luego `test-runner`.
5. Implementar por orden del roadmap del README: `auth-local` → `employees` → `parking-spaces` → `requests` → `fixed-assignments` → `releases` → `availability-calendar` → `visitors` → `notifications`; luego alcance ampliado de puestos.

## Pendientes / bloqueos

- ⚠️ `GITHUB_PROJECT_NUMBER` por confirmar.
- ⚠️ Datos externos de ALEATICA: clave de firma JWT (Fase 2), spec del WS `consultaporlogin`, dominios PRE/PRO, credenciales SMTP de PRO, hash BCrypt del admin bootstrap.
- ⚠️ `backlog.md` no creado (decisión: de momento no).
