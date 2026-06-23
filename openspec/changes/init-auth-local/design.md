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
- Flyway: tablas `employees`, `login_log` y `SPRING_SESSION`/`SPRING_SESSION_ATTRIBUTES` (ver `docs/data-model.md`).
- Sin migración de datos (capability nueva).
