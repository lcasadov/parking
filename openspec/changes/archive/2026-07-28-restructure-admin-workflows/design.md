## Context

La app está en construcción, sin usuarios en producción, lo que da libertad para cambios estructurales (fusionar secciones, cambiar rutas, añadir endpoints) sin migrar hábitos ni datos. El diagnóstico procede del consenso de cuatro análisis funcionales independientes: la administración se construyó pantalla-por-endpoint (13 destinos admin, varios de solo lectura y varios duplicados), y falta la operación más básica del admin: dar un recurso a un empleado para una fecha concreta.

Restricciones del proyecto: prosa de negocio en español, identificadores de código en inglés (README "Nomenclatura"); backend Spring Boot 3.3 / Java 21 / WAR sobre Tomcat, frontend React 18 + Vite con design system propio (sin Tailwind), SQL Server 2022 con Flyway como dueño del esquema; Quality Gate obligatorio (cobertura ≥80% líneas / ≥75% branches, 0 violations Sonar nuevas). Este change corrige **estructura y funcionalidad**; el rediseño estético es una fase posterior separada.

## Goals / Non-Goals

**Goals:**
- Reagrupar la navegación por tarea: ADMIN 13→9, EMPLOYEE 5→4 con "Mi Semana" como índice.
- Hacer accionables las vistas de ocupación/disponibilidad (asignar/liberar inline).
- Añadir la asignación puntual del admin (una fecha concreta) reutilizando `Request`.
- Completar la generalización PARKING/DESK en calendario semanal y "Mi Semana".
- Ampliar y documentar el rol AGENCIA.
- Corregir los tres defectos de comportamiento detectados.

**Non-Goals:**
- Rediseño visual, sistema de diseño, animaciones (fase UI posterior).
- Puestos para visitantes (decisión de negocio pendiente; requeriría generalizar `visitor_reservations`).
- Modo de aprobación por tipo de recurso, solicitudes por rango/recurrentes, lista de espera, dashboard de inicio (candidatos a changes futuros).
- Cambios de esquema de base de datos (la asignación puntual reutiliza `Request`).

## Decisions

**D1. La asignación puntual reutiliza la entidad `Request`, no una entidad nueva.**
Se crea un endpoint admin (p. ej. `POST /requests/admin`) que produce una `Request` naciendo `APPROVED` con `resolved_by_id` = admin. Alternativa descartada: una entidad `DirectAssignment` separada — duplicaría la lógica de disponibilidad, auto-asignación, calendario, exportación y auditoría que `Request` ya tiene. Reutilizar `Request` hace que la asignación aparezca "gratis" en "Mi Semana", calendario admin y exports.

**D2. "Ocupación" = fusión de calendario semanal + disponibilidad, con celdas accionables.**
La disponibilidad deja de ser una ruta; pasa a ser el estado `FREE` (ya filtrable) de la propia rejilla. Asignar desde una celda usa dos caminos según la intención: **fija** por día de la semana → `PUT /fixed-assignments/employee/{id}` (ya existe); **puntual** por fecha concreta → endpoint nuevo de D1. Alternativa descartada: mantener "Disponibilidad" como pantalla y solo añadirle un botón "Asignar" — deja dos vistas del mismo dato.

**D3. Fusiones de secciones = agrupación de UI, no cambio de contrato.**
"Recursos" (Plazas|Puestos), "Registros" (Auditoría|Accesos) y "Mis plazas" (Fijas|Liberaciones) son pestañas que montan los componentes de página ya existentes. Cero cambios de backend. "Liberar" unifica los dos pivotes sobre los endpoints de liberación ya compartidos.

**D4. Generalización PARKING/DESK por extensión de los DTO existentes.**
`GET /calendar/admin` gana el parámetro `resourceType` (default PARKING para retrocompatibilidad); `MyWeekDay` se extiende para llevar el estado de ambos recursos por día. Se prefiere extender los contratos actuales antes que crear endpoints paralelos por tipo.

**D5. AGENCIA se amplía por reutilización de superficies existentes.**
Al unificar "Liberar" con ambos pivotes (D3), dar a AGENCIA ese mismo destino resuelve su carencia principal (hoy solo tiene el pivote por-empleado). Se le añade una vista de ocupación de solo lectura y el historial de sus propias liberaciones. Se mantiene fail-closed en los endpoints del portal de empleado.

**D6. Los tres defectos se tratan dentro de este change.**
(a) Liberación de recurso fijo enviando `resourceType` correcto y mostrando la etiqueta real — verificar en `MyFixedAssignmentsPage`/`ReleaseResourceModal`. (b) En modo MANUAL, el modal indica que el puesto elegido es "preferencia" (el contrato ya lo ignora por diseño; el arreglo es de UX, no de API). (c) Confirmación antes de solicitar desde el plano.

## Risks / Trade-offs

- **Alcance amplio en un solo change** → Mitigación: `tasks.md` se ordena en fases (quick wins solo-UI primero; endpoint de asignación puntual después; generalización DESK al final), de modo que se pueda entregar y validar por partes sin romper la rama.
- **Cambiar rutas rompe enlaces internos/tests E2E** → Mitigación: como no hay usuarios, se actualizan rutas y selectores de test en el mismo change; se mantiene redirección de rutas viejas a las nuevas donde sea barato.
- **`PUT /fixed-assignments` tiene semántica de reemplazo del conjunto de días** → Mitigación: la asignación inline precarga el estado actual (`GET /fixed-assignments/employee/{id}`) y reenvía el conjunto completo con el día añadido/quitado, para no borrar días existentes.
- **El endpoint de asignación puntual podría heredar la ventana de 14 días del autoservicio** → decisión de negocio (ver Open Questions); por defecto se implementa sin heredar la restricción y se documenta.
- **Regresión en la generalización DESK del calendario** → Mitigación: `resourceType` con default PARKING mantiene el comportamiento actual; los tests cubren ambos tipos.

## Migration Plan

1. Sin migración de datos (no hay cambios de esquema ni usuarios).
2. Orden de entrega recomendado: (F1) fusiones de UI + toggle disponibilidad + correcciones de los tres defectos (solo frontend); (F2) endpoint de asignación puntual + celdas accionables de Ocupación; (F3) generalización DESK del calendario y "Mi Semana"; (F4) ampliación de AGENCIA.
3. Rollback: al ser feature de una rama, revertir el merge; sin estado persistente que deshacer.
4. Actualizar `docs/openapi.yaml`, `docs/ux-flows.md`, `docs/ui-screens.md` y `README.md` (rol AGENCIA) como parte del change.

## Open Questions — RESUELTAS (decisión de negocio, 2026-07-23)

- **Asignación puntual del admin — email:** ✅ **Sí, con plantilla propia** (distinta de "solicitud aprobada"). El empleado recibe un aviso "un administrador te ha asignado [recurso] para [fecha]". Envío resiliente vía `email_outbox`.
- **Asignación puntual del admin — ventana temporal:** ✅ **Sin límite de días.** El admin puede asignar cualquier fecha futura; solo se rechazan fechas pasadas (ya implementado). No hereda la ventana hoy+14 del autoservicio.
- **Rol AGENCIA — asignación puntual:** ✅ **Solo ADMIN.** AGENCIA sigue siendo mínimo privilegio (libera y consulta ocupación, no asigna). Fail-closed ya verificado con IT.
- **Modo de aprobación:** sigue siendo global (fuera de alcance). La asignación puntual del admin es independiente de él: siempre nace `APPROVED`.
