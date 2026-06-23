# Design: bootstrap-mvp

## Context
Arranque del backend hexagonal (Java 21 LTS + Spring Boot 3.3, WAR sobre
Tomcat 10.1, SQL Server 2022). El objetivo es un esqueleto **compilable,
arrancable y verificable** sobre el que las capabilities construyan
lógica en TDD, sin reabrir decisiones de configuración.

## Goals
Desde una máquina limpia:
1. `docker compose up -d` levanta **SQL Server** (y MailHog opcional).
2. `mvn clean install` compila sin errores ni warnings.
3. `mvn spring-boot:run -Dspring-boot.run.profiles=des` arranca en `http://localhost:8080/parking-api`.
4. `GET /parking-api/api/v1/health` → 200 `{"status":"UP"}`.
5. Flyway aplica las migraciones y crea `SPRING_SESSION`, `SPRING_SESSION_ATTRIBUTES`, `audit_log`, `login_log` (vacías).
6. CI (`mvn verify`) en verde.

## Decisions
- **WAR sobre Tomcat 10.1 externo** (no JAR embebido): estándar de despliegue corporativo. `parkingApplication extends SpringBootServletInitializer`.
- **Java 21 LTS** (no 22): soporte a largo plazo.
- **Spring Session JDBC** (no Redis): no hay infra Redis en ALEATICA; suficiente para <500 usuarios. Cookie `parking_SESSION` (`HttpOnly`, `Secure` solo en prod, `SameSite=Lax`, `Path=/parking-api`, TTL 60 min).
- **Flyway** (no Liquibase): SQL puro, curva menor.
- **Migraciones incrementales**: este change crea SOLO infra (`V1` spring session, `V2` audit/login, `V3` índices). Cada capability añade sus propias `V*` en su change. `docs/data-model.md` documenta el **esquema objetivo consolidado**, no la numeración real de los ficheros `V*`.
- **Testcontainers (SQL Server)** en integración (no H2): fidelidad con producción (los *filtered indexes* son específicos de SQL Server).
- **MapStruct** (no ModelMapper): compile-time, sin reflection.
- **Docker solo para BD** (no la app): iteración rápida del backend. **SMTP = Ethereal hosted** (`smtp.ethereal.email`) vía `.env` (`SMTP_*`); **NO se dockeriza** (MailHog opcional solo si se quiere UI de correo local).
- **Error uniforme** `ApiError { error, message, fields, timestamp }` vía `@ControllerAdvice`.
- **`PasswordEncoder` BCrypt (coste 12)** como bean desde el arranque (lo usará `auth-local`).
- Seguridad mínima: `/api/v1/health` y `/api/v1/auth/**` públicos (auth devuelve 501), resto `authenticated()`. Sin `UserDetailsService` real todavía.

## Risks
- TLS del contenedor SQL Server / trust store en local → documentar en README de arranque.
- Migración incremental vs esquema consolidado de `data-model.md` → mitigado declarando `data-model.md` como objetivo y cada change como dueño de sus `V*`.

## Migration Plan
- `V1__spring_session_schema.sql` — copia **verbatim** del schema oficial de Spring Session JDBC para SQL Server (`SPRING_SESSION`, `SPRING_SESSION_ATTRIBUTES`).
- `V2__audit_and_login_log.sql` — tablas `audit_log` y `login_log` (sin las funcionales).
- `V3__infra_indexes.sql` — índices `IX_audit_log_occurred_at`, `IX_login_log_occurred_at`.
