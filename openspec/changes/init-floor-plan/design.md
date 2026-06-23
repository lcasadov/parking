# Design: init-floor-plan

## Context
El plano es la cara visual del modelo de puestos: una imagen de la planta con un
marcador por puesto, posicionado mediante coordenadas relativas y coloreado según
el estado del puesto para una fecha. El cálculo de estado reutiliza la lógica de
disponibilidad ya existente (`availability-calendar`) sobre `BookableResource`
con `ResourceType = DESK`, por lo que esta capability no introduce reglas de
disponibilidad nuevas: las consume y las proyecta sobre coordenadas. El parking
queda fuera (no tiene plano).

## Goals
- Una sola llamada (`GET /floor-plan?date`) devuelve posición + estado de los 65 puestos para pintar el plano sin N+1.
- Solicitar un puesto desde el plano es equivalente al flujo normal de `requests` (mismo `Request`, mismas validaciones de ventana/unicidad/disponibilidad).
- El admin reposiciona marcadores sin tocar BD manualmente; las coordenadas son independientes de la resolución (porcentaje 0-100).

## Decisions
- **Coordenadas relativas (%)**: `coord_x`/`coord_y` en 0-100 sobre el ancho/alto de la imagen, no en píxeles. Justificación: independencia de resolución y de la imagen concreta; el frontend escala al renderizar.
- **Estado derivado, no almacenado**: `state` por puesto/fecha se calcula en lectura a partir de `FixedAssignment`/`Request`/`Release`, no se persiste. Justificación: evita desincronización; el estado depende del empleado que consulta (`MINE`).
- **Reutilizar `requests`**: `POST /floor-plan/desks/{deskId}/request` delega en el caso de uso de creación de `Request` (`resourceType = DESK`). Justificación: una sola fuente de verdad para ventana (400 `OUTSIDE_REQUEST_WINDOW`), unicidad pendiente (409 `REQUEST_ALREADY_PENDING`) y disponibilidad (409).
- **No revelar titulares**: los puestos de terceros se devuelven como `ASSIGNED` sin nombre; solo el propio aparece como `MINE`. Justificación: coherencia con "Mi Semana" (RGPD/minimización).
- **`EXECUTIVE` solo es presentación**: la categoría se devuelve como flag para estilo visual; no altera reglas de disponibilidad ni liberación. Justificación: README §"Categorías de puesto".
- **Carga única por fecha**: un único query con `JOIN`/proyección para los 65 puestos evita N+1 en el cálculo de estado. Justificación: rendimiento del plano (un render por cambio de fecha).

## Risks
- **Concurrencia al pinchar el mismo puesto libre**: dos empleados a la vez → resuelto por la validación de disponibilidad de `requests` (uno 201, otro 409). Mitigado reutilizando el control de concurrencia existente.
- **Puestos sin coordenadas**: recién creados sin `coord_x`/`coord_y`; mitigado devolviéndolos sin posición y dejando que el admin los coloque (no rompe el render).
- **Coste de cálculo de estado**: 65 puestos × consultas → mitigado con carga única por fecha (decisión arriba).

## Migration Plan
- Sin tablas nuevas propias: `coord_x`/`coord_y`, `category` y `desk_number` viven en la tabla `desks`, introducida por la capability `desks` (ver `docs/data-model.md`, sección de `Desk` _[aún no presente: desks es posterior al núcleo de parking]_).
- Sin migración de datos en esta capability (solo lectura/escritura de coordenadas sobre `desks` existente).
- Los endpoints `/floor-plan` se añadirán a `docs/openapi.yaml` al implementar (hoy marcados `_[no en openapi.yaml todavía]_`).
