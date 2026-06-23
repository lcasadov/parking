Genera el documento `docs/PROJECT.md` para el proyecto **parking** (gestión
de plazas de parking ALEATICA). Este documento es la **fuente única de
verdad** que leerá un sistema multi-agente (orquestador + 14 agentes
especializados) antes de cualquier operación. Cualquier valor que falte o
sea ambiguo bloqueará al orquestador.

# Entradas

- `README.md` adjunto: descripción funcional completa del proyecto
  (glosario, reglas de negocio, autenticación Fase 1 y Fase 2, modelo
  conceptual, notificaciones, stack tecnológico, despliegue).
- Opcionalmente, los `.md` de los agentes — si están presentes, su lectura
  de variables (`grep VAR docs/PROJECT.md | cut -d'|' -f3 | xargs`) es la
  forma autoritativa de saber qué claves esperar y con qué formato.

# Contrato técnico de salida

El fichero `docs/PROJECT.md` DEBE cumplir estas reglas mecánicas o los
agentes fallarán al parsearlo:

1. **Formato de variables clave**: tabla Markdown con tres columnas
   `| Variable | Descripción | Valor |`. Los agentes hacen
   `grep VARIABLE docs/PROJECT.md | cut -d'|' -f3 | xargs`, así que el
   valor debe ir en la **tercera columna** sin pipes internos.
2. **Idioma**: español neutro (los agentes están preparados para ambos).
3. **Encabezados de nivel 2 (`##`)** para cada sección — no `#`, reservado
   para el título del documento.
4. **Sin información inventada**: si un valor no aparece en el README ni
   puede deducirse, escribe `_[pendiente — completar]_` y añade una nota
   en la sección "Pendientes" al final.

# Variables OBLIGATORIAS que deben aparecer en la tabla de variables clave

Estas son las claves que distintos agentes leen explícitamente. Tienen que
estar todas, una por fila, en una **única** tabla llamada
"Variables del proyecto":

| Variable | Para qué la usan los agentes | Valor sugerido si no hay info |
|---|---|---|
| `REPO_ROOT` | Path absoluto del repo en el equipo del orquestador | `_[pendiente]_` |
| `BASE_BRANCH` | Rama base de los PRs | `develop` |
| `GITHUB_REMOTE` | URL del remoto | `_[pendiente]_` |
| `GITHUB_ORG` | Organización GitHub | `_[pendiente]_` |
| `GITHUB_REPO` | Nombre del repo | `parking` |
| `GITHUB_PROJECT_NUMBER` | Número del Project v2 | `_[pendiente]_` |
| `ORCHESTRATOR_USER` | Cuenta GitHub bot para commits/PRs | `orquestadoria` |
| `ORCHESTRATOR_TOKEN_VAR` | Nombre de la env var con el token | `ORCHESTRATORIA_TOKEN` |
| `GIT_BOT_EMAIL` | Email para git commits del bot | `_[pendiente]_` |
| `BACKEND_DIR` | Carpeta del backend | `backend` |
| `FRONTEND_DIR` | Carpeta del frontend | `frontend` |
| `OPENSPEC_PATH` | Raíz de OpenSpec | `openspec/` |
| `OPENSPEC_API_PATH` | Ruta del OpenAPI canónico | `docs/openapi.yaml` |
| `PR_REVIEWER` | Reviewer obligatorio de PRs | `lcasadov` |
| `BACKEND_PORT` | Puerto local backend | `8080` |
| `FRONTEND_PORT` | Puerto local frontend | `5173` |
| `API_BASE_URL_LOCAL` | URL base API en DES local | `http://localhost:8080/parking-api/api/v1` |

# Secciones OBLIGATORIAS del documento

Genera estas secciones en este orden:

## 1. Identidad del proyecto

- Nombre completo, propietario (ALEATICA), responsable funcional
  (pendiente si el README no lo aporta), descripción ejecutiva de 3-5
  líneas extraída del README.

## 2. Variables del proyecto

La tabla descrita arriba. Una sola tabla.

## 3. Bot Orchestrator

- Sección dedicada con `ORCHESTRATOR_USER`, scopes que debe tener el token
  (`repo`, `project`, `workflow`), y ubicación del `.env`
  (`.claude/agents/.env`).

## 4. Stack tecnológico

Tabla por capa (backend / frontend / BD / mail / CI/CD / contenedores)
con componente, versión y propósito. Extrae todo del README, sección
"Stack Tecnológico".

## 5. Entornos

Tabla con `DES`, `PRE`, `PRO` y para cada uno: URL frontend, URL backend,
URL BD, URL SMTP, perfil Spring (`application-{perfil}.yml`). Si falta
información (URLs de PRE/PRO), márcalo `_[pendiente]_`.

## 6. Autenticación

- **Fase 1 (login local)**: resumen + endpoints + política de contraseña
  + bloqueo.
- **Fase 2 (SSO)**: resumen + reparto (landing autentica, parking autoriza)
  + algoritmo de `/ssocallback` + fallback de emergencia (config flag
  `parking.auth.local-fallback.enabled`, rotación 90 días).
- Tabla de mapeo de roles. Aclarar que el claim `roles` del JWT se ignora
  y el rol lo decide parking contra su BD.

## 7. Roles y RBAC

Matriz de permisos `ADMIN` vs `EMPLEADO` por área funcional (empleados,
plazas, asignaciones, solicitudes, liberaciones, visitantes, auditoría).
Una tabla con filas por endpoint o agrupación funcional y columnas por rol
con `✅` / `❌`.

## 8. Reglas críticas de negocio (RN-xx)

Numera las reglas de negocio del README como `RN-01` ... `RN-NN`. Estas
reglas son **contratos verificables**: cada una debe tener test dedicado
con cobertura 100 %. Mínimo a incluir (extraído del README):

- RN-01: Días válidos para solicitud (cualquier día del año).
- RN-02: Ventana de solicitud (hoy + 14 días naturales).
- RN-03: Unicidad de solicitud PENDIENTE por empleado-día.
- RN-04: Disponibilidad de plaza (las 4 condiciones).
- RN-05: Unicidad de asignación fija por plaza-día.
- RN-06: Unicidad de asignación fija por empleado-día.
- RN-07: Modificación de asignación fija no afecta a días pasados.
- RN-08: Motivo de rechazo obligatorio mínimo 5 caracteres.
- RN-09: Liberación voluntaria solo para fechas presentes/futuras y por
  el titular.
- RN-10: Liberación administrativa puede crearse por el admin para
  cualquier fecha presente/futura con motivo.
- RN-11: Multi-admin — notificación de nueva solicitud va a todos los
  admins activos.
- RN-12: Empleado solo ve su plaza y huecos libres; nombres ajenos
  ocultos.
- RN-13: Visitantes no acceden a la app; reserva la crea solo el admin
  y ocupa plaza igual que una solicitud APROBADA.
- RN-14: Login Fase 1 — bloqueo a 5 intentos fallidos durante 15 min.
- RN-15: Fase 2 — rotación obligatoria 90 días para cuentas con fallback.
- RN-16: Provisioning en SSO — usuario no existente devuelve 403, no se
  crea automáticamente.
- RN-17: Email AFTER_COMMIT — un fallo SMTP nunca revierte la operación
  funcional.

Lee el README y AÑADE las que falten. No quites ninguna.

## 9. Normativa aplicable

- RGPD: tratamiento de datos personales (nombre, apellidos, email,
  teléfono, matrícula, DNI de visitantes).
- Retención: 2 años para datos históricos; sin purga para entidades vivas.
- Auditoría completa de acciones y logs de login.

## 10. Estructura del repositorio

Extrae del README la sección "Estructura del Proyecto" si aún está, o
genera una estructura básica:
- `backend/` (Spring Boot WAR)
- `frontend/` (React + Vite)
- `database/` (seeds)
- `docs/`
- `openspec/`
- `docker-compose.yml`

## 11. Reglas de implementación

Reglas técnicas que los agentes deben respetar al implementar:
- Capas: `controller` → `service` → `repository`, sin saltos.
- DTOs como `record` en Java.
- Validación con `jakarta.validation` en DTOs.
- `@Transactional` en servicios, no en controladores.
- `@EntityGraph` o JOIN FETCH para evitar N+1 en queries con joins.
- Envelope de respuesta estándar (ver sección 13).
- Mappers explícitos (MapStruct) entre entity ↔ DTO.
- Auditoría vía AOP (`@Aspect`) sobre servicios anotados.
- Mensajes de error en español; nombres de variables/clases en español
  cuando el dominio lo requiera (Empleado, Plaza, Solicitud, etc.).

## 12. Módulos a implementar

Lista de módulos funcionales. Mínimo:
1. `auth-local` (Fase 1)
2. `auth-sso` (Fase 2)
3. `empleados`
4. `plazas`
5. `asignaciones-fijas`
6. `liberaciones`
7. `solicitudes`
8. `visitantes`
9. `disponibilidad-calendario`
10. `auditoria-retencion`
11. `notificaciones-email`
12. `exportaciones`

## 13. Contrato API

- Base path: `/api/v1`.
- Envelope de respuesta: `{ "data": ..., "meta": {...} }` para éxito;
  `{ "error": "código", "mensaje": "texto", "campos": {...} }` para error.
- Paginación: `?page=0&size=20` → `{ content, totalElements, totalPages, page, size }`.
- Fechas: ISO-8601, `YYYY-MM-DD` para días, timestamps con offset Z.
- Códigos HTTP: 200, 201, 204, 400, 401, 403, 404, 409, 422, 500.

## 14. Manejo de errores

Tabla con los principales casos:

| Caso | Código HTTP | Excepción dominio |
|---|---|---|
| Validación de entrada | 400 | `ValidationException` |
| Sin sesión | 401 | `UnauthorizedException` |
| Rol insuficiente | 403 | `ForbiddenException` |
| Recurso no encontrado | 404 | `NotFoundException` |
| Conflicto (ej. solicitud PENDIENTE duplicada) | 409 | `ConflictException` |
| Regla de negocio violada | 422 | `BusinessRuleException` |
| Error interno | 500 | `InternalException` (no expone stack) |

## 15. Tareas programadas

- **Purga retención 2 años**: `@Scheduled` diario sobre históricos.
- **Reintento de emails fallidos**: `@Scheduled` cada N minutos.
- Configurables vía `application.yml`.

## 16. Convenciones git y PR

- Ramas: `feature/<ID>-<slug>`, `task/<ID>-<slug>`, `bugfix/<ID>-<slug>`
  donde `<ID>` es el número del Issue de GitHub.
- Commits: Conventional Commits con scope (`feat(backend):`, `test(api):`).
- PR reviewer: leer de `PR_REVIEWER`.
- PRs siempre contra `BASE_BRANCH`.

## 17. Pendientes

Lista numerada de **toda** la información que no pudiste cerrar y dejaste
como `_[pendiente]_`. El orquestador usará esto para preguntar al humano
antes de empezar.

# Restricciones

- No inventes URLs, tokens, emails ni claves de firma.
- No incluyas información que NO esté en el README adjunto, salvo los
  valores por defecto explícitamente listados arriba.
- No reescribas el README — produces un documento NUEVO orientado a
  agentes.
- Mantén el documento por debajo de 800 líneas; si te quedas largo,
  consolida.
- No uses emojis dentro de los nombres de variables ni de los encabezados
  de tabla — los `grep` se rompen.

# Resultado esperado

Un único fichero Markdown `docs/PROJECT.md` listo para commitear, con:

- Tabla de variables parseable con `grep VAR docs/PROJECT.md | cut -d'|' -f3 | xargs`.
- 17 secciones numeradas exactamente como arriba.
- Una sección final "Pendientes" con todo lo no resuelto.
- Sin contradicciones internas con el README adjunto.

Devuelve el archivo completo, sin preámbulo conversacional. Si detectas
ambigüedades antes de generar, pregúntame solo lo crítico que bloquee la
generación; el resto márcalo como pendiente.

=== FIN DEL PROMPT ===