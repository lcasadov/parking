## Context

- **Plazas** (`RequestService.autoAssignParkingSpace(category, date)`): toma las plazas libres y las ordena por preferencia de planta según `isHighCategory(category)` (`HIGH_CATEGORIES = {CEO, CONSEJO, DIRECTOR_N1, DIRECTOR_N2}`). Es **preferencia**, no exclusión: cualquiera puede acabar en cualquier plaza libre.
- **Puestos**: la creación `DESK` en `AUTOMATIC` exige `resourceId` (`chosenDesk`); sin él → `ResourceSelectionRequiredException` (`RESOURCE_SELECTION_REQUIRED`). No hay auto-asignación. `Desk` ya tiene `DeskCategory` (`STANDARD`/`EXECUTIVE`), hoy sin efecto funcional.
- Existe `freeParkingSpacesForDate(date)`; NO existe aún el equivalente de puestos libres con su categoría.

## Goals

- Reservar puesto **sin plano** en la reserva rápida del empleado.
- `EXECUTIVE` = puestos de dirección (exclusivos de categorías altas); resto solo `STANDARD`.
- Coherencia con el parking: misma frontera `HIGH_CATEGORIES`.

### Non-Goals
- 7 niveles en el puesto; planta en puestos; cambiar el flujo del admin/visitante con `resourceId`.

## Decisions

- **Regla de asignación (a diferencia del parking, hay EXCLUSIÓN):**
  - Alto → candidatos `EXECUTIVE` primero, luego `STANDARD` (fallback).
  - No-alto → candidatos **solo** `STANDARD`.
  - Dentro del conjunto de candidatos, orden estable por número de puesto (determinista).
- **Sin candidato libre → `PENDING`** aunque `AUTOMATIC` (reutiliza `createPending`), NO `409` ni `RESOURCE_SELECTION_REQUIRED`. Motivo: el empleado no elige, así que "sin hueco" no debe bloquear; el ADMIN decide.
- **`resourceId` presente** (elección explícita) → ruta `chosenDesk` actual sin cambios (respeta la elección del admin/visitante y valida disponibilidad).
- **Frontera reutilizada:** `HIGH_CATEGORIES` ya definido; `DeskCategory.EXECUTIVE` ↔ altos. Sin duplicar la frontera.
- **Frontend:** el empleado no envía `resourceId` de puesto; el backend auto-asigna. Se elimina el `DeskPickerModal` del flujo del empleado (el plano sigue para admin/visitante).

## Risks / Trade-offs

- **Exclusión de `EXECUTIVE`**: si todos los puestos son `EXECUTIVE` y un no-alto solicita, siempre irá a `PENDING`. Aceptable (config de inventario); se documenta.
- **Interacción con lista de espera**: la promoción automática de puestos (`request-waitlist`) ya toma "el primer puesto libre"; conviene que **también respete la categoría** para no promover un no-alto a un `EXECUTIVE`. Se ajusta la promoción de `DESK` para reutilizar la misma regla.
- **Concurrencia**: misma protección que hoy (índice único recurso-fecha); si el puesto elegido se ocupa entre selección y guardado, se reintenta/– o cae a `PENDING`.

## Migration Plan

Ninguna: `desks.category` ya existe (V13, `STANDARD`/`EXECUTIVE`). Solo cambia la lógica de asignación.

## Open Questions

Resueltas: opción A (frontera del parking, no 7 niveles); `EXECUTIVE` exclusivo de altos; alto puede caer a `STANDARD`; sin puesto válido → pendiente.
