# request-any-future-date

## Why
La solicitud del empleado está acotada a la ventana `hoy..hoy+14 días`. El negocio
requiere poder solicitar plaza/puesto para CUALQUIER fecha futura (hoy en adelante),
sin tope superior. Se mantiene la restricción de no permitir fechas pasadas.

## What Changes
- Backend: `RequestService` deja de aplicar el límite superior de la ventana; solo
  rechaza fechas anteriores a hoy. Se conserva la unicidad (`PENDING` por empleado/tipo/fecha).
- Frontend: el selector de fecha de solicitud (`CreateRequestModal`) y la navegación de
  fecha del plano (`FloorPlanDatebar`) dejan de topar en hoy+14; permiten cualquier fecha
  futura. Textos i18n actualizados.
- Spec `requests`: requirement "Creación de solicitud con ventana y unicidad" y sus escenarios.

## Impact
- Solo la validación de rango de fecha de la solicitud del empleado. Unicidad, modos
  MANUAL/AUTOMATIC, auto-asignación y aprobación NO cambian.
- Affected specs: `requests`.
- Affected code: backend `request/application/RequestService.java`; frontend
  `utils/requests.ts`, `CreateRequestModal.tsx`, `FloorPlanDatebar.tsx`, `useCalendar.ts`, i18n.

## Out of scope
- Permitir fechas pasadas (se siguen rechazando).
- Cambios de presentación del rediseño (van en otra rama).
