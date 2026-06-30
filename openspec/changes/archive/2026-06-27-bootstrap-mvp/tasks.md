# Tasks: bootstrap-mvp

> **Orden TDD estricto (Red → Green → Refactor).** Primero los tests (deben fallar), luego la implementación mínima para que pasen, luego el refactor. Aunque es un change de infraestructura, los objetivos verificables se expresan como tests. (Coherente con las oleadas 2-3 del flujo `apply` y `docs/TESTING-STRATEGY.md`.)

## 1. Tests primero — RED (deben fallar antes de implementar)
- [x] 1.1 `should_load_spring_context_when_app_starts` (smoke: el contexto arranca).
- [x] 1.2 `should_apply_flyway_migrations_when_starting` — test de integración con **Testcontainers (SQL Server 2022)**: verifica que existen `SPRING_SESSION`, `SPRING_SESSION_ATTRIBUTES`, `audit_log`, `login_log`.
- [x] 1.3 `should_return_200_up_when_get_health_unauthenticated` (`GET /api/v1/health` público → `{"status":"UP"}`).
- [x] 1.4 `should_return_401_when_access_protected_endpoint_unauthenticated` (seguridad mínima).
- [x] 1.5 `should_return_501_when_call_auth_login_placeholder` (y `/auth/logout`, `/auth/me`, `/auth/change-password`).
- [x] 1.6 `should_return_apierror_shape_when_validation_fails` (formato `{ error, message, fields, timestamp }` del `@ControllerAdvice`).
- [x] 1.7 `should_create_session_cookie_named_parking_SESSION_with_flags` (cookie `HttpOnly`/`SameSite=Lax`/`Path=/parking-api`).

## 2. Implementación — GREEN (lo mínimo para que los tests pasen)
- [x] 2.1 Estructura raíz: `.gitignore` (Java/Maven, `.env`, `application-local.yml`) [ya existía, respetado], `.editorconfig`, `docker-compose.yml` (SQL Server 2022; MailHog opcional), `database/seed/00_create_database.sql`.
- [x] 2.2 `backend/pom.xml`: Spring Boot 3.3, `<packaging>war</packaging>`, Java 21; deps web/security/data-jpa/validation/mail/spring-session-jdbc/flyway-core+flyway-sqlserver/mssql-jdbc/springdoc/mapstruct/jjwt(Fase 2); test: spring-boot-starter-test, testcontainers (mssqlserver, junit-jupiter), awaitility; JaCoCo (check 80/75).
- [x] 2.3 `parkingApplication extends SpringBootServletInitializer` + paquetes hexagonales `com.aleatica.parking.{config,auth,audit,exception,...}` con `package-info.java`.
- [x] 2.4 Perfiles `application.yml` + `application-{des,pre,pro}.yml` (vars `SMTP_*`, `parking.auth.local-fallback.enabled=false`, `parking.retention.years=2`).
- [x] 2.5 `SecurityConfig` mínima (health + `/auth/**` públicos, resto `authenticated()`); sin `UserDetailsService` real.
- [x] 2.6 `SessionConfig` (`@EnableJdbcHttpSession`) + cookie `parking_SESSION` (flags + TTL 60 min).
- [x] 2.7 Configuración Flyway + migraciones `V1__spring_session_schema.sql`, `V2__audit_and_login_log.sql`, `V3__infra_indexes.sql`.
- [x] 2.8 `/api/v1/health` + Actuator (`health,info`) protegido; controllers placeholder `/auth/*` → 501.
- [x] 2.9 `@ControllerAdvice` global (`MethodArgumentNotValidException`→400, `AccessDeniedException`→403, `EntityNotFoundException`→404, catch-all→500 sin stack) con `ApiError`.
- [x] 2.10 AOP base: anotación `@Auditable` + `AuditAspect` (`@Around` que escribe en `audit_log`; sin uso todavía).
- [x] 2.11 `PasswordEncoder` BCrypt (coste 12) como bean; `logback-spring.xml`; `OpenApiConfig` (Swagger UI).
- [x] 2.12 CI `.github/workflows/ci.yml` (setup JDK 21, cache Maven, `mvn verify`, reporte JaCoCo, SonarCloud con `SONAR_TOKEN`) + `sonar-project.properties`.

## 3. Refactor
- [x] 3.1 Con los tests en verde: limpiar configuración, aplicar `docs/SONAR-STANDARDS.md` (inyección por constructor, sin literales mágicos, etc.), sin cambiar comportamiento.

## Criterios de aceptación
- [x] Los 6 objetivos verificables del `design.md` se cumplen en máquina limpia.
- [x] `verification-specialist` **PASS** · CI **verde** (run 28030732254). *(reality-checker N/A: change de infraestructura sin user journeys funcionales.)*
- [x] Deltas de `specs/` (auth-local, audit-retention) listos para `archive`.

## Notas de implementación (decisiones no documentadas previamente)

- **Toolchain JDK**: el JDK en PATH es 25 y `JAVA_HOME` apunta a 17, pero hay un **JDK 21 LTS instalado** (`C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot`). Maven se ejecuta con `JAVA_HOME` → JDK 21 para compilar a `release 21` y correr Spring Boot 3.3 sobre un runtime compatible. **No se tocó la versión de Spring Boot ni de Java.** El riesgo del JDK 25 que advertía el prompt **no se materializó** porque se usa el JDK 21 para Maven.
- **Numeración Flyway**: se usó la del `tasks.md` de este change (V1 spring-session, V2 audit/login, V3 índices), no la de `docs/data-model.md` §7 (que documenta el esquema consolidado objetivo, no la numeración real — ver `design.md` §Decisions). `data-model.md` es autoridad del **esquema**, este `tasks.md` lo es de la **numeración de ficheros**.
- **FKs a `employees` en `audit_log`/`login_log`**: omitidas en V2 porque `dbo.employees` la crea el change funcional de empleados (este es solo infraestructura). Las columnas `actor_employee_id`/`employee_id` quedan como `BIGINT NULL` sin constraint; la FK la añadirá el change funcional.
- **Esquema Spring Session V1**: copia verbatim de `schema-sqlserver.sql` del jar `spring-session-jdbc:3.3.5` (resuelto por el BOM de Spring Boot 3.3.5).
- **Testcontainers**: anclado a **1.21.4** (no 1.20.4) por compatibilidad con el Docker Engine 29.x instalado (docker-java 3.x más reciente). Contenedor SQL Server como **singleton** compartido para que la caché de contextos de Spring no apunte a un contenedor detenido.
