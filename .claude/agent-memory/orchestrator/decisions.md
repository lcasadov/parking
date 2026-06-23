---
name: technical-decisions-parking
description: Decisiones de arquitectura y proceso adoptadas en parking (ALEATICA)
type: project
---

# parking — Decisiones técnicas y de proceso

> Cada entrada lleva fecha de adopción para juzgar relevancia futura.

## D-001 · Nomenclatura: negocio ES, código EN

**Adoptada:** 2026-06-20. **Decisión:** prosa funcional en español; entidades, clases, enums, campos, columnas y tablas en **inglés**. Autoridad: README "Nomenclatura del código". **Why:** el usuario lo pidió explícitamente; mantiene los docs accesibles para ALEATICA y el código en inglés estándar. **How to apply:** al generar cualquier doc o código, usar los nombres EN del README; no traducir contrato externo (claims JWT, `/ssocallback`, `parking_SESSION`, `SPRING_SESSION`, `parking.*`). Ver [[project_language_convention]].

## D-002 · Arquitectura hexagonal por módulo

**Adoptada:** 2026-06-20. **Decisión:** monolito modular con **hexagonal (puertos y adaptadores)** por módulo; dominio aislado de infraestructura. **Why:** reglas de negocio testeables sin BD, doble auth (Fase 1/2) como adaptadores sobre un mismo use case, persistencia reemplazable. **How to apply:** dominio sin dependencias de framework; adaptadores de entrada (REST/SSO/scheduler) → puertos de entrada; puertos de salida → adaptadores JPA/mail/audit/JWT/session. Ver `docs/architecture.md`.

## D-003 · Concurrencia en BD (filtered indexes), no locking

**Adoptada:** 2026-06-20. **Decisión:** las colisiones (aprobar misma plaza/fecha, unicidades) se resuelven con *filtered unique indexes* de SQL Server → `409 Conflict` determinista. **Why:** más simple y robusto que el bloqueo pesimista aplicativo. **How to apply:** ver `data-model.md` §4.

## D-004 · Stack fijado

**Adoptada:** 2026-06-20. Java **21 LTS** (no 22) · Spring Boot 3.3 · SQL Server 2022 · Tomcat 10.1 (WAR). Frontend: Vite + Vitest/RTL, **react-i18next**, **TanStack Query + Context**, design system propio (sin MUI), E2E **Playwright**. Despliegue **mismo origen** (Tomcat sirve SPA + API; sin reverse proxy dedicado → CORS solo en DES). JWT Fase 2 con **jjwt 0.12.x**. Spring Session **3.3.x** (BOM).

## D-005 · Gestión de trabajo en GitHub (con migración prevista a Azure DevOps)

**Adoptada:** 2026-06-20. **Decisión:** durante el arranque, gestión en **GitHub Projects v2** (Issues/Milestones), sincronizado con `backlog.md` por `gh-projects-sync`. **Migración prevista a Azure DevOps** (Repos + Pipelines YAML) al cerrar el arranque; SonarCloud se mantiene. **Why:** así lo define README/PROJECT de ALEATICA. **How to apply:** no asumir que GitHub es permanente; las variables de `docs/PROJECT.md` aíslan el proveedor.

## D-006 · Identidad git `lcasadov`

**Adoptada:** 2026-06-20 (sustituye a la heredada del bot). Todos los commits/PRs/`gh` con el usuario **`lcasadov`** (dueño del repo, autenticado en keyring). **Sin bot** `orquestadoria`. **Why:** el repo es personal de `lcasadov`, que ya tiene acceso ADMIN; el bot añadía fricción y su token (copiado de otro proyecto) ni siquiera daba acceso. **How to apply:** no cargar tokens de `.env`; `gh` usa el keyring. Para Projects v2 hace falta el scope `project` (`gh auth refresh -s project`).

## D-007 · Plan + aprobación antes de actuar; paralelismo por defecto

**Adoptada:** heredada (CLAUDE.md Phase 0/3). El orquestador presenta plan y espera "sí" antes de mutar. Delega agentes **en paralelo** salvo dependencia real (tests→tras código, PR→tras tests, migraciones→antes del código que las usa).

## D-008 · OpenSpec como fuente de requisitos

**Adoptada:** heredada. Cada cambio funcional en `openspec/changes/<slug>/` (proposal/design/tasks/specs). GitHub Projects = tracker de ejecución; OpenSpec = requisitos.

## D-009 · Decisiones de producto sobre mockups vs modelo

**Adoptada:** 2026-06-20. Plazas **solo por número** (`label`, sin ubicación). Aprobación con `approval_note` (viaja en email). Exportación **solo CSV/XLSX** (sin PDF). Rechazo con **catálogo** `rejection_reason_code` (`NO_AVAILABILITY`/`OUTSIDE_POLICY`/`OTHER`) + texto libre. Ver `ui-screens.md` §Inconsistencias y `data-model.md`/`openapi.yaml`.
