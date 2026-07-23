# release-occupied-resource

## Why
La acción "Liberar" solo actúa sobre asignaciones FIJAS (crea un `Release`). Un recurso
ocupado por una SOLICITUD APROBADA no se puede liberar desde "Liberar por fecha" (admin) —
la UI lista el recurso pero el backend responde `NoFixedAssignmentException`. Y en "Mi semana"
el botón "Liberar" del empleado está inerte cuando no hay asignación fija. El negocio requiere
poder liberar cualquier recurso ocupado (fija o por solicitud), plaza y puesto, para empleado y admin.

## What Changes
- Backend: **nueva capacidad ADMIN** para cancelar/liberar la solicitud APROBADA (fecha futura)
  de un empleado, con motivo obligatorio y auditoría; libera el recurso para esa fecha (misma
  semántica de liberación por cancelación que ya usa el empleado). El empleado ya puede cancelar
  la suya (change `cancel-approved-request`); no cambia.
- Frontend empleado ("Mi semana"): "Liberar" opera sobre el recurso del día — si viene de una
  solicitud propia la cancela (APPROVED futura libera; PENDING cancela), si viene de asignación
  fija crea un `Release`. Soporta plaza y puesto.
- Frontend admin ("Liberar por fecha"): "Liberar" sobre recurso ocupado por solicitud usa la nueva
  cancelación admin; sobre asignación fija sigue con la liberación administrativa. Etiqueta del
  recurso corregida ("Plaza"→"Recurso", muestra plaza o puesto).

## Impact
- Affected specs: `requests` (nueva cancelación administrativa de solicitud aprobada).
- Affected code backend: `request/` (controller + service + RBAC + auditoría).
- Affected code frontend: `MyWeekPage`, `ReleaseResourceModal`, `AdministrativeReleaseModal`/
  `ReleaseByDatePage`, hooks/api de requests/releases, i18n.

## Out of scope
- Registrar un `Release` cuando se libera por cancelación de solicitud (decisión: solo cancelar;
  aparece en "Mis solicitudes" como cancelada, no en "Mis liberaciones").
- Fechas pasadas (no se libera/cancela el pasado, como hoy).
