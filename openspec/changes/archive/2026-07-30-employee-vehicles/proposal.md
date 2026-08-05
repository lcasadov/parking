## Why

Hoy el empleado tiene un ÚNICO campo `licensePlate` (matrícula) en la pestaña Detalles del formulario, así que no se puede registrar más de un vehículo por empleado (coche + moto, coche de empresa + particular…), ni guardar marca, modelo o color. Se necesita gestionar varios vehículos por empleado.

## What Changes

- **Nueva capacidad `employee-vehicles`:** un CRUD de vehículos por empleado (relación 1:N). Cada vehículo tiene **marca**, **modelo**, **matrícula** y **color**; **solo la matrícula es obligatoria**.
- **Backend:** entidad `EmployeeVehicle` + tabla `employee_vehicles` (migración Flyway **V35**, FK a `employees(id)` ON DELETE CASCADE, `license_plate` NOT NULL, resto nullable). CRUD REST reservado a `ADMIN` anidado bajo el empleado: `GET/POST /employees/{employeeId}/vehicles`, `PUT/DELETE /employees/{employeeId}/vehicles/{vehicleId}`. Validación: matrícula obligatoria (400 si falta) y única por empleado (409 si se duplica); marca/modelo/color opcionales con longitud máxima.
- **Frontend:** nuevo tab **Vehículos** en `EmployeeFormModal` (visible solo en edición; en alta se pide guardar antes el empleado). Tabla de vehículos (Marca, Modelo, Matrícula, Color, Acciones) + "Añadir vehículo", alta/edición en un modal anidado y borrado con confirmación en un diálogo aparte. Tipos, API, hooks react-query e i18n asociados.
- **Compatibilidad:** el campo único `employee.licensePlate` existente **se mantiene intacto** en esta fase (ver Out of scope); el borrado del empleado elimina en cascada sus vehículos.

## Capabilities

### New Capabilities
- `employee-vehicles`: gestión (alta, edición, borrado y listado) de varios vehículos por empleado, con marca/modelo/matrícula/color (solo matrícula obligatoria), su contrato REST anidado bajo el empleado, las reglas de validación/unicidad y el tab "Vehículos" del formulario de empleado.

### Modified Capabilities
<!-- Ninguna: el campo `licensePlate` único del empleado no cambia su contrato en esta fase. -->

## Impact

- **Backend nuevo:** `employee/EmployeeVehicle.java` (entidad), `employee/EmployeeVehicleController.java`, `employee/EmployeeVehicleRepository.java`, `employee/application/EmployeeVehicleService.java`, `employee/dto/EmployeeVehicle{Response,CreateRequest,UpdateRequest}.java`, migración `db/migration/V35__employee_vehicles.sql`. Referencia de patrón: módulo `com.aleatica.parking.visitor`.
- **Frontend nuevo:** `types/employeeVehicle.ts`, `api/employeeVehiclesApi.ts`, `hooks/useEmployeeVehicles.ts`, componentes del tab (tabla + form de vehículo dentro de `EmployeeFormModal`), claves i18n (es/en).
- **Frontend modificado:** `components/EmployeeFormModal.tsx` (nuevo `TabId 'vehicles'` + panel), `TabBar`.
- **Contrato API:** nuevos endpoints `/employees/{employeeId}/vehicles[/{vehicleId}]` (ADMIN) documentados vía springdoc.
- **BD:** nueva tabla `employee_vehicles`; sin cambios en `employees`.
- **Sin breaking changes:** no se toca el campo `licensePlate` del empleado ni los flujos existentes.
