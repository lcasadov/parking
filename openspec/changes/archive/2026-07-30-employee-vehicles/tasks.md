## 1. Backend — modelo y persistencia

- [x] 1.1 Migración `db/migration/V35__employee_vehicles.sql` (SQL Server, con `GO`): tabla `employee_vehicles` (`id` PK identity, `employee_id` FK → `employees(id)` ON DELETE CASCADE, `license_plate` NVARCHAR(15) NOT NULL, `brand` NVARCHAR(60) NULL, `model` NVARCHAR(60) NULL, `color` NVARCHAR(30) NULL, `created_at` DATETIME2 NOT NULL); índice único `(employee_id, license_plate)` e índice por `employee_id`.
- [x] 1.2 Entidad JPA `employee/EmployeeVehicle.java` (mapeo de columnas; `@ManyToOne`/`employeeId`).
- [x] 1.3 `employee/EmployeeVehicleRepository.java` (Spring Data): `findByEmployeeId`, `findByIdAndEmployeeId`, `existsByEmployeeIdAndLicensePlate`.

## 2. Backend — DTOs, servicio y controlador (CRUD, ADMIN)

- [x] 2.1 DTOs: `EmployeeVehicleResponse`, `EmployeeVehicleCreateRequest`, `EmployeeVehicleUpdateRequest` (validación `@NotBlank` matrícula + `@Size` en todos; nunca exponer la entidad — S4684).
- [x] 2.2 `employee/application/EmployeeVehicleService.java`: list/create/update/delete; verificar que el empleado existe; normalizar matrícula (trim + mayúsculas); comprobar unicidad por empleado (→ conflicto); verificar pertenencia `vehicleId`↔`employeeId` (→ not found).
- [x] 2.3 `employee/EmployeeVehicleController.java`: `GET/POST /employees/{employeeId}/vehicles`, `PUT/DELETE /employees/{employeeId}/vehicles/{vehicleId}`, `@PreAuthorize("hasRole('ADMIN')")`, anotaciones springdoc (@Operation/@ApiResponses).
- [x] 2.4 Mapear la violación de unicidad a `409` en el flujo (reutilizar `FieldConflictException`/`GlobalExceptionHandler`); matrícula vacía → `400` (bean validation); vehículo inexistente/ajeno → `404`.

## 3. Frontend — datos y estado

- [x] 3.1 `types/employeeVehicle.ts`: `EmployeeVehicle`, `EmployeeVehicleCreate`, `EmployeeVehicleUpdate`.
- [x] 3.2 `api/employeeVehiclesApi.ts`: `listEmployeeVehicles`, `createEmployeeVehicle`, `updateEmployeeVehicle`, `deleteEmployeeVehicle` (rutas anidadas bajo el empleado).
- [x] 3.3 `hooks/useEmployeeVehicles.ts`: `useEmployeeVehiclesQuery(employeeId)` + mutaciones create/update/delete con invalidación de la lista.

## 4. Frontend — tab "Vehículos" en el formulario de empleado

- [x] 4.1 `components/EmployeeFormModal.tsx`: añadir `TabId 'vehicles'` al `TabBar` y su panel; el tab solo gestiona cuando el empleado tiene `id` (en alta, aviso "guarda primero el empleado").
- [x] 4.2 Panel de vehículos: tabla (Marca, Modelo, Matrícula, Color, Acciones) + botón "Añadir vehículo"; alta/edición en un modal anidado (matrícula obligatoria en el front) y borrado con confirmación en un diálogo aparte.
- [x] 4.3 i18n (es/en): título del tab, columnas, labels del formulario del vehículo, validación de matrícula y acciones (añadir/editar/borrar/guardar/cancelar).

## 5. Tests y Quality Gate

- [x] 5.1 Backend: tests del CRUD — alta con solo matrícula (201), alta completa (201), matrícula vacía (400), duplicado por empleado (409), editar (200), borrar (204), listar (200), no-ADMIN (403), vehículo ajeno/inexistente (404); cascada al borrar empleado.
- [x] 5.2 Frontend: tests del tab — render de la tabla, aviso en alta sin id, validación de matrícula obligatoria en el form, e invalidación de la lista tras alta/edición/borrado.
- [x] 5.3 Verificación integral: `mvn verify` (back) y `npm run lint && npm test && npm run build` (front) verdes, cobertura ≥ umbral, 0 violations nuevas de Sonar. Documentar el `sort`/contrato nuevo en la OpenAPI (springdoc) de los endpoints de vehículos.
