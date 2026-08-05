## Context

El empleado tiene hoy un único campo `licensePlate` (matrícula) en `Employee` (back) y en la pestaña Detalles de `EmployeeFormModal` (front). El modal ya es multipestaña (`TabId = details | parking | desk | history` con un `TabBar` que conserva `role="tab"` para los tests). El módulo `visitor` es el patrón de referencia de un CRUD completo (entidad + controller REST + repository + service + DTOs) y `VisitorFormModal`/`VisitorsPanel` el del CRUD en el front. Última migración Flyway: **V34** → la nueva es **V35**.

## Goals / Non-Goals

**Goals:**
- Modelar N vehículos por empleado (1:N) con marca/modelo/matrícula/color; solo matrícula obligatoria.
- CRUD REST anidado bajo el empleado, reservado a ADMIN, con validación y unicidad por empleado.
- Tab "Vehículos" en el formulario de empleado con tabla + alta/edición/borrado.
- No romper nada existente (el campo `licensePlate` del empleado sigue igual).

**Non-Goals:**
- Retirar/migrar el `employee.licensePlate` único actual.
- Usar los vehículos en otras vistas (ocupación, plano, accesos), "vehículo activo", export de vehículos.
- Unicidad GLOBAL de matrícula entre empleados; auditoría/historial detallado por vehículo.

## Decisions

- **Modelo 1:N con tabla propia `employee_vehicles`.** Entidad `EmployeeVehicle { id, employeeId (FK), brand?, model?, licensePlate (NOT NULL), color?, createdAt }`. FK a `employees(id)` **ON DELETE CASCADE** (borrar empleado borra vehículos). Índice por `employee_id` para el listado.
- **Endpoints anidados bajo el empleado.** `GET/POST /employees/{employeeId}/vehicles` y `PUT/DELETE /employees/{employeeId}/vehicles/{vehicleId}`. Expresa la pertenencia y encaja con el RBAC del CRUD de empleados (ADMIN). Se valida que el `vehicleId` pertenezca al `employeeId` de la ruta (si no, 404).
- **Validación.** `licensePlate` obligatoria (`@NotBlank`, trim, longitud máx. p. ej. 15, se normaliza a mayúsculas al guardar/validar); `brand/model/color` opcionales con longitud máx. (p. ej. 60/60/30). Matrícula **única por empleado**: índice único `(employee_id, license_plate)` → violación se traduce a **409** en el `GlobalExceptionHandler` (patrón `FieldConflictException`). La unicidad global se descarta (coches compartidos entre empleados posible).
- **Normalización de matrícula.** Trim + mayúsculas antes de comparar/persistir, para que "1234abc" y "1234ABC" no se dupliquen.
- **Front: tab dependiente del id del empleado.** El tab "Vehículos" solo gestiona vehículos cuando el empleado ya existe (tiene `id`). En el alta de un empleado nuevo, el panel muestra un aviso "guarda primero el empleado" (evita gestionar hijos de un padre inexistente). La lista se carga con react-query (`useEmployeeVehiclesQuery(employeeId)`), y alta/edición/borrado invalidan esa query.
- **Alta/edición del vehículo.** Modal anidado (encima del modal del empleado) con campos marca/modelo/matrícula*/color; solo matrícula obligatoria en el front (espejo del back). Borrado con confirmación en un diálogo aparte (patrón `ConfirmDialog` del proyecto).
- **DTOs y patrón.** `EmployeeVehicleResponse`, `EmployeeVehicleCreateRequest`, `EmployeeVehicleUpdateRequest` (DTOs, nunca la entidad JPA en la capa web — S4684). Servicio + repositorio siguiendo `VisitorService`/`VisitorRepository`.

## Risks / Trade-offs

- **Duplicidad conceptual con `employee.licensePlate`.** Coexisten el campo único y la tabla de vehículos hasta un change de consolidación. Riesgo de confusión: se documenta que el tab Vehículos es la vía nueva; el campo suelto se mantiene solo por compatibilidad (export/otras vistas) y se abordará después.
- **Unicidad y normalización.** El índice único `(employee_id, license_plate)` debe casar con la normalización (mayúsculas/trim) para no permitir duplicados "equivalentes". Se normaliza en el servicio antes de persistir.
- **Cascade en SQL Server.** La FK ON DELETE CASCADE debe declararse en la migración; verificar que el borrado de empleado (si existe hard-delete) o su baja lógica se comporta como se espera. Si el empleado solo se DESACTIVA (no borra), la cascada no se dispara y los vehículos permanecen (aceptable).
- **UX del alta de empleado.** El tab deshabilitado/avisando durante el alta puede sorprender; se mitiga con copy claro y habilitándolo tras el primer guardado.

## Open Questions (negocio)

- ¿La matrícula debe ser única GLOBALMENTE (un vehículo = un dueño) o basta única por empleado? Por defecto: única por empleado.
- ¿Formato/validación de matrícula por país o solo longitud/no-vacío? Por defecto: no-vacío + longitud máx.
- ¿Se consolidará el `employee.licensePlate` actual como "primer vehículo" en un change posterior?
