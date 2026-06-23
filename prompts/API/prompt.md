# Prompt 1

Rol: Eres un experimentado Arquitecto de Software Senior en el diseño de arquitectura de software.

Objetivo: Diseño y generar los contratos completos por módulo (request body, response schema,
HTTP status codes, validaciones). Esquema OpenAPI 3.0 en YAML/JSON que sirva como contrato Front-Back

Outputs a generar: actualiza el @readme.md genera la documentación relativa al punto 5 Especificación de la API e incluye los diagramas  necesarios en formato Mermaid

No inventes lo que no sepas. Si necesitas solucionar dudas, pregúntame antes de realizar nada.

# Respuesta Prompt 1
Antes de escribir una sola línea del contrato, necesito confirmar **10 decisiones de diseño** que afectan directamente al esquema. Las agrupo por módulo:

---

### Bloque 1 — Fundamentos transversales

**P1 — Versionado de la API**
¿Los endpoints llevan prefijo de versión?
- **Opción A** — `/api/reservas` (sin versión, como aparece en los diagramas actuales)
- **Opción B** — `/api/v1/reservas` (con versión, facilita evolución futura)
Respuesta Bloque 1 P1: Opción A, para versión tenemos el repositorio de código
**P2 — Formato de errores**
¿Qué estándar usamos para los errores de respuesta?
- **Opción A** — RFC 7807 Problem Details `{ "type", "title", "status", "detail", "instance" }`
- **Opción B** — Custom `{ "code", "message", "errors": [] }`
Respuesta Bloque 1 P2: Opción B
**P3 — Paginación de listas**
¿Qué formato devuelven los endpoints de listado?
- **Opción A** — Spring Page `{ "content": [], "totalElements", "totalPages", "number", "size" }`
- **Opción B** — Custom `{ "data": [], "total", "page", "pageSize" }`
Respuesta Bloque 1 P3: Opción B
---

### Bloque 2 — Módulo `usuarios`

**P4 — Registro de usuarios**
¿Cómo se dan de alta los jugadores?
- **Opción A** — Solo el administrador puede crear usuarios (panel admin)
- **Opción B** — Auto-registro público (`POST /api/auth/register`)
- **Opción C** — Ambos (admin crea + auto-registro con aprobación)
Respuesta Bloque 2 P4: Opción C
---
**P5 — Configuración Telegram del usuario**
¿Cómo vincula el usuario su Telegram ID?
- **Opción A** — El administrador lo rellena al crear/editar el usuario
- **Opción B** — El propio usuario lo configura desde su perfil (`PATCH /api/usuarios/me/telegram`)
- **Opción C** — Flujo automático: el bot detecta al usuario y lo vincula
Respuesta Bloque 2 P5: Opción C. Por el teléfono
---

### Bloque 3 — Módulo `reservas`

**P6 — Creación de reserva**
Al crear una reserva, ¿el creador queda automáticamente como participante?
- **Opción A** — Sí, se añade automáticamente como `participante[0]`
- **Opción B** — No, el body debe incluirlo explícitamente en la lista de participantes
Respuesta Bloque 3 P6: Opción A
**P7 — Participantes externos**
En el body de creación, ¿se pueden incluir personas sin cuenta en el sistema?
- **Opción A** — Solo usuarioId de usuarios registrados
- **Opción B** — También se admite `nombre` libre para externos (sin cuenta)
Respuesta Bloque 3 P7: Opción B
---

### Bloque 4 — Módulo `pagos`

**P8 — Respuesta de inicio de pago Redsys**
Cuando el usuario pulsa "Pagar online", ¿qué devuelve la API?
- **Opción A** — URL de redirección a la TPV de Redsys (el front redirige)
- **Opción B** — Formulario HTML generado por Redsys (el front lo renderiza y auto-envía)
- **Opción C** — Parámetros firmados HMAC que el front monta en un `<form>` y envía
Respuesta Bloque 4 P8: Opción A
**P9 — Confirmación OTP en flujo de reserva**
¿El OTP de confirmación de reserva se verifica por un endpoint REST o es exclusivamente por Telegram?
- **Opción A** — Solo Telegram (el bot recibe el código, no hay endpoint REST para esto)
- **Opción B** — También hay un endpoint `POST /api/otp/verificar` que usa el front web
Respuesta Bloque 4 P9: Opción B
---

### Bloque 5 — Módulo `bot`

**P10 — Webhook Telegram**
El endpoint `POST /api/bot/telegram` que recibe los updates de Telegram, ¿necesita documentarse en la spec OpenAPI pública (Swagger UI), o es un endpoint interno que solo Telegram conoce?
- **Opción A** — Se documenta en OpenAPI (visible en Swagger, aunque no lo usen los devs del front)
- **Opción B** — Se excluye de la spec pública (se marca `x-internal: true` o directamente se omite)
Respuesta Bloque 5 P10: Opción B
---

Con estas 10 respuestas genero el OpenAPI 3.0 completo en una sola pasada, sin necesidad de revisiones.

# Respuesta Prompt 1.1

Perfecto. Tengo todo lo que necesito. Voy a generar la sección completa. Es extensa, la escribo en una sola operación:


Leyó 2 archivos, ejecutó un agente, ejecutó un comando
Leyó 2 archivos, ejecutó un agente, ejecutó un comando

La sección está bien generada. Verifico también el YAML de OpenAPI:


Leyó 2 archivos, código buscado
Leyó 2 archivos, código buscado

La sección está completa. El documento pasa de 2.515 a 5.399 líneas. Resumen de lo generado:

✅ Sección 5 — Especificación de la API (2.884 líneas nuevas)
5.1 Convenciones Generales

Tabla de URL base y cabeceras (producción + desarrollo)
Envelope de error con JSON de ejemplo y tabla de code de aplicación
Formato de paginación (query params + response shape)
Tabla completa de HTTP status codes con descripción de uso
5.2 Mapa de Endpoints

Diagrama mindmap Mermaid con los 23 endpoints agrupados por módulo
5.3–5.7 Documentación por módulo

Módulo	Endpoints	Incluye
auth	6	login, register (pending approval), refresh, logout, solicitar-reset OTP, confirmar-reset
usuarios	8	perfil propio, CRUD admin, aprobación de auto-registro
reservas	8	disponibilidad, CRUD usuario, unirse, gestión admin
pagos	4	listado, iniciar Redsys (→ URL redirect), efectivo admin; webhook marcado como interno
otp	1	verificar código (REST + nota equivalente bot Telegram)
Cada endpoint documenta: request body JSON · response body JSON · tabla de status codes · validaciones.

5.8 Esquema OpenAPI 3.0.3 (YAML completo)

components/securitySchemes: BearerAuth JWT
components/schemas: 17 esquemas con $ref cruzadas
paths: todos los endpoints con requestBody, parameters, responses completos
Endpoints /api/auth/* marcados con security: [] (públicos)
Webhook Redsys excluido de la spec (solo mencionado en 5.6)