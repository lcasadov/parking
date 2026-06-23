=== INICIO DEL PROMPT 2 ===

# Misión

Genera `docs/data-model.md` — el modelo de datos canónico de parking
para SQL Server 2022. Lo consumirán `database-optimizer` y
`backend-architect`. Debe ser **ejecutable**: el DDL que produzcas tiene
que poder lanzarse con `sqlcmd` sin retoques.

# Entradas

- `README.md` adjunto (sección "Modelo de Datos" con diagrama ASCII y
  notas).
- `docs/PROJECT.md` adjunto (stack: SQL Server 2022, Spring Data JPA,
  Hibernate 6.5, Flyway 10).

# Contrato del documento

- Idioma: inglés para descripciones, inglés/snake_case para nombres SQL
  (convención SQL Server).
- DDL completo, válido para SQL Server 2022.
- Migraciones Flyway numeradas: `V1__...sql`, `V2__...sql`.
- No incluyas datos seed aquí — eso va en `database/seed/`.

# Secciones obligatorias

## 1. Convenciones
- Esquema único: `dbo`.
- Nombres de tabla en plural snake_case: `empleados`, `plazas`.
- Nombres de columna en snake_case.
- PKs: `id BIGINT IDENTITY(1,1) PRIMARY KEY`.
- FKs: `<tabla_singular>_id` con `FOREIGN KEY` explícita y nombre
  `FK_<tabla>_<tabla_destino>`.
- Booleanos: `BIT NOT NULL DEFAULT 0`.
- Timestamps: `DATETIME2(3)` con `DEFAULT SYSUTCDATETIME()`.
- Enums: `VARCHAR(N)` con `CHECK` constraint, NUNCA `VARCHAR` libre.
- Texto corto: `NVARCHAR(N)` (soporta unicode).

## 2. Diagrama Entidad-Relación
Reproduce el diagrama ASCII del README, completo. Si lo mejoras con
cardinalidades explícitas (1:N, N:1), mejor.

## 3. Tablas (una subsección por entidad)

Para cada tabla, incluye:
- **Propósito**: 1-2 líneas.
- **DDL completo**: `CREATE TABLE` con todas las columnas, FKs, CHECK
  constraints y filtered indexes.
- **Índices**: explícitos, con nombre `IX_<tabla>_<columnas>`.
- **Notas**: reglas especiales, gotchas, decisiones de diseño.

Entidades obligatorias (extrae del README):
1. `empleados`
2. `plazas`
3. `asignaciones_fijas`
4. `liberaciones`
5. `solicitudes`
6. `visitantes`
7. `reservas_visita`
8. `audit_log`
9. `login_log`
10. `SPRING_SESSION` y `SPRING_SESSION_ATTRIBUTES` (referencia al schema
    oficial de Spring Session JDBC para SQL Server — no lo redefinas,
    cita el path).

## 4. Restricciones críticas (filtered indexes)

SQL Server soporta unicidad parcial con filtered indexes. Incluye estos
literalmente:

```sql
-- Una plaza no puede estar asignada a dos empleados el mismo día de la semana
CREATE UNIQUE INDEX UX_asignaciones_fijas_plaza_dia_activa
ON asignaciones_fijas(plaza_id, dia_semana)
WHERE activa = 1;

-- Un empleado no puede tener dos asignaciones fijas el mismo día
CREATE UNIQUE INDEX UX_asignaciones_fijas_empleado_dia_activa
ON asignaciones_fijas(empleado_id, dia_semana)
WHERE activa = 1;

-- Un empleado solo puede tener una solicitud PENDIENTE por día
CREATE UNIQUE INDEX UX_solicitudes_empleado_fecha_pendiente
ON solicitudes(empleado_id, fecha_solicitada)
WHERE estado = 'PENDIENTE';

-- DNI único entre visitantes
CREATE UNIQUE INDEX UX_visitantes_dni ON visitantes(dni);

-- Login y email únicos en empleados
CREATE UNIQUE INDEX UX_empleados_login ON empleados(login);
CREATE UNIQUE INDEX UX_empleados_email ON empleados(email);
```

## 5. Índices de rendimiento

Cubre los queries más frecuentes:
- `IX_solicitudes_estado_fecha_creacion` (para listar pendientes FIFO).
- `IX_solicitudes_empleado_id_fecha_solicitada` (para "mis solicitudes").
- `IX_asignaciones_fijas_empleado_id_activa`.
- `IX_liberaciones_plaza_id_fecha`.
- `IX_reservas_visita_plaza_id_fecha`.
- `IX_audit_log_timestamp`.
- `IX_login_log_timestamp`.

Justifica cada índice con el query que sirve.

## 6. Enums (CHECK constraints)

Tabla con los enums del sistema:

| Tabla | Columna | Valores permitidos |
|---|---|---|
| empleados | rol | `ADMIN`, `EMPLEADO` |
| empleados | origen_auth | `LOCAL`, `ENTRA_ID` |
| solicitudes | estado | `PENDIENTE`, `APROBADA`, `RECHAZADA`, `CANCELADA` |
| liberaciones | tipo | `VOLUNTARIA`, `ADMINISTRATIVA` |
| login_log | resultado | `OK`, `CRED_INVALIDAS`, `BLOQUEADO`, `INACTIVO`, `SIN_ACCESO`, `FALLBACK_OK` |
| login_log | fase | `FASE_1`, `FASE_2`, `FALLBACK` |

Implementa cada uno con `CHECK (columna IN ('VAL1', 'VAL2', ...))`.

## 7. Migraciones Flyway

Propón la secuencia de migraciones:

- `V1__schema_inicial.sql` — todas las tablas vivas + filtered indexes
  + CHECK constraints.
- `V2__spring_session_schema.sql` — copia literal del schema de Spring
  Session para SQL Server.
- `V3__indices_rendimiento.sql` — todos los índices de la sección 5.
- `V4__seed_admin_inicial.sql` — un único admin de bootstrap para Fase 1
  (login `admin`, password `_[pendiente — generar BCrypt]_`).

## 8. Mapeo a entidades JPA

Para cada tabla, indica el nombre esperado de la clase Java:

| Tabla | Entidad JPA | Repositorio |
|---|---|---|
| empleados | `Empleado` | `EmpleadoRepository` |
| plazas | `Plaza` | `PlazaRepository` |
| ... | ... | ... |

## 9. Política de retención

Documenta el job de purga:
- Tabla afectada → criterio de purga → frecuencia.
- Para `audit_log`, `login_log`, `solicitudes` cerradas, `liberaciones`,
  `reservas_visita` con `fecha < DATEADD(year, -2, GETUTCDATE())`.
- Entidades vivas (`empleados`, `plazas`, `asignaciones_fijas` activas,
  `visitantes`) NO se purgan.
- Implementación con `DELETE TOP (1000)` en bucle para evitar bloqueos.

## 10. Pendientes
Lista numerada de información que dejaste sin completar.

# Restricciones de generación

- DDL ejecutable: si lo pego en `sqlcmd`, debe funcionar sin retoques.
- No mezcles esquemas con datos seed.
- No uses `IDENTITY` con saltos raros (1,1 siempre).
- Nombres de constraints e índices SIEMPRE explícitos, nunca anónimos.

=== FIN DEL PROMPT 2 ===

=== INICIO DEL PROMPT 3 ===

# Misión

Genera `docs/security-design.md` — el documento autoritativo de seguridad
de parking. Lo consume principalmente `security-auditor`, y también
`backend-architect` para implementación de filtros, RBAC y JWT.

# Entradas

- `README.md` adjunto (secciones de Autenticación Fase 1 y Fase 2, y
  Reglas de Negocio).
- `docs/PROJECT.md` adjunto (variables de proyecto, RN-xx enumeradas).

# Contrato del documento

- Idioma: español.
- Cada decisión debe llevar **justificación** (no solo "se usa X" sino
  "se usa X porque Y").
- Si una política no aplica en Fase 1 pero sí en Fase 2 (o viceversa),
  márcalo con 🟢 Fase 1 / 🔵 Fase 2.
- Sin código de implementación; sí pseudocódigo o algoritmos paso a paso.

# Secciones obligatorias

## 1. Principios de seguridad
- Defensa en profundidad: validación en cliente + servidor + BD.
- Mínimo privilegio: ningún endpoint sin anotación de rol.
- No exposición de datos sensibles: ni en logs, ni en respuestas de
  error, ni en stack traces.
- Trazabilidad completa: cada acción auditable.
- Fail closed: en duda, denegar.

## 2. Modelo de autenticación

### 🟢 Fase 1 — Login local
- Mecanismo: `POST /api/v1/auth/login` con `{login, password}`.
- Hash: **BCrypt coste 12** (justifica el coste).
- Cookie de sesión: `parking_SESSION`, `HttpOnly`, `Secure`, `SameSite=Lax`.
- Persistencia: Spring Session JDBC sobre SQL Server.
- Bloqueo: 5 intentos fallidos consecutivos → 15 min de bloqueo
  (`locked_until`).
- Política de contraseña:
  - ≥10 caracteres.
  - Al menos 1 mayúscula, 1 minúscula, 1 dígito, 1 símbolo.
  - Distinta de login y email.
  - Sin caducidad obligatoria en Fase 1.
- Reset administrativo: el admin lo dispara, se muestra en pantalla,
  el empleado debe cambiarla en el primer login.

### 🔵 Fase 2 — SSO ALEATICA
- Reparto: la landing **autentica**, parking **autoriza**.
- Endpoint: `GET /ssocallback?id_token=...&client_id=parking&redirect_uri=...`
- Validación del JWT:
  - Firma (clave compartida con SSOTTS, por entorno).
  - `iss == "SSOTTS"`.
  - `aud == "parking"`.
  - `exp` no vencido.
  - `username` y `client_sid` presentes.
- Autorización:
  - Buscar `Empleado` por `username`.
  - Si no existe → 403 + `LOGIN_LOG.resultado=SIN_ACCESO`.
  - Si existe pero `activo=false` → 403 + `INACTIVO`.
  - Si OK → tomar rol de `Empleado.rol`, NO del claim `roles` del JWT.
- Single Logout: `POST /CloseSSOSessionID` con `{slo_token, client_sid}`.
- Fallback de emergencia: flag `parking.auth.local-fallback.enabled`.
  - Desactivado por defecto.
  - Activable por config (requiere recarga).
  - Cualquier empleado con `password_hash` no nulo puede usarlo.
  - Rotación obligatoria cada 90 días.

## 3. Matriz RBAC

Tabla detallada con todas las áreas funcionales y los dos roles. Usa
✅/❌. Cubre como mínimo:

| Área | Acción | ADMIN | EMPLEADO |
|---|---|---|---|
| Empleados | Listar | ✅ | ❌ |
| Empleados | Ver propio perfil | ✅ | ✅ (solo el suyo) |
| Empleados | Crear/Modificar/Borrar | ✅ | ❌ |
| Empleados | Reset password | ✅ | ❌ |
| Plazas | Listar | ✅ | ❌ (solo ve huecos libres) |
| Plazas | CRUD | ✅ | ❌ |
| Asignaciones fijas | Listar todas | ✅ | ❌ |
| Asignaciones fijas | Ver propias | ✅ | ✅ |
| Asignaciones fijas | CRUD | ✅ | ❌ |
| Solicitudes | Crear propia | ❌ | ✅ |
| Solicitudes | Listar propias | ❌ | ✅ |
| Solicitudes | Listar todas | ✅ | ❌ |
| Solicitudes | Aprobar/Rechazar | ✅ | ❌ |
| Solicitudes | Cancelar propia | ❌ | ✅ |
| Liberaciones | Liberar propia | ❌ | ✅ |
| Liberaciones | Liberación administrativa | ✅ | ❌ |
| Visitantes | CRUD | ✅ | ❌ |
| Reservas visita | CRUD | ✅ | ❌ |
| Auditoría | Consultar | ✅ | ❌ |
| Calendario admin | Ver | ✅ | ❌ |
| Mi semana | Ver | ✅ | ✅ |
| Exportar mis solicitudes | — | ✅ | ✅ |
| Exportar histórico completo | — | ✅ | ❌ |

Implementación esperada: `@PreAuthorize("hasRole('ADMIN')")` o
`@PreAuthorize("hasAnyRole('ADMIN','EMPLEADO')")` en cada endpoint.

## 4. Validación de entradas

- Toda DTO validado con `jakarta.validation`: `@NotNull`, `@Size`,
  `@Pattern`, `@Email`.
- Validación adicional de reglas de negocio en servicios
  (`BusinessRuleException` con código de error específico).
- Sanitización: nunca concatenar input en SQL (todo vía JPA/parámetros).
- Para campos de búsqueda (`?q=...`): escape de wildcards SQL Server
  (`%` y `_`).

## 5. Protección contra OWASP API Top 10

| Riesgo | Mitigación en parking |
|---|---|
| API1 Broken Object Level Authz | Endpoints verifican que el recurso pertenece al usuario (ej. `solicitud.empleado_id == sesion.empleado_id`) |
| API2 Broken Authentication | BCrypt coste 12, bloqueo por intentos, Spring Session con TTL |
| API3 Broken Property Level Authz | DTOs nunca incluyen `password_hash`, `failed_attempts`, etc. |
| API4 Unrestricted Resource Consumption | Paginación obligatoria, rate limiting (sección 7) |
| API5 Broken Function Level Authz | RBAC en cada endpoint con `@PreAuthorize` |
| API6 Server-Side Request Forgery | No aplica (no se hacen requests salientes salvo SMTP/SSO WS) |
| API7 Security Misconfiguration | Actuator restringido a `health,info`; no exponer stack en errores |
| API8 Lack of Inventory Mgmt | OpenAPI siempre sincronizado con código |
| API9 Improper Inventory Mgmt | Mismo punto |
| API10 Unsafe Consumption of APIs | Validación estricta del SSO WS (Fase 2) |

## 6. CORS y cookies

- `Access-Control-Allow-Origin`: lista exacta por entorno (sin `*`).
- `Access-Control-Allow-Credentials: true`.
- En DES: `http://localhost:5173` + `http://localhost:8080`.
- En PRE/PRO: dominios oficiales (`_[pendiente]_`).
- Cookie de sesión:
Set-Cookie: parking_SESSION=...;
HttpOnly;
Secure;
SameSite=Lax;
Path=/parking-api;
Max-Age=3600
## 7. Rate limiting

- `POST /auth/login`: 10 intentos / 15 min / IP.
- `POST /solicitudes`: 30 / hora / empleado.
- Endpoints de exportación: 5 / minuto / usuario.
- Implementación: Bucket4j o equivalente con persistencia en memoria
  (Fase 1) o Redis (futuro si se distribuye).

## 8. Auditoría

- `AUDIT_LOG`: rellenado vía `@Aspect` Spring AOP sobre servicios
  anotados con `@Auditable`.
- Campos: `actor_id`, `actor_login`, `accion`, `entidad`, `entidad_id`,
  `payload_antes`, `payload_despues`, `ip`, `user_agent`, `timestamp`.
- `LOGIN_LOG`: rellenado en el filtro de autenticación.
- Retención: 2 años; purga automática diaria.

## 9. Secretos y gestión de claves

| Secreto | Dónde vive | Cómo se inyecta |
|---|---|---|
| Conexión SQL Server | `application-{env}.yml` cifrado / env var | Spring Boot |
| BCrypt salt | Generado por BCrypt | — |
| Clave firma JWT SSO (Fase 2) | Vault corporativo / env var | `application-{env}.yml` |
| SMTP credenciales | Ethereal en DES; vault en PRE/PRO | `application-{env}.yml` |
| `ORCHESTRATORIA_TOKEN` (bot GitHub) | `.claude/agents/.env` | Solo lectura local |

- Nunca commitear secretos.
- `.gitignore` debe excluir `.env`, `application-local.yml`.

## 10. Logging seguro

- Nunca loguear: contraseñas, hashes, tokens JWT completos, cookies.
- Tokens en logs: truncar a primeros 8 caracteres + `...`.
- Datos personales en logs solo si es estrictamente necesario para
  trazar incidente; preferir IDs.
- Stack traces: solo en logs, nunca en respuesta HTTP.

## 11. Plan de respuesta a incidentes

- Detección: alertas si `LOGIN_LOG` muestra > 50 fallos en 5 min
  desde la misma IP.
- Aislamiento: bloqueo manual del empleado desde el panel admin.
- Forense: `AUDIT_LOG` + `LOGIN_LOG` permiten reconstruir el incidente.
- Notificación: a `_[pendiente — DPO o responsable seguridad]_` en caso
  de brecha de datos personales (RGPD: 72 horas).

## 12. Cumplimiento RGPD

- Base legal: interés legítimo del empleador.
- Datos tratados: ver sección 13.
- Derechos del interesado:
  - Acceso: exportar mis datos vía `/empleados/me/export`.
  - Rectificación: modificar perfil propio (campos limitados).
  - Supresión: solicitud al admin; baja lógica del empleado tras
    cumplir retención.
- Retención: 2 años para datos históricos; entidades vivas mientras el
  empleado esté activo.

## 13. Datos personales tratados

| Dato | Origen | Propósito | Visibilidad |
|---|---|---|---|
| Nombre, apellidos | Alta admin / SSO | Identificación | Empleado + admins |
| Email | Alta admin / SSO | Notificaciones | Empleado + admins |
| Teléfono | Alta admin | Contacto contingencia | Solo admins |
| Matrícula | Alta admin | Verificar plaza ocupada | Solo admins |
| Departamento | Alta admin / SSO | Organización | Empleado + admins |
| DNI (visitante) | Alta admin | Control acceso físico | Solo admins |

## 14. Pendientes
Lista numerada de información que dejaste sin completar.

# Restricciones de generación

- No inventes flujos de seguridad; cíñete al README + RN-xx de PROJECT.md.
- No prometas mitigaciones que no se puedan implementar con el stack.
- Si una mitigación requiere infra extra (Redis, Vault), márcalo claro
  como "futuro" o "pendiente".

=== FIN DEL PROMPT 3 ===