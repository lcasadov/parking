# Design: desk-support-admin-flows

## Context
El modelo genérico de recurso (C1) hace que `Request`, `FixedAssignment` y la
disponibilidad lleven `resourceType` (`PARKING`/`DESK`) y `resource_id`. La respuesta
de solicitud ya expone `resourceType`; `GET /availability?resourceType=DESK` devuelve
puestos disponibles (con `parkingSpaceId` = id del puesto y `label` `D-xx`); el approve
usa `parkingSpaceId` como `resource_id` genérico; `FixedAssignmentPutRequest` acepta
`resourceType`. La UI admin, en cambio, asume plaza en todos estos puntos. Este change
solo cablea el frontend a lo que el backend ya ofrece.

## Goals
- El admin ve el tipo de cada solicitud y la resuelve con el recurso correcto.
- La asignación fija cubre plaza y puesto por igual.
- Cero cambios de contrato/backend.

## Decisions
- **El modal de aprobación lee `resourceType` de la solicitud** y carga la lista de recursos correspondiente (plazas vs `availability?resourceType=DESK`). *Por qué:* misma UX que el mockup 05 ("Plaza/Puesto disponible"); el `parkingSpaceId` del body transporta el id del recurso elegido (plaza o puesto).
- **Etiquetas dependientes del tipo** (i18n): "Solicitud de plaza"/"Solicitud de puesto", "Plaza disponible"/"Puesto disponible". *Por qué:* claridad para el admin.
- **Asignación fija con selector de recurso** (Plaza/Puesto) que cambia la lista de recursos y envía `resourceType`. *Por qué:* el backend ya lo acepta; es la pieza que falta en UI. Se permite que un empleado tenga plaza fija y puesto fijo (recursos independientes, como en C2).
- **Tipo visible en listados** (inbox admin y "Mis solicitudes") con un pill/columna. *Por qué:* el usuario reportó que no se distingue.

## Risks
- **Disponibilidad de puestos para la fecha**: al aprobar un puesto hay que listar puestos libres esa fecha (no todos). *Mitigación:* usar `availability?date=<requestedDate>&resourceType=DESK`.
- **Regresión del flujo de plaza**: al generalizar el modal. *Mitigación:* PARKING sigue el camino actual (default); tests para ambos tipos; verificación visual + e2e.
- **Unicidad**: aprobar un puesto ya asignado → 409. *Mitigación:* el backend ya lo controla; la UI muestra el error existente.

## Migration Plan
- Sin migración ni cambios de contrato. Solo frontend.
- Verificación visual (mockups 04/05) + prueba en vivo del flujo puesto (solicitud empleado → aprobación admin con puesto).
