# Design: init-auth-local

## Context
Fase 1 gestiona credenciales propias. La autenticación es por sesión
server-side (cookie), no por JWT propio, para permitir invalidación
inmediata y el Single Logout de Fase 2. Arquitectura hexagonal: el
dominio (`AuthorizeUseCase`, `PasswordPolicy`) no depende de Spring.

## Goals
- Login local seguro y trazable.
- Bloqueo por fuerza bruta sin habilitar DoS de bloqueo permanente.
- Política de contraseña fuerte sin caducidad obligatoria en Fase 1.

## Decisions
- **Hash**: BCrypt coste 12 (≈200-300 ms/verificación; frena fuerza bruta offline sin penalizar UX). `PasswordEncoder` como bean.
- **Sesión**: Spring Session JDBC sobre SQL Server; cookie `parking_SESSION` (`HttpOnly`, `Secure`, `SameSite=Lax`, `Path=/parking-api`, TTL 60 min).
- **Bloqueo**: `failed_login_attempts` + `locked_until` en `employees`; 5 intentos → 15 min; reinicio a 0 tras login OK.
- **Mensajes**: respuesta genérica en fallo (no revelar existencia de `login`).
- **Trazabilidad**: cada intento en `login_log` (`result`, `phase`).
- **Reloj inyectable** (`ClockPort`) para testear ventana de bloqueo y rotación.

## Risks
- Enumeración de usuarios → mitigada con mensajes genéricos y mismo tiempo de respuesta.
- Bloqueo abusivo de cuentas ajenas → mitigado con ventana temporal (no permanente).

## Migration Plan
- **`employees` es la única tabla nueva** que aporta este change: se crea en
  `V4__employees.sql` con el esquema completo de `docs/data-model.md` §3.1
  (credenciales, bloqueo, rotación), más las FKs pendientes desde `audit_log`
  y `login_log` hacia `employees`.
- **Ya existen** (creadas por `bootstrap-mvp`): `login_log` y `audit_log`
  (`V2__audit_and_login_log.sql`), `SPRING_SESSION`/`SPRING_SESSION_ATTRIBUTES`
  (`V1__spring_session_schema.sql`) y los índices de infraestructura (`V3`).
  Este change **no las recrea**.
- **Seed admin de desarrollo** idempotente en `V5__seed_dev_admin.sql`: un único
  `ADMIN` local con BCrypt(coste 12) para poder probar login real en LOCAL/DES y
  en los tests e2e. Credenciales documentadas en `proposal.md` §"Seed admin de
  desarrollo". No es el admin de PRO.
- **Aislamiento del seed por perfil** (bug #11, CWE-798): el seed vive en
  `classpath:db/seed/dev` — fuera de `db/migration`, porque Flyway escanea las
  localizaciones de forma **recursiva** y un subdirectorio de `db/migration`
  seguiría ejecutándose en todos los entornos. `spring.flyway.locations`:
  - base (`application.yml`): `classpath:db/migration` (solo esquema, default seguro);
  - `des` (y tests, que corren con el perfil default `des`): añade `classpath:db/seed/dev`;
  - `pre`/`pro`: fijan **explícitamente** `classpath:db/migration` para que un
    cambio futuro del default no re-filtre el seed.
  Se conserva el número `V5` con contenido byte-idéntico (mismo checksum: las BD
  de DES ya migradas validan sin reparación; PRE/PRO simplemente nunca resuelven
  V5, lo cual Flyway acepta al no existir versiones posteriores). Guard de
  regresión: `FlywayLocationsByProfileTest`.
- Sin migración de datos (capability nueva).

## Decisión de contrato (capturada en este change)
- En `docs/openapi.yaml`, el schema `CurrentUser` lleva `passwordMustChange`
  en `required` (junto a `employeeId`, `login`, `role`). `/auth/login` y
  `/auth/me` devuelven siempre los cuatro campos: el frontend depende de que
  `passwordMustChange` venga presente para decidir si fuerza el cambio.
