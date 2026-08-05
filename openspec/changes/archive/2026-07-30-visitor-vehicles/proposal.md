## Why

Hoy el visitante tiene un ÚNICO campo `licensePlate` (matrícula) en el formulario de ficha, así que no se puede registrar más de un vehículo por visitante (coche + moto, vehículo de empresa + particular…), ni guardar marca, modelo o color. Se necesita gestionar varios vehículos por visitante, igual que ya se hace con los empleados (capacidad `employee-vehicles`).

## What Changes

- **Nueva capacidad `visitor-vehicles`:** un CRUD de vehículos por visitante (relación 1:N). Cada vehículo tiene **marca**, **modelo**, **matrícula** y **color**; **solo la matrícula es obligatoria**.
- **Backend:** entidad `VisitorVehicle` + tabla `visitor_vehicles` (migración Flyway **V36**, FK a `visitors(id)` ON DELETE CASCADE, `license_plate` NOT NULL, resto nullable). CRUD REST reservado a `ADMIN` anidado bajo el visitante: `GET/POST /visitors/{visitorId}/vehicles`, `PUT/DELETE /visitors/{visitorId}/vehicles/{vehicleId}`. Validación: matrícula obligatoria (400 si falta) y única por visitante (409 si se duplica); marca/modelo/color opcionales con longitud máxima.
- **Frontend:** el formulario de visitante pasa a tener **tabs** (Detalles / Vehículos). El tab **Vehículos** reutiliza el panel genérico `VehiclesPanel` (extraído de `employee-vehicles`): tabla (Marca, Modelo, Matrícula, Color, Acciones) + "Añadir vehículo", alta/edición en un modal anidado y borrado con confirmación en un diálogo aparte. En el alta de un visitante nuevo (aún sin id) el tab funciona en **modo borrador**: los vehículos se acumulan en memoria y el formulario los persiste al crear el visitante (no obliga a guardar antes).
- **Retirada del campo de matrícula suelto:** se elimina el campo `licensePlate` del **formulario de visitante**; la matrícula se gestiona ahora en el tab Vehículos. El campo `visitor.licensePlate` de la entidad y su columna en tabla/detalle se mantienen intactos por compatibilidad (ver Out of scope).
- **Compatibilidad:** el borrado del visitante elimina en cascada sus vehículos.

## Capabilities

### New Capabilities
- `visitor-vehicles`: gestión (alta, edición, borrado y listado) de varios vehículos por visitante, con marca/modelo/matrícula/color (solo matrícula obligatoria), su contrato REST anidado bajo el visitante, las reglas de validación/unicidad y el tab "Vehículos" del formulario de visitante (incluido el alta en modo borrador y la retirada del campo de matrícula suelto del formulario).

### Modified Capabilities
<!-- El contrato REST del CRUD de visitante no cambia; solo se retira el campo de matrícula del formulario (UI), documentado dentro de la nueva capacidad. -->

## Impact

- **Backend nuevo:** `visitor/VisitorVehicle.java` (entidad), `visitor/VisitorVehicleController.java`, `visitor/VisitorVehicleRepository.java`, `visitor/application/VisitorVehicleService.java`, `visitor/application/VisitorVehicleConflictException.java`, `visitor/dto/VisitorVehicle{Request,Response}.java`, migración `db/migration/V36__visitor_vehicles.sql`. Referencia de patrón: capacidad `employee-vehicles`.
- **Frontend nuevo:** `types/visitorVehicle.ts`, `api/visitorVehiclesApi.ts`, `hooks/useVisitorVehicles.ts`, `components/VisitorVehiclesPanel.tsx`. Panel genérico compartido `components/VehiclesPanel.tsx` + tipos `types/vehicle.ts` (`Vehicle`, `VehicleRequest`, `VehiclesHooks`, `DraftVehicle`, `VehiclesDraft`).
- **Frontend modificado:** `components/VisitorFormModal.tsx` (tabs Detalles/Vehículos, retirada del campo matrícula, estado de borrador y persistencia al crear); `components/EmployeeVehiclesPanel.tsx` (pasa a wrapper del panel genérico); claves i18n (es/en) — bloque `visitors.vehicles` y `visitors.form.tabs`, retirada de `visitors.form.licensePlate`.
- **Contrato API:** nuevos endpoints `/visitors/{visitorId}/vehicles[/{vehicleId}]` (ADMIN) documentados vía springdoc.
- **BD:** nueva tabla `visitor_vehicles`; sin cambios en `visitors`.
- **Sin breaking changes:** no se toca el contrato REST de visitante ni la entidad; solo desaparece el campo de matrícula del formulario (UI).
