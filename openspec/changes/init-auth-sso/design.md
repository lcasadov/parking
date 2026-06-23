# Design: init-auth-sso

## Context
En Fase 2 parking se publica detrás de la landing corporativa ALEATICA. La
landing decide internamente cómo verificar al usuario (EntraID o credenciales
locales de la landing) y emite un `id_token` JWT. parking no conoce el método
de verificación: solo **autoriza** sobre su propia tabla `employees`. La sesión
sigue siendo server-side por cookie (`parking_SESSION`, Spring Session JDBC),
no por JWT propio, para permitir invalidación inmediata y el Single Logout.
Arquitectura hexagonal: el dominio (autorización, validación de claims) no
depende de Spring; el reloj es inyectable (`ClockPort`) para testear `exp` y la
rotación de 90 días.

## Goals
- Autorizar de forma segura y trazable a partir de un `id_token` externo tratado
  como entrada no confiable (fail closed).
- Mantener el rol bajo control interno (`Employee.role`), no del claim externo.
- Soportar Single Logout correlacionado por `client_sid`.
- Conservar el login local como capacidad latente de contingencia, minimizando su
  superficie (desactivado por defecto, rotación obligatoria).

## Decisions
- **Endpoints fuera de `/api/v1`**: `/ssocallback` y `/CloseSSOSessionID` se montan
  bajo `/parking-api` porque forman parte del contrato con la landing, no de la API
  funcional versionada. *Por qué:* alinearse con las rutas que la landing ya invoca.
- **Validación del JWT**: firma con la clave compartida con `SSOTTS` (por entorno),
  `iss == SSOTTS`, `aud == parking`, `exp` no vencido, `username` y `client_sid`
  presentes. Cualquier fallo → 403 (fail closed). *Por qué:* la entrada del SSO es
  no confiable (OWASP API10).
- **Rol interno autoritativo**: se toma de `Employee.role`; el claim `roles` del JWT
  se ignora. *Por qué:* confiar en un claim externo permitiría escalada si la landing
  se viera comprometida o mal configurada.
- **Sin provisioning automático**: si el `username` no existe en `employees` → 403
  `NO_ACCESS`. El alta es siempre manual por el admin.
- **Atributos de sesión**: al autorizar se guardan `employee_id`, `role`, `client_sid`,
  `slo_token`. *Por qué `client_sid`:* es el identificador que correlaciona la sesión de
  la landing con la de parking para el Single Logout.
- **Single Logout autenticado por `slo_token`** (token firmado), no por la cookie: la
  petición llega desde la landing, no desde el navegador del usuario.
- **Fallback de emergencia**: flag `parking.auth.local-fallback.enabled`, desactivado
  por defecto en todos los entornos; cualquier empleado con contraseña válida puede
  usarlo; rotación obligatoria a 90 días desde `last_password_change_at`. *Por qué:* es
  contingencia; minimizar superficie y forzar rotación reduce credenciales obsoletas.
- **Cookie `SameSite=Lax`** (no `Strict`): necesario para permitir el redirect top-level
  por GET de vuelta desde la landing. *Por qué:* `Strict` rompería el retorno del SSO.

## Risks
- **Clave de firma del `id_token` pendiente** (la provee ALEATICA por entorno) →
  bloquea la verificación real; mitigar con clave por config y tests con clave de prueba.
- **Aceptar un `id_token` mal validado** permitiría suplantación → mitigado con validación
  estricta y fail closed.
- **Escalada por claim `roles`** → mitigada ignorándolo y usando `Employee.role`.
- **Fallback abusado en producción** → mitigado con desactivación por defecto y rotación.
- **`consultaporlogin`** (WS del SSO) aún sin especificación de ALEATICA → fuera de alcance
  de este change; se marca como pendiente (`docs/security-design.md`).

## Migration Plan
- Sin nuevas tablas: reutiliza `employees`, `login_log` y `SPRING_SESSION` /
  `SPRING_SESSION_ATTRIBUTES` (ver `docs/data-model.md`).
- `login_log.result` ya admite `NO_ACCESS` y `FALLBACK_OK`; `login_log.phase` ya admite
  `PHASE_2` y `FALLBACK` (CHECK constraints existentes en `data-model.md`). Sin migración
  de datos.
- Empleados creados en Fase 2 nacen con `password_hash = NULL`; el login local se habilita
  caso a caso vía reset administrativo (`employees`). No se eliminan columnas de `employees`.
