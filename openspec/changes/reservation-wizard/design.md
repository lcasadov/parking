## Context

Documento **as-built**: reconstruye, tras la implementación, el diseño del asistente de reserva multipaso para el ADMIN, ya mergeado en `develop`. Fuente: commits `97c2366`, `fec0a3f`, `39e46f4`, `a6cf2c9`, `4bdc6f4`, `300cc10`, `9f1acc2`, `de493c1`, `865c219`, `5fbc9c4`, `726d83c`, `57b48cd`, `5bb2785` (24 jul 2026, rama `feat/fullstack/rediseno-completo`) y el código actual de `frontend/src/components/wizard/*`. No hubo proposal/design previos — se documenta el resultado, no una intención previa a implementar.

El asistente reutiliza en su totalidad la capability backend `admin-punctual-assignment` (`POST /requests/admin`, `restructure-admin-workflows`, ya `APPROVED` con `resolved_by_id` = admin, auto-asignación por categoría/planta, email de aviso). Este change añade la vista previa de esa auto-asignación (`GET /requests/admin/suggested-space`) y el flujo de UI que la consume.

## Goals / Non-Goals

**Goals:**
- Dar al ADMIN un camino de UI para reservar un recurso a un empleado en una o varias fechas, en un único flujo guiado.
- Mostrar antes de confirmar qué recurso exacto le tocaría a la auto-asignación por categoría.
- Permitir recurso distinto por día cuando las fechas elegidas no son homogéneas.
- Reutilizar el plano existente como selector de puesto, con navegación (zoom, minimapa) que no dependa del arrastre del lienzo (para no interferir con la selección).
- Evitar cierres accidentales del asistente (flujo largo, varios pasos) por pulsar Escape.

**Non-Goals (de este change; ver otros changes):**
- Reserva de un visitante externo desde el asistente (`beneficiaryType = VISITOR`) — capability `visitor-reservations`, posterior.
- Cambios de contrato en `POST /requests/admin` (ya existente y sin modificar).
- Rediseño de la vista de Ocupación/"Ver en plano" más allá de lo que el asistente reutiliza directamente (el fix del pulso continuo y la columna `#id` de esa rejilla no se documentan en esta capability).

## Decisions

**D1. El asistente es un modal (`Dialog fullScreen`), no una ruta.**
Se abre desde `TopbarReserve` (CTA del topbar) y vive como estado local (`ReservationWizard`), no como página con URL propia. Alternativa descartada: ruta `/admin/reservations/new` — el flujo es transitorio (crea y termina), y el modal a pantalla completa da todo el alto disponible sin la sobrecarga de una ruta y su navegación de vuelta.

**D2. La confirmación crea un `Request` por fecha, sin transacción entre fechas (`Promise.allSettled`).**
`POST /requests/admin` no admite lote; el asistente lo invoca una vez por fecha y agrega los resultados. Alternativa descartada: abortar todo si una fecha falla — con varias fechas (rango o dispersas) un solo conflicto (recurso ocupado, duplicado) no debe tirar las demás; el paso final (`StepResult`) informa el resultado por fecha para que el admin entienda exactamente qué se creó y qué no.

**D3. La vista previa de auto-asignación es un endpoint de solo lectura nuevo, no un efecto secundario de `POST /requests/admin`.**
`GET /requests/admin/suggested-space?employeeId&date` replica la MISMA regla de categoría/planta que usa la creación real (`autoAssignParkingSpace`), sin persistir nada. Alternativa descartada: inferir la plaza en el cliente — duplicaría en JS la regla de negocio completa (prioridad de categoría, exclusión de recursos ocupados) con riesgo de divergencia; el cliente solo replica el **orden** de prioridad para agrupar la rejilla manual (`utils/parkingPriority.ts`), no decide qué plaza está libre.

**D4. El estado del asistente distingue `ALL` (una elección para todos los días) de `PER_DAY` (mapa fecha→elección), en vez de forzar siempre una fila por día.**
Con una fecha o con fechas homogéneas, `ALL` es más simple (una tarjeta/rejilla, un valor). `PER_DAY` solo aparece con >1 fecha y se siembra con la elección de `ALL` al activarse, para que cambiar de modo no obligue a re-elegir desde cero.

**D5. La navegación del plano en modo vista es "explorar" (marquee + minimapa), no arrastre directo del lienzo.**
Arrastrar el lienzo competiría con arrastrar para dibujar una selección o, en el asistente, con el propio gesto de elegir un puesto. Se separa: clic en marcador = selecciona; arrastrar en el lienzo = zoom por rectángulo; arrastrar el minimapa = panea. El modo edición (ADMIN reposicionando puestos) conserva el arrastre de marcador tal cual, sin tocarlo.

**D6. El maximizado del plano es in-app (overlay `position:fixed`), no la Fullscreen API nativa.**
La Fullscreen API sube el elemento al "top layer" del navegador, por encima de cualquier diálogo Radix — al maximizar el plano dentro del asistente y luego abrir el modal de confirmación de asignación, este quedaba oculto detrás del plano. El overlay in-app respeta el z-index normal de la app (por debajo de los diálogos), así que el modal de confirmación sigue apareciendo por encima.

**D7. Los modales dejan de cerrarse con Escape, en toda la aplicación.**
Decisión de producto general (no específica del asistente, pero motivada por él): en un flujo de varios pasos, un Escape accidental que cerrara el modal perdería el progreso. Se aplica de forma uniforme a `Dialog` (Radix, `onEscapeKeyDown` anulado), `Modal` y `ConfirmDialog` (legacy) para que el comportamiento sea consistente en toda la app, no solo en el asistente.

## Risks / Trade-offs

- **`Promise.allSettled` sin transacción entre fechas** → el admin puede terminar con una reserva parcial (p. ej. 3 de 5 fechas creadas). Mitigado por el resultado por-fecha en `StepResult` y porque cada `Request` individual sigue siendo atómica en el backend.
- **Vista previa de auto-asignación puede quedar obsoleta entre la consulta y la confirmación** (otro admin o el propio auto-asignación de otra solicitud toma la plaza sugerida mientras el resumen sigue abierto) → `POST /requests/admin` re-valida disponibilidad en el momento de crear (409 `NO_AVAILABILITY` si ya no hay plaza), así que la preview es informativa, no reserva nada.
- **Modo explorar (marquee) cambia el gesto de arrastre del plano en TODA la vista lectura**, no solo en el asistente → mitigado documentando y probando que el modo edición (arrastre de marcador) queda intacto; el cambio solo afecta a cuando `editMode = false`.
- **Suprimir Escape en todos los modales** reduce una vía de cierre habitual (accesibilidad/expectativa de usuario) → mitigado manteniendo cierre por (x), overlay y acciones del pie en todos los casos; es una decisión de producto explícita, no un descuido.

## Migration Plan

1. Sin migración de datos ni de esquema (reutiliza `Request`).
2. Sin flag de despliegue: el asistente sustituye directamente la navegación previa de la CTA "Nueva reserva" para ADMIN.
3. Rollback: revertir los commits de la rama: sin estado persistente que deshacer (la única superficie backend nueva es el endpoint de solo lectura `suggested-space`).
