## Why

Reservar un **puesto** obliga hoy al empleado a **elegirlo en el plano** (el backend exige `resourceId`; sin él lanza `RESOURCE_SELECTION_REQUIRED`). En móvil ese selector es **inusable** (plano minúsculo, marcadores amontonados) y, para una **solicitud rápida**, elegir sitio concreto es fricción innecesaria. Las **plazas** de parking ya se **auto-asignan por el rango del empleado** (frontera "altos" `HIGH_CATEGORIES` → ciertas plantas); los puestos, no. Además los puestos ya tienen `DeskCategory` (`STANDARD`/`EXECUTIVE`) pero es **decorativa**. Alineamos los puestos con la misma frontera de rango que el parking y les damos auto-asignación, para que el empleado reserve puesto **sin plano**.

## What Changes

- **Auto-asignación de puesto por categoría (opción A).** En la creación de solicitud de `DESK` **sin `resourceId`**, el sistema asigna un puesto según el rango del empleado, con la MISMA frontera que el parking:
  - **Altos** (`CEO, CONSEJO, DIRECTOR_N1, DIRECTOR_N2`) → prefieren `EXECUTIVE`; si no hay `EXECUTIVE` libre, caen a `STANDARD`.
  - **Resto** (`GERENTE, MANDO_INTERMEDIO, EMPLEADO`) → **solo** `STANDARD` (nunca `EXECUTIVE`).
- **`EXECUTIVE` deja de ser decorativo**: pasa a ser **exclusivo de dirección** en la auto-asignación (un no-alto nunca cae en `EXECUTIVE`).
- **Sin puesto válido libre → PENDIENTE** (aunque el modo sea `AUTOMATIC`): la solicitud nace `PENDING` para que el ADMIN la resuelva; no se devuelve `409` ni se obliga a elegir en el plano.
- **Elegir puesto concreto sigue disponible** cuando se envía `resourceId` (asistente del admin/visitante): comportamiento actual intacto.
- **Reserva rápida del empleado sin plano.** El modal "Nueva reserva" deja de ofrecer el selector de puesto en el plano; queda **fecha + plaza/puesto**. (El modal ya es a pantalla completa y el "atrás" del móvil lo cierra.)

## Capabilities

### New Capabilities
- `desk-auto-assignment`: asignación automática de puesto según el rango del empleado, con `EXECUTIVE` reservado a las categorías altas y fallback a `STANDARD`; si no hay puesto válido libre, la solicitud queda pendiente de aprobación aunque el modo sea automático.

### Modified Capabilities
- `requests`: la creación de solicitud de puesto en modo `AUTOMATIC` **sin `resourceId`** auto-asigna por categoría en vez de exigir elección; sin puesto válido libre → `PENDING`. Con `resourceId` (elección explícita) no cambia.
- `employee-portal`: la reserva rápida del empleado no usa el plano como selector de puesto; el modal queda fecha + plaza/puesto.

## Impact

- **Backend:** `RequestService` (rama de creación `DESK` automática → nuevo `autoAssignDesk(category, date)` espejo de `autoAssignParkingSpace`, con filtro por `DeskCategory` y regla de exclusión); `AvailabilityService` (nuevo `freeDesksForDate(date)` o reutilizar `availabilityForDate(date, DESK)` cargando la categoría); reutiliza `HIGH_CATEGORIES`. `docs/openapi.yaml` (aclarar que `resourceId` es opcional para `DESK`). Sin migración (el modelo ya tiene `DeskCategory`).
- **Frontend:** `CreateRequestModal` (quitar `DeskPickerModal`/"elegir puesto" y el envío de `resourceId` de puesto; rediseño limpio), tipos/hooks, i18n, MSW, tests.
- **Datos:** ninguno (los puestos ya llevan `category`).

## Out of scope

- Ampliar `DeskCategory` a los 7 niveles del empleado (opción B, descartada: el parking tampoco lo hace).
- Auto-asignación de puesto por **planta** (los puestos no tienen planta).
- Elección de puesto en el plano para el ADMIN/visitante (se mantiene tal cual).
- Puestos para visitantes.
