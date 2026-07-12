# security-design.md — Diseño de seguridad de parking

> **Documento autoritativo de seguridad.** Lo consume principalmente `security-auditor`; también `backend-architect` para implementar filtros, RBAC y validación JWT.
> **Idioma:** español. **Identificadores de código** (entidades, tablas, enums, campos, roles) en **inglés**, según la *Nomenclatura del código* del `README.md`. Donde el enunciado original usaba términos en español (`Empleado`, `EMPLEADO`, `SIN_ACCESO`, `AUDIT_LOG`…), aquí aparecen sus equivalentes en inglés (`Employee`, `EMPLOYEE`, `NO_ACCESS`, `audit_log`…).
> **Autoridad de esquema:** `docs/data-model.md`. Cuando un campo citado en el prompt no existe como columna propia, se indica cómo se materializa en el esquema real.
> **Convención de fases:** 🟢 = aplica en Fase 1 (login local) · 🔵 = aplica en Fase 2 (SSO ALEATICA).

---

## Tabla de contenidos

1. Principios de seguridad
2. Modelo de autenticación
3. Matriz RBAC
4. Validación de entradas
5. Protección OWASP API Top 10 (2023)
6. CORS, CSRF y cookies
7. Rate limiting
8. Auditoría
9. Secretos y gestión de claves
10. Logging seguro
11. Plan de respuesta a incidentes
12. Cumplimiento RGPD
13. Datos personales tratados
14. Pendientes

---

## 1. Principios de seguridad

| Principio | Qué significa en parking | Por qué |
|-----------|--------------------------|---------|
| **Defensa en profundidad** | Validación en cliente (UX) + servidor (`jakarta.validation` + reglas de negocio) + base de datos (constraints, *filtered indexes*). | El cliente es manipulable; la validación de servidor y BD es la que realmente protege. La BD es la última línea ante condiciones de carrera (concurrencia en aprobación). |
| **Mínimo privilegio** | Ningún endpoint sin anotación de rol; cada operación expone solo lo imprescindible. | Un endpoint sin `@PreAuthorize` es accesible por defecto: fallar en abrir es peor que fallar en cerrar. |
| **No exposición de datos sensibles** | Ni en logs, ni en respuestas de error, ni en stack traces. Los DTO de salida nunca llevan `password_hash`, `failed_login_attempts`, `locked_until`. | Reduce la superficie de fuga de credenciales y de datos personales (RGPD). |
| **Trazabilidad completa** | Toda acción funcional sensible en `audit_log`; todo intento de login en `login_log`. | Permite reconstruir incidentes y responde a la obligación de rendición de cuentas del RGPD. |
| **Fail closed** | En caso de duda (token inválido, rol indeterminado, recurso ajeno), **denegar**. | La denegación segura evita accesos no previstos; un error nunca debe traducirse en acceso concedido. |

---

## 2. Modelo de autenticación

### 🟢 Fase 1 — Login local

- **Mecanismo:** `POST /api/v1/auth/login` con cuerpo `{ login, password }`. *Por qué credenciales propias:* en Fase 1 no existe aún la integración con la landing; parking necesita autenticar de forma autónoma.
- **Hash de contraseña: BCrypt coste 12.** *Por qué BCrypt:* es un hash adaptativo con *salt* por contraseña, resistente a tablas precalculadas. *Por qué coste 12:* fija ~2^12 iteraciones (≈200-300 ms por verificación en hardware actual), suficiente para frustrar fuerza bruta offline sin degradar la UX de un login interactivo. Un coste menor (≤10) es demasiado barato de atacar; uno mayor (≥14) penaliza la latencia sin ganancia proporcional.
- **Cookie de sesión `parking_SESSION`** con `HttpOnly`, `Secure`, `SameSite=Lax` (detalle en sección 6). *Por qué cookie de sesión y no token en JS:* `HttpOnly` la hace inaccesible a XSS, a diferencia de un token guardado en `localStorage`.
- **Persistencia: Spring Session JDBC sobre SQL Server** (`SPRING_SESSION`). *Por qué en BD:* permite invalidación inmediata desde el servidor (logout, bloqueo) y, en Fase 2, el Single Logout localizando la sesión por `client_sid`. Misma infraestructura en ambas fases.
- **Bloqueo de cuenta:** 5 intentos fallidos consecutivos → 15 minutos de bloqueo, almacenado en `Employee.locked_until` y `Employee.failed_login_attempts`. *Por qué 5/15:* corta el barrido de contraseñas sin habilitar un DoS trivial de bloqueo permanente; tras la ventana, la cuenta se reactiva sola.
- **Política de contraseña:**
  - ≥ 10 caracteres.
  - Al menos 1 mayúscula, 1 minúscula, 1 dígito y 1 símbolo.
  - Distinta del `login` y del `email`.
  - **Sin caducidad obligatoria en Fase 1** (la rotación de 90 días solo aplica en Fase 2 a cuentas con fallback). *Por qué sin caducidad:* la rotación forzada frecuente empuja a contraseñas débiles y predecibles; se prefiere longitud y unicidad.
- **Reset administrativo:** lo dispara el admin; el sistema genera una contraseña aleatoria segura, marca `password_must_change = true` y **la muestra en pantalla una sola vez** (entrega por canal directo, no por email en Fase 1). El empleado debe cambiarla en el primer login. *Por qué `password_must_change`:* evita que una credencial conocida por el admin persista como válida.

**Algoritmo de login (paso a paso) — 🟢 Fase 1:**

```
1. Recibir {login, password}.
2. Buscar Employee por login.
   - Si no existe → registrar login_log(result=INVALID_CREDENTIALS, phase=PHASE_1) → 401.
     (No revelar si el login existe: mismo mensaje genérico.)
3. Si Employee.locked_until > ahora →
     login_log(result=LOCKED, phase=PHASE_1) → 401 "cuenta temporalmente bloqueada".
4. Si Employee.active = false o enabled = false →
     login_log(result=INACTIVE, phase=PHASE_1) → 401 genérico.
5. Verificar BCrypt(password, Employee.password_hash).
   - Falla → failed_login_attempts++ ; si llega a 5 → locked_until = ahora + 15 min ;
            login_log(result=INVALID_CREDENTIALS o LOCKED, phase=PHASE_1) → 401.
   - OK   → failed_login_attempts = 0 ; locked_until = NULL.
6. Crear sesión (Spring Session) y emitir cookie parking_SESSION.
7. login_log(result=OK, phase=PHASE_1).
8. Si password_must_change = true → respuesta indica que debe cambiarla antes de operar.
```

### 🔵 Fase 2 — SSO ALEATICA

- **Reparto de responsabilidades:** la **landing autentica**, parking **autoriza**. *Por qué:* parking no debe conocer el método de verificación (EntraID o credenciales locales de la landing); solo decide acceso y rol sobre su propia tabla `employees`. **No hay provisioning automático.**
- **Endpoint:** `GET /ssocallback?id_token=...&client_id=parking&redirect_uri=...`.
- **Validación del `id_token` (JWT):**
  - Firma válida con la clave compartida con SSOTTS (por entorno).
  - `iss == "SSOTTS"`.
  - `aud == "parking"`.
  - `exp` no vencido.
  - Claims `username` y `client_sid` presentes.
  *Por qué validar `iss`/`aud`/`exp` además de la firma:* la firma garantiza integridad, pero sin comprobar emisor/audiencia/expiración un token legítimo de otro servicio o caducado podría reutilizarse.
- **Autorización:**
  - Buscar `Employee` por `login = username`.
  - Si **no existe** → `403` + `login_log(result=NO_ACCESS, phase=PHASE_2)`.
  - Si existe pero `active = false` → `403` + `login_log(result=INACTIVE, phase=PHASE_2)`.
  - Si OK → tomar el rol de `Employee.role` (`ADMIN`/`EMPLOYEE`), **NO del claim `roles` del JWT**. *Por qué ignorar el claim `roles`:* la autoridad sobre el rol en parking es interna; confiar en un claim externo permitiría escalada si la landing se viera comprometida o mal configurada.
- **Single Logout:** `POST /CloseSSOSessionID` con `{ slo_token, client_sid }`. El backend valida el `slo_token`, localiza la sesión en `SPRING_SESSION` por `client_sid` y la invalida (200, o 404 si no existe). *Por qué por `client_sid`:* es el identificador que correlaciona la sesión de la landing con la de parking.
- **Fallback de emergencia:** flag `parking.auth.local-fallback.enabled`.
  - **Desactivado por defecto** en todos los entornos.
  - Activable por configuración (requiere recarga/reinicio del WAR).
  - Cualquier empleado con `password_hash` **no nulo** puede usarlo (no solo admins).
  - **Rotación obligatoria cada 90 días** (`Employee.last_password_change_at`): si han pasado > 90 días, se obliga a cambiarla antes de continuar.
  - Cada uso se registra con `login_log(phase=FALLBACK, result=FALLBACK_OK | …)`.
  *Por qué desactivado por defecto y con rotación:* es una capacidad de contingencia; minimizar su superficie y forzar rotación reduce el riesgo de credenciales locales obsoletas en producción.

**Algoritmo de `/ssocallback` (paso a paso) — 🔵 Fase 2:**

```
1. Extraer id_token de la query.
2. Validar firma con la clave del entorno. Inválida → 403 (fail closed).
3. Validar iss==SSOTTS, aud==parking, exp no vencido, username y client_sid presentes.
   Cualquier fallo → 403.
4. Buscar Employee por login = username.
   - No existe → login_log(NO_ACCESS, PHASE_2) → 403.
   - active=false → login_log(INACTIVE, PHASE_2) → 403.
5. role = Employee.role  (ignorar claim roles del JWT).
6. Crear sesión con atributos {employee_id, role, client_sid, slo_token}; emitir parking_SESSION.
7. 302 → redirect_uri.
```

---

## 3. Matriz RBAC

Roles: `ADMIN` y `EMPLOYEE` (`Role`). El rol procede **siempre** de `Employee.role`. Implementación esperada: cada endpoint anotado con `@PreAuthorize("hasRole('ADMIN')")` o `@PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")`. *Por qué anotación explícita en cada endpoint:* materializa el principio de mínimo privilegio y hace auditable la autorización endpoint a endpoint.

| Área | Acción | ADMIN | EMPLOYEE |
|------|--------|:-----:|:--------:|
| Empleados | Listar | ✅ | ❌ |
| Empleados | Ver propio perfil | ✅ | ✅ (solo el suyo) |
| Empleados | Crear / Modificar / Baja lógica | ✅ | ❌ |
| Empleados | Reset de contraseña | ✅ | ❌ |
| Empleados | Cambiar la propia contraseña | ✅ | ✅ |
| Plazas (`ParkingSpace`) | Listar | ✅ | ❌ (solo ve huecos libres vía disponibilidad) |
| Plazas | CRUD / configuración masiva | ✅ | ❌ |
| Puestos (`Desk`) | CRUD + editor de plano | ✅ | ❌ |
| Puestos | Ver plano y estado por fecha | ✅ | ✅ |
| Asignaciones fijas (`FixedAssignment`) | Listar todas | ✅ | ❌ |
| Asignaciones fijas | Ver propias | ✅ | ✅ |
| Asignaciones fijas | CRUD / revocar | ✅ | ❌ |
| Solicitudes (`Request`) | Crear propia | ❌ | ✅ |
| Solicitudes | Listar propias | ✅ | ✅ (solo las suyas) |
| Solicitudes | Listar todas | ✅ | ❌ |
| Solicitudes | Aprobar / Rechazar | ✅ | ❌ |
| Solicitudes | Cancelar propia (en `PENDING`) | ❌ | ✅ |
| Liberaciones (`Release`) | Liberar la propia | ❌ | ✅ |
| Liberaciones | Liberación administrativa | ✅ | ❌ |
| Visitantes (`Visitor`) | CRUD | ✅ | ❌ |
| Reservas de visita (`VisitorReservation`) | CRUD | ✅ | ❌ |
| Auditoría (`audit_log`, `login_log`) | Consultar | ✅ | ❌ |
| Calendario semanal admin | Ver | ✅ | ❌ |
| "Mi Semana" | Ver | ✅ | ✅ |
| Exportar mis solicitudes | — | ✅ | ✅ |
| Exportar histórico completo | — | ✅ | ❌ |

> **Nota sobre "Ver propio perfil" y "Listar propias":** el rol `EMPLOYEE` accede al endpoint, pero la pertenencia se verifica además a nivel de objeto (sección 5, API1): `request.employee_id == session.employee_id`. El RBAC concede la *función*; la comprobación de objeto concede el *dato concreto*.

---

## 4. Validación de entradas

- **Toda DTO de entrada validada con `jakarta.validation`:** `@NotNull`, `@Size`, `@Pattern`, `@Email`, etc. *Por qué declarativa:* la validación en el borde (controller) rechaza pronto y de forma uniforme las entradas malformadas.
- **Reglas de negocio en la capa de dominio/servicio:** ventana de 14 días, unicidad de solicitud `PENDING`, disponibilidad, longitud mínima de `rejection_reason` (≥5), etc. Se señalan con una excepción de negocio (`BusinessRuleException`) que porta un **código de error específico**. *Por qué separadas de la validación sintáctica:* dependen del estado del sistema (BD), no del formato del dato.
- **Sanitización SQL:** nunca se concatena entrada en SQL; todo va por JPA con **parámetros vinculados**. *Por qué:* elimina por construcción la inyección SQL (OWASP). 
- **Campos de búsqueda (`?q=...`):** escape de los comodines de `LIKE` de SQL Server (`%`, `_`, `[`) usando cláusula `ESCAPE`. *Por qué:* sin escaparlos, un `%` del usuario altera la semántica de la búsqueda y puede provocar escaneos costosos.

**Pseudocódigo — escape de comodines `LIKE` (SQL Server):**

```
patrón_seguro = q.replace("[", "[[]")
                 .replace("%", "[%]")
                 .replace("_", "[_]")
consulta:  WHERE campo LIKE :patrón_seguro + '%'   -- prefijo controlado por el servidor
```

---

## 5. Protección OWASP API Top 10 (2023)

> Se usa la numeración **oficial OWASP API Security Top 10 — 2023** (el enunciado traía algunas etiquetas desplazadas; se corrigen para que `security-auditor` pueda mapearlas).

| Riesgo (2023) | Mitigación en parking |
|---------------|------------------------|
| **API1 — Broken Object Level Authorization (BOLA)** | Cada endpoint sobre un recurso propio verifica pertenencia: `request.employee_id == session.employee_id`, asignación/liberación propias, etc. No basta el rol. |
| **API2 — Broken Authentication** | BCrypt coste 12, bloqueo por intentos (`locked_until`), sesión Spring Session con TTL e invalidación server-side; 🔵 validación estricta del JWT (firma + `iss`/`aud`/`exp`). |
| **API3 — Broken Object Property Level Authorization** | Los DTO de salida **nunca** exponen `password_hash`, `failed_login_attempts`, `locked_until`, `slo_token`. Los DTO de entrada ignoran propiedades no editables (sin *mass assignment*: el rol nunca se setea desde el cuerpo). |
| **API4 — Unrestricted Resource Consumption** | Paginación obligatoria en todos los listados; límites de tamaño de payload. Rate limiting **parcial** (sección 7): implementado solo en exportación; login y solicitudes quedan **pendientes** en Fase 1. |
| **API5 — Broken Function Level Authorization** | RBAC con `@PreAuthorize` en cada endpoint (sección 3); fail closed por defecto. |
| **API6 — Unrestricted Access to Sensitive Business Flows** | Flujos sensibles (creación masiva de solicitudes, aprobación, reset de contraseña) protegidos por RBAC; la unicidad de `PENDING` por empleado/fecha frena el abuso. El rate limiting sobre estos flujos está **pendiente** en Fase 1 (solo exportación lo aplica; ver sección 7). |
| **API7 — Server-Side Request Forgery (SSRF)** | Superficie mínima: parking solo realiza peticiones salientes a SMTP y, 🔵, al WS SSO (`consultaporlogin`), ambos con destino fijo por configuración; no se construyen URLs de salida desde entrada de usuario. |
| **API8 — Security Misconfiguration** | Actuator restringido a `health,info` y protegido; sin stack traces ni detalles internos en respuestas de error; cabeceras de seguridad por defecto de Spring Security. **CSRF desactivado deliberadamente** (API JSON sin formularios + cookie `SameSite=Lax`; ver sección 6). CORS de backend **no cableado** en Fase 1: la propiedad `parking.cors.allowed-origins` existe pero ningún bean la consume; la protección cross-origin en desarrollo recae en el **proxy de Vite** (ver sección 6). |
| **API9 — Improper Inventory Management** | Contrato OpenAPI (SpringDoc) mantenido sincronizado con el código; versionado de API (`/api/v1`); entornos DES/PRE/PRO claramente separados. |
| **API10 — Unsafe Consumption of APIs** | 🔵 Validación estricta de todo lo recibido del SSO/WS de ALEATICA (firma, claims, formato); se trata como entrada no confiable. |

---

## 6. CORS, CSRF y cookies

**CSRF — desactivado deliberadamente.** `SecurityConfig` desactiva CSRF (`csrf(AbstractHttpConfigurer::disable)`). *Por qué es seguro desactivarlo aquí:* la API es **JSON sin formularios** (no hay envío de formularios HTML clásicos susceptibles del ataque CSRF tradicional) y la cookie de sesión usa **`SameSite=Lax`**, que ya bloquea el envío de la cookie en peticiones cross-site de terceros. La combinación *API JSON + `SameSite=Lax`* cubre el vector CSRF sin necesidad del token sincronizador de Spring Security. *(No confundir con «cabeceras de seguridad por defecto»: la protección CSRF de Spring está explícitamente apagada; la mitigación proviene del atributo de cookie, no del filtro CSRF.)*

**CORS — config actualmente huérfana (pendiente de cablear en Fase 1).** La propiedad `parking.cors.allowed-origins` existe en `application-des.yml` (`http://localhost:5173,http://localhost:8080`), pero **ningún bean la consume**: no hay `CorsConfigurationSource` ni `.cors(...)` en `SecurityConfig`. Por tanto, **el backend no aplica CORS por sí mismo** hoy.
- **En desarrollo (DES):** la protección cross-origin recae en el **proxy del dev server de Vite**, que sirve la SPA (`:5173`) y reenvía las llamadas de API al backend bajo el mismo origen aparente, evitando la petición *cross-origin* real desde el navegador.
- **En PRE/PRO:** la SPA y la API se sirven desde el **mismo origen** por el propio Tomcat, así que no hay petición *cross-origin* y CORS no es necesario.
- **Diseño objetivo (cuando se cablee el CORS de backend):** `Access-Control-Allow-Origin` como **lista exacta, nunca `*`** (con credenciales, `*` está prohibido por el navegador), consumiendo `parking.cors.allowed-origins`, y `Access-Control-Allow-Credentials: true` solo en DES. **Marcado como pendiente** (sección 14): hoy la propiedad está declarada pero no conectada a ningún `CorsConfigurationSource`.

**Cookie de sesión** (serializada por `SessionConfig#cookieSerializer`):

```
Set-Cookie: parking_SESSION=<id>;
            HttpOnly;
            [Secure]        ← condicional por entorno (ver abajo)
            SameSite=Lax;
            Path=/parking-api;
            Max-Age=3600
```

Justificación de cada flag:
- **`HttpOnly`** — inaccesible a JavaScript: un XSS no puede robar la sesión. Siempre activo.
- **`Secure`** — **condicional por entorno**, controlado por `parking.session.cookie.secure` (`useSecureCookie`): en **DES** vale `false` (la app corre sobre HTTP plano, marcar `Secure` impediría enviar la cookie); solo en **PRO** (perfil `pro`) vale `true`, exigiendo HTTPS para que la cookie viaje. *Por qué condicional:* una cookie `Secure` no se envía por HTTP, lo que rompería el desarrollo local; en producción sobre HTTPS sí evita la captura en claro.
- **`SameSite=Lax`** — bloquea el envío en peticiones cross-site de terceros (anti-CSRF), pero **permite** la navegación top-level por GET, necesaria para el retorno desde la landing en el flujo SSO (🔵). *Por qué Lax y no Strict:* `Strict` rompería el redirect de vuelta del SSO. Este atributo es la base de la mitigación CSRF (ver arriba).
- **`Path=/parking-api`** — acota el envío de la cookie a la API.
- **`Max-Age=3600`** — TTL de sesión de 1 hora; reduce la ventana de uso de una sesión robada. La expiración server-side la gobierna además Spring Session (JDBC, `SessionConfig.SESSION_TTL_SECONDS = 3600`).

---

## 7. Rate limiting

> **Estado de implementación (Fase 1):** de las tres medidas de esta sección, **solo el rate limiting de exportación está implementado** (`ExportRateLimiter`, 5/min por usuario). Las dos primeras (login y solicitudes) son **diseño objetivo pero NO están implementadas** en Fase 1 — ver la columna «Estado».

| Endpoint | Límite | Clave | Estado |
|----------|--------|-------|--------|
| `POST /auth/login` 🟢 | 10 intentos / 15 min | por IP | ⛔ **PENDIENTE — no implementado en Fase 1** |
| `POST /requests` | 30 / hora | por empleado | ⛔ **PENDIENTE — no implementado en Fase 1** |
| Endpoints de exportación | 5 / minuto | por usuario | ✅ Implementado (`ExportRateLimiter`) |

- *Por qué por IP en login y por empleado en el resto:* el login no tiene aún identidad fiable (la clave natural es la IP); el resto opera bajo sesión autenticada (la clave natural es el empleado).
- **Rate limiting de login — PENDIENTE:** el límite 10/15 min por IP **no está implementado** en Fase 1. La única defensa anti-fuerza-bruta activa hoy es el **bloqueo por cuenta** (5 intentos / 15 min, en `AuthService`; ver sección 2), que sí está implementado. Como consecuencia, la **mitigación anti-fuerza-bruta distribuida** (barrido de muchas cuentas desde una misma IP, o desde muchas IP) **queda abierta**: el bloqueo por cuenta frena el ataque contra *una* cuenta concreta, pero no limita el volumen de intentos por origen. Cablear el rate limiting de login por IP es requisito para cerrar este hueco.
- **Rate limiting de `POST /requests` — PENDIENTE:** el límite 30/hora por empleado **no está implementado**; el abuso de creación de solicitudes se contiene hoy únicamente por RBAC y por la **unicidad de `PENDING`** por empleado/fecha (regla de negocio), no por un limitador de tasa.
- **Implementación prevista (cuando se aborde):** **Bucket4j (o equivalente) con almacenamiento en memoria** en Fase 1 (un único WAR). *Implementable con el stack actual, sin infraestructura extra.* Hoy solo existe este patrón en el módulo `export/` (`ExportRateLimiter`).
- **Futuro / pendiente:** si parking se despliega en **varias instancias**, el contador en memoria deja de ser global → se requeriría **Redis** (u otro backend compartido). Marcado como **futuro** (sección 14), no se asume ahora.

---

## 8. Auditoría

- **`audit_log`** — se rellena vía **`@Aspect` Spring AOP** sobre los casos de uso anotados con `@Auditable`. *Por qué AOP:* centraliza la auditoría fuera de la lógica de negocio, garantizando cobertura uniforme sin ensuciar el dominio.
- **Campos conceptuales auditados:** actor (`actor_employee_id` + su `login` resuelto), `action`, entidad (`entity_type`, `entity_id`), estado antes/después, IP, user-agent, momento.
  - **Mapeo al esquema real (`data-model.md`):** `audit_log` tiene `actor_employee_id`, `action`, `entity_type`, `entity_id`, `details (NVARCHAR(MAX))`, `occurred_at`. Los atributos enriquecidos —`actor_login`, `ip`, `user_agent` y los *payload* `antes`/`después`— se serializan como **JSON dentro de `details`**, no como columnas propias. *Por qué:* mantiene el esquema estable y flexible sin proliferar columnas; `security-auditor` debe buscar esos valores dentro de `details`.
- **`login_log`** — se rellena en el **filtro de autenticación**; columnas: `login_attempted`, `employee_id`, `result`, `phase`, `ip_address`, `user_agent`, `occurred_at`. Cubre 🟢 Fase 1, 🔵 Fase 2 y fallback. Separado de `audit_log` para no contaminar la auditoría funcional con ruido de autenticación.
- **Retención:** 2 años; purga automática diaria por lotes (`parking.retention.years`). Ver `data-model.md` §9.

---

## 9. Secretos y gestión de claves

| Secreto | Dónde vive | Cómo se inyecta |
|---------|------------|-----------------|
| Conexión SQL Server | Variable de entorno / `application-{env}.yml` (valor cifrado o referenciado, no en claro) | Spring Boot (`spring.datasource.*`) |
| *Salt* de BCrypt | Generado por BCrypt e **incluido en el propio hash** | — (no se gestiona aparte) |
| Clave de firma del JWT SSO 🔵 | Vault corporativo *(futuro)* / variable de entorno por entorno | `application-{env}.yml` (referencia) |
| Credenciales SMTP | **Ethereal** en LOCAL/DES/PRE (en `.env` local, excluido de git); **SMTP corporativo** solo en PRO | Variables de entorno (`SMTP_*`) → `application-{env}.yml` (referencia, nunca el valor) |
| Token de GitHub de `lcasadov` | Keyring de `gh` (no en el repo) | Lo usa `gh` para Issues/PRs/Projects; **no** es un secreto en tiempo de ejecución de la app |

- **Nunca commitear secretos.** `.gitignore` debe excluir `.env` y `application-local.yml`.
- *Por qué env var / yml referenciado en vez de hardcode:* desacopla el secreto del artefacto desplegable (el mismo WAR sirve a todos los entornos).
- **Vault corporativo:** marcado como **futuro** — la línea base implementable hoy es inyección por variable de entorno; el almacén centralizado se adopta cuando exista infraestructura corporativa para ello.

---

## 10. Logging seguro

- **Nunca loguear:** contraseñas, hashes (`password_hash`), tokens JWT completos, `slo_token`, ni la cookie de sesión.
- **Tokens en logs:** si es imprescindible referenciar uno, **truncar** a los primeros 8 caracteres + `…`. *Por qué:* permite correlacionar sin reconstruir el secreto.
- **Datos personales en logs:** solo si es estrictamente necesario para trazar un incidente; **preferir IDs** (`employee_id`) a nombre/email. *Por qué:* minimización RGPD.
- **Stack traces:** solo en los logs del servidor, **nunca** en la respuesta HTTP (un manejador global devuelve un error genérico con código, sin detalle interno). *Por qué:* el stack trace revela estructura interna explotable y puede contener datos.

---

## 11. Plan de respuesta a incidentes

- **Detección:** alerta cuando `login_log` muestre **> 50 fallos en 5 minutos desde la misma IP** (consulta programada sobre `result` ≠ `OK` agrupado por `ip_address`/ventana). *Implementable con el stack:* es una consulta sobre `login_log`; la alerta externa (correo/monitor) puede ser un job programado.
- **Aislamiento:** bloqueo manual del empleado desde el panel admin (baja lógica `active=false` y/o `locked_until`).
- **Forense:** `audit_log` + `login_log` permiten reconstruir quién hizo qué, desde dónde y cuándo.
- **Notificación:** ante brecha de datos personales, comunicar a `_[pendiente — DPO o responsable de seguridad]_` dentro de las **72 horas** que exige el RGPD.

---

## 12. Cumplimiento RGPD

- **Base legal:** interés legítimo del empleador en gestionar un recurso corporativo escaso.
- **Datos tratados:** ver sección 13.
- **Derechos del interesado:**
  - **Acceso:** exportar los propios datos vía `/employees/me/export`.
  - **Rectificación:** el empleado modifica su perfil propio (campos limitados; el rol y el estado no son autoeditables).
  - **Supresión:** se solicita al admin; baja lógica del empleado y eliminación tras cumplir la retención.
- **Retención:** 2 años para datos históricos; las entidades vivas se conservan mientras el empleado esté activo. *Por qué baja lógica y no borrado inmediato:* preserva la integridad referencial del histórico auditado durante el periodo de retención.

---

## 13. Datos personales tratados

| Dato | Origen | Propósito | Visibilidad |
|------|--------|-----------|-------------|
| Nombre, apellidos (`first_name`, `last_name`) | Alta admin / 🔵 SSO | Identificación | Empleado + admins |
| Email (`email`) | Alta admin / 🔵 SSO | Notificaciones | Empleado + admins |
| Teléfono móvil (`mobile_phone`) | Alta admin | Contacto de contingencia | Solo admins |
| Matrícula (`license_plate`) | Alta admin | Verificar plaza ocupada | Solo admins |
| Departamento (`department`) | Alta admin / 🔵 SSO | Organización | Empleado + admins |
| Documento de identidad de visitante (`national_id`) | Alta admin | Control de acceso físico | Solo admins |

> El empleado **no** ve datos personales de otros empleados (la vista "Mi Semana" no muestra nombres ajenos). Los datos marcados "Solo admins" no se exponen a `EMPLOYEE` en ningún DTO.

---

## 14. Decisiones fijadas y pendientes

**Decisiones fijadas** (antes pendientes):
- **CORS**: no aplica en PRE/PRO (SPA y API en el mismo origen, servidas por Tomcat); en DES la protección cross-origin recae en el **proxy de Vite** (`:5173`). El CORS de backend queda pendiente de cablear (ver más abajo).
- **CSRF**: desactivado deliberadamente en `SecurityConfig` (API JSON + cookie `SameSite=Lax`; ver sección 6). Decisión cerrada, no pendiente.
- **Validación JWT (Fase 2)**: librería **jjwt 0.12.x** (`io.jsonwebtoken`).

**Pendientes de implementación (Fase 1)** — diseñados en este documento pero **no presentes en el código**:
1. **Rate limiting de `POST /auth/login`** (10/15 min por IP): no implementado. Único freno actual = bloqueo por cuenta (5/15 min, `AuthService`). La mitigación anti-fuerza-bruta **distribuida** queda abierta (sección 7).
2. **Rate limiting de `POST /requests`** (30/hora por empleado): no implementado; hoy solo lo contiene RBAC + unicidad de `PENDING` (sección 7).
3. **Cableado del CORS de backend:** `parking.cors.allowed-origins` está declarada pero ningún bean (`CorsConfigurationSource` / `.cors(...)`) la consume; en Fase 1 la protección cross-origin la aporta el proxy de Vite (sección 6).

**Pendientes** (externos / por confirmar):
4. **Clave/secreto de firma del JWT** del SSO (Fase 2) y su gestión por entorno — lo provee ALEATICA.
5. **DPO o responsable de seguridad** destinatario de la notificación de brecha (sección 11).
6. **Vault corporativo** para secretos en PRE/PRO: confirmar disponibilidad; hasta entonces, env vars. *(Futuro.)*
7. **Redis (o backend compartido)** para rate limiting si parking se despliega en varias instancias. *(Futuro.)*
8. **Especificación del WS `consultaporlogin`** del SSO (Fase 2), para validar su consumo (API10).
9. **Credenciales SMTP corporativas** de **PRO** (LOCAL/DES/PRE usan Ethereal, ya configurado en `.env`).
