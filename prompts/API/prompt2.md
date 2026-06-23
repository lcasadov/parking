=== INICIO DEL PROMPT 4 ===

# Misión

Genera `docs/openapi.yaml` — el contrato OpenAPI 3.1 canónico de parking.
Lo consumen `backend-architect`, `frontend-engineer`, `api-tester` y
`verification-specialist`.

# Entradas

- `README.md` adjunto (sección "API REST" original con todas las tablas
  de endpoints).
- `docs/PROJECT.md` adjunto.
- `docs/data-model.md` adjunto (para schemas de respuesta).
- `docs/security-design.md` adjunto (para esquemas de auth y RBAC).

# Contrato del documento

- Formato: **YAML válido, OpenAPI 3.1**.
- Debe poder cargarse en Swagger UI sin errores.
- Cada endpoint con `operationId` único en `camelCase`.
- Cada `operationId` debe coincidir con el método del controller en el
  código (convención: `<verbo><Recurso>`, ej. `crearSolicitud`,
  `aprobarSolicitud`).
- Cada endpoint con tags por módulo.
- Respuestas de error con el schema común.
- Validaciones de campos (`minLength`, `maxLength`, `pattern`, `enum`)
  coherentes con `data-model.md`.

# Estructura obligatoria

## info
```yaml
openapi: 3.1.0
info:
  title: parking API
  description: API REST de gestión de plazas de parking ALEATICA
  version: 1.0.0
  contact:
    name: Equipo parking
    email: _[pendiente]_
servers:
  - url: http://localhost:8080/parking-api/api/v1
    description: Desarrollo local
  - url: https://parking.aleatica.com/parking-api/api/v1
    description: Producción
```

## tags
Define un tag por módulo: `Auth`, `Empleados`, `Plazas`,
`AsignacionesFijas`, `Solicitudes`, `Liberaciones`, `Visitantes`,
`ReservasVisita`, `Calendario`, `Disponibilidad`, `Auditoria`,
`Exportaciones`.

## components.securitySchemes

```yaml
securitySchemes:
  sessionCookie:
    type: apiKey
    in: cookie
    name: parking_SESSION
```

Por defecto todos los endpoints requieren `sessionCookie` salvo los
marcados explícitamente como `security: []`.

## components.schemas

Define todos los schemas reutilizables. Mínimo:

- `Empleado`, `EmpleadoCrear`, `EmpleadoModificar`, `EmpleadoResetPasswordResp`
- `Plaza`, `PlazaCrear`, `PlazaConfigurarReq`
- `AsignacionFija`, `AsignacionFijaPutReq`
- `Solicitud`, `SolicitudCrearReq`, `SolicitudAprobarReq`, `SolicitudRechazarReq`
- `Liberacion`, `LiberacionCrearReq`, `LiberacionAdminReq`
- `Visitante`, `VisitanteCrearReq`
- `ReservaVisita`, `ReservaVisitaCrearReq`
- `DisponibilidadResp`, `CalendarioSemanalAdminResp`, `MiSemanaResp`
- `AuditLogEntry`, `LoginLogEntry`
- `Page<T>` genérico para paginación
- `ApiError` para errores
- Enums: `RolEmpleado`, `EstadoSolicitud`, `TipoLiberacion`, etc.

`ApiError` mínimo:
```yaml
ApiError:
  type: object
  required: [error, mensaje, timestamp]
  properties:
    error: { type: string, description: "Código corto" }
    mensaje: { type: string }
    campos:
      type: object
      additionalProperties: { type: string }
    timestamp:
      type: string
      format: date-time
```

## paths

Genera **todos** los endpoints listados en el README original (tabla
"API REST"). Para cada uno:

- `summary` corto.
- `description` con la regla de negocio aplicable (`RN-XX`) si procede.
- `parameters` con `in: path`, `in: query` validados.
- `requestBody` con schema referenciado.
- `responses` mínimas: 200/201/204, 400, 401, 403, 404, 409 (donde
  aplique), 500.
- `security` indicado si difiere del default.

**Endpoints mínimos a cubrir** (extrae detalle del README):

Auth:
- POST `/auth/login` (público)
- POST `/auth/logout`
- GET `/auth/me`
- POST `/auth/cambiar-password`
- GET `/ssocallback` (público, Fase 2)
- POST `/CloseSSOSessionID` (firmado, Fase 2)

Empleados:
- GET/POST/PUT/DELETE `/empleados`
- POST `/empleados/{id}/reactivar`
- POST `/empleados/{id}/reset-password`
- GET `/empleados/export`

Plazas:
- GET/POST/PUT `/plazas`
- POST `/plazas/configurar`

Asignaciones fijas:
- GET `/asignaciones-fijas`
- GET/PUT/DELETE `/asignaciones-fijas/empleado/{empleadoId}`

Liberaciones:
- GET `/liberaciones/mias`
- POST `/liberaciones`
- DELETE `/liberaciones/{id}`
- POST `/liberaciones/administrativas`

Solicitudes:
- GET `/solicitudes/mias`
- POST `/solicitudes`
- POST `/solicitudes/{id}/cancelar`
- GET `/solicitudes/pendientes`
- GET `/solicitudes/{id}`
- POST `/solicitudes/{id}/aprobar`
- POST `/solicitudes/{id}/rechazar`
- GET `/solicitudes/mias/export`
- GET `/solicitudes/export`

Visitantes:
- GET/POST/PUT `/visitantes`
- GET `/visitantes/{id}`

Reservas visita:
- GET/POST/DELETE `/reservas-visita`

Disponibilidad/Calendario:
- GET `/disponibilidad`
- GET `/calendario/admin`
- GET `/calendario/mi-semana`

Auditoría:
- GET `/audit`
- GET `/logins`
- GET `/audit/export`

# Restricciones de generación

- YAML válido y parseable. Si dudas, valida sintaxis antes de devolver.
- Todos los `$ref` deben resolver.
- Sin endpoints que no estén en el README.
- No inventes campos en schemas que no estén en `data-model.md`.
- Cada `operationId` único.
- Cada endpoint con al menos una respuesta de éxito.

=== FIN DEL PROMPT 4 ===