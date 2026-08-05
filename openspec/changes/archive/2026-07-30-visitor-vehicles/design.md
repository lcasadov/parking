## Context

El visitante tiene hoy un único campo `licensePlate` (matrícula) en `Visitor` (back) y en el formulario `VisitorFormModal` (front, sobre `Dialog`, sin pestañas). Ya existe la capacidad `employee-vehicles` (CRUD 1:N de vehículos por empleado) como patrón directo: entidad + controller REST anidado + repository + service + DTOs + panel de front. Última migración Flyway: **V35** (`employee_vehicles`) → la nueva es **V36**. El panel de vehículos del empleado se generaliza para reutilizarlo aquí.

## Goals / Non-Goals

**Goals:**
- Modelar N vehículos por visitante (1:N) con marca/modelo/matrícula/color; solo matrícula obligatoria.
- CRUD REST anidado bajo el visitante, reservado a ADMIN, con validación y unicidad por visitante.
- Formulario de visitante con tabs (Detalles / Vehículos), reutilizando el panel genérico de vehículos.
- Poder añadir vehículos durante el alta de un visitante nuevo sin obligar a guardar antes (modo borrador).
- Retirar el campo de matrícula suelto del formulario de visitante.

**Non-Goals:**
- Retirar/migrar el `visitor.licensePlate` único de la entidad ni su columna en tabla/detalle.
- Usar los vehículos en otras vistas (accesos, matching), "vehículo activo", export de vehículos.
- Unicidad GLOBAL de matrícula entre visitantes; auditoría/historial detallado por vehículo.

## Decisions

- **Modelo 1:N con tabla propia `visitor_vehicles`.** Entidad `VisitorVehicle { id, visitorId (FK), brand?, model?, licensePlate (NOT NULL), color?, createdAt }`. FK a `visitors(id)` **ON DELETE CASCADE** (borrar visitante borra vehículos). Índice por `visitor_id` para el listado. `created_at` lo fija la BD (`DEFAULT SYSUTCDATETIME`, `insertable=false`), mapeado con `@JdbcTypeCode(SqlTypes.TIMESTAMP)` (espejo de `employee_vehicles`).
- **Endpoints anidados bajo el visitante.** `GET/POST /visitors/{visitorId}/vehicles` y `PUT/DELETE /visitors/{visitorId}/vehicles/{vehicleId}`. Se valida que el `vehicleId` pertenezca al `visitorId` de la ruta (si no, 404). RBAC `ADMIN` como el resto del CRUD de visitante.
- **Validación y normalización.** `licensePlate` obligatoria (`@NotBlank`, trim, longitud máx. 15, normalizada a mayúsculas al guardar/validar); `brand/model/color` opcionales (máx. 60/60/30). Matrícula **única por visitante**: índice único `(visitor_id, license_plate)` → violación traducida a **409** (`FieldConflictException` → `GlobalExceptionHandler`). Unicidad global descartada.
- **Front: panel genérico reutilizado.** Se extrae `VehiclesPanel` (de `employee-vehicles`) a un componente común parametrizado por: `hooks` (bundle react-query del propietario), `ns` (namespace i18n) y `ownerId`. La UI del CRUD (tabla + modal de alta/edición + `ConfirmDialog` de borrado) es idéntica para empleado y visitante; solo cambian el origen de datos y los textos. `EmployeeVehiclesPanel` y `VisitorVehiclesPanel` quedan como wrappers de 6 líneas.
- **Alta en modo borrador (visitante nuevo, sin id).** El panel abstrae el origen de datos en un `store`: modo *live* (react-query, propietario con id) y modo *borrador* (en memoria, propietario sin id). En el alta, el tab Vehículos opera en borrador: alta/edición/borrado sobre estado local; al **crear el visitante**, `VisitorFormModal` persiste cada vehículo del borrador contra el id recién creado (best-effort, `Promise.allSettled`). Si el alta del visitante falla (p. ej. DNI duplicado), el borrador no se pierde (solo se persiste en `onSuccess`).
- **Tabs en el formulario de visitante.** `VisitorFormModal` (sobre `Dialog`) gana un `TabBar` interno (`role="tab"`, mismo patrón que `EmployeeFormModal`) con `Detalles` (el formulario actual, sin el campo matrícula) y `Vehículos` (el panel). El footer del diálogo muestra Cancelar/Guardar en Detalles y solo Cerrar en Vehículos.
- **Retirada del campo matrícula del formulario.** Se elimina el `Input` de matrícula de la pestaña Detalles y del cuerpo de la petición (`toRequestBody`), y la clave i18n `visitors.form.licensePlate`. El campo `licensePlate` de la entidad y de la tabla/detalle se mantiene (compatibilidad).
- **DTOs y patrón.** `VisitorVehicleRequest` (uno para alta y edición), `VisitorVehicleResponse` (DTOs, nunca la entidad JPA en la capa web — S4684). Servicio + repositorio espejo de `EmployeeVehicleService`/`EmployeeVehicleRepository`.

## Risks / Trade-offs

- **Duplicidad conceptual con `visitor.licensePlate`.** El campo suelto de la entidad y su columna en tabla/detalle siguen existiendo (ahora vestigiales para visitantes nuevos, que no lo rellenan). Se documenta que el tab Vehículos es la vía nueva; la consolidación (migrar el campo a "primer vehículo" o retirarlo) se aborda en un change posterior.
- **Persistencia del borrador best-effort.** Si el visitante se crea pero algún vehículo del borrador falla al persistir, el visitante queda creado igualmente (se cierra el modal). Aceptable: la unicidad definitiva la valida el backend y el caso es poco probable (validación local previa). Un endurecimiento futuro podría reportar el fallo por vehículo.
- **Unicidad y normalización.** El índice único `(visitor_id, license_plate)` debe casar con la normalización (mayúsculas/trim) para no permitir duplicados "equivalentes". Se normaliza en el servicio antes de persistir.
- **Cascade en SQL Server.** La FK ON DELETE CASCADE se declara en la migración; si el visitante solo se desactiva (no borra), la cascada no se dispara y los vehículos permanecen (aceptable).

## Open Questions (negocio)

- ¿La matrícula debe ser única GLOBALMENTE (un vehículo = un dueño) o basta única por visitante? Por defecto: única por visitante.
- ¿Formato/validación de matrícula por país o solo longitud/no-vacío? Por defecto: no-vacío + longitud máx.
- ¿Se consolidará el `visitor.licensePlate` actual como "primer vehículo" y se retirará su columna en tabla/detalle en un change posterior?
