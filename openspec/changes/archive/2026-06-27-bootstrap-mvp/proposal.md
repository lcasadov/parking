# Proposal: bootstrap-mvp

## Why
Dejar el **backend** en un estado en el que las capabilities funcionales
puedan empezar a programar lógica de negocio sin perder tiempo en
configuración base. Es un change de **infraestructura, no funcional**:
no añade comportamiento visible para el usuario final.

## What Changes
- Estructura del repo (`backend/`, `database/`) e infra del proyecto.
- `pom.xml` (Spring Boot 3.3, **Java 21 LTS**, WAR), arquitectura hexagonal de paquetes.
- Configuración por perfiles (`application-{des,pre,pro}.yml`).
- **Spring Security mínima** (sin login real) + **Spring Session JDBC** + cookie `parking_SESSION`.
- **Flyway base**: solo tablas de infraestructura (`SPRING_SESSION`/`SPRING_SESSION_ATTRIBUTES`, `audit_log`, `login_log`) + sus índices. Las tablas **funcionales** las añade cada capability en su propio change.
- Endpoint `GET /api/v1/health` (público) → `{"status":"UP"}`.
- `@ControllerAdvice` global con error uniforme `ApiError { error, message, fields, timestamp }`.
- AOP de auditoría base (`@Auditable` + `AuditAspect`, sin uso todavía), `PasswordEncoder` BCrypt (coste 12) como bean, `logback-spring.xml`, OpenAPI/Swagger.
- `docker-compose.yml` (SQL Server 2022; MailHog opcional para UI de correo local).
- CI GitHub Actions (`mvn verify` con JDK 21, JaCoCo, SonarCloud).

## Capabilities afectadas (deltas mínimos)
- `auth-local` — **ADDED**: endpoints placeholder `/auth/*` que devuelven `501` hasta que el change funcional de auth los implemente.
- `audit-retention` — **ADDED**: las tablas `audit_log` y `login_log` existen (vía Flyway), aunque ningún servicio las pueble todavía.

> `bootstrap-mvp` no es una capability propia; toca esas dos de forma infraestructural.

## Impact
- Dirs `backend/` y `database/`; pipeline CI; perfiles de configuración.
- SMTP: **Ethereal hosted** (`smtp.ethereal.email`) vía `.env` (NO se dockeriza).
- Identidad git: `lcasadov` (sin bot).

## Out of scope
- Tablas y endpoints funcionales (`employees`, `parking-spaces`, `requests`, …) → sus changes.
- Login real (`/auth/login` devuelve 501) → change funcional de `auth-local`.
- Frontend → change `frontend-bootstrap`.
- SSO Fase 2; seed del admin inicial.
