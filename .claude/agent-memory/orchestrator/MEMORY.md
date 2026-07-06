# Orchestrator Memory — Index

> Índice de memorias persistentes del orquestador para el proyecto **parking** (ALEATICA).
> Cada entrada apunta a un fichero hermano. Mantener conciso (≤ 200 líneas).

## Project context

- [Project context](context.md) — Dominio (plazas/puestos ALEATICA), stack (Java 21/Spring Boot 3.3/SQL Server/React 18), gestión en GitHub Projects (migración prevista a Azure DevOps), identidad git `lcasadov`, agentes, branching, OpenSpec.

## Decisions

- [Technical decisions](decisions.md) — Nomenclatura ES/EN, hexagonal por módulo, concurrencia en BD, stack fijado (Java 21, mismo origen, jjwt, Spring Session 3.3.x), gestión GitHub→ADO, identidad git, plan+aprobación, OpenSpec, decisiones de producto sobre mockups.

## Progress

- [Project progress](progress.md) — Fase de documentación (sin código aún); docs y mockups completados; agentes saneados; pendientes (GITHUB_PROJECT_NUMBER, backlog, datos externos de ALEATICA).

## Conventions

- [project_language_convention](project_language_convention.md) — parking: negocio en español, identificadores de código en inglés; nomenclatura autoritativa en README.md.

## Feedback

- [feedback_autonomous_execution](feedback_autonomous_execution.md) — tras aprobar el plan multi-change, encadenar changes sin pausas salvo bloqueo real; PR + CI-gate por change; agentes en worktree aislado.
- [feedback_mockups_plaza_puesto](feedback_mockups_plaza_puesto.md) — los mockups solo dibujan "plaza"; implementar también "puesto" en modales/pantallas (resource_type PARKING|DESK).
