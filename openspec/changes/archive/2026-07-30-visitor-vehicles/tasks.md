## 1. Backend — modelo y persistencia

- [x] 1.1 Migración `db/migration/V36__visitor_vehicles.sql` (SQL Server, con `GO`): tabla `visitor_vehicles` (`id` PK identity, `visitor_id` FK → `visitors(id)` ON DELETE CASCADE, `license_plate` NVARCHAR(15) NOT NULL, `brand` NVARCHAR(60) NULL, `model` NVARCHAR(60) NULL, `color` NVARCHAR(30) NULL, `created_at` DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME); índice único `(visitor_id, license_plate)` e índice por `visitor_id`.
- [x] 1.2 Entidad JPA `visitor/VisitorVehicle.java` (mapeo de columnas; `visitorId`; `created_at` con `@JdbcTypeCode(SqlTypes.TIMESTAMP)` + `insertable=false`).
- [x] 1.3 `visitor/VisitorVehicleRepository.java` (Spring Data): `findByVisitorIdOrderByIdAsc`, `findByIdAndVisitorId`, `existsByVisitorIdAndLicensePlate`, `existsByVisitorIdAndLicensePlateAndIdNot`.

## 2. Backend — DTOs, servicio y controlador (CRUD, ADMIN)

- [x] 2.1 DTOs: `VisitorVehicleRequest` (un record para alta y edición; `@NotBlank` matrícula + `@Size` en todos) y `VisitorVehicleResponse` (nunca exponer la entidad — S4684).
- [x] 2.2 `visitor/application/VisitorVehicleService.java`: list/create/update/delete; verificar que el visitante existe; normalizar matrícula (trim + mayúsculas); comprobar unicidad por visitante (→ `VisitorVehicleConflictException`); verificar pertenencia `vehicleId`↔`visitorId` (→ not found).
- [x] 2.3 `visitor/VisitorVehicleController.java`: `GET/POST /visitors/{visitorId}/vehicles`, `PUT/DELETE /visitors/{visitorId}/vehicles/{vehicleId}`, `@PreAuthorize("hasRole('ADMIN')")`, anotaciones springdoc (@Operation/@ApiResponses).
- [x] 2.4 Mapear la violación de unicidad a `409` (reutilizar `FieldConflictException`/`GlobalExceptionHandler`); matrícula vacía → `400` (bean validation); vehículo inexistente/ajeno → `404`.

## 3. Frontend — datos, estado y panel genérico reutilizable

- [x] 3.1 Extraer el panel de vehículos a un componente genérico `components/VehiclesPanel.tsx` + tipos compartidos `types/vehicle.ts` (`Vehicle`, `VehicleRequest`, `VehiclesHooks`, `DraftVehicle`, `VehiclesDraft`); `EmployeeVehiclesPanel` pasa a ser wrapper.
- [x] 3.2 `types/visitorVehicle.ts` (alias del tipo genérico), `api/visitorVehiclesApi.ts` (list/create/update/delete anidados bajo el visitante), `hooks/useVisitorVehicles.ts` (query + mutaciones con invalidación) + bundle `visitorVehiclesHooks`.
- [x] 3.3 `components/VisitorVehiclesPanel.tsx`: wrapper del panel genérico con los hooks/textos del visitante; soporta modo borrador (`draft`).

## 4. Frontend — tabs y modo borrador en el formulario de visitante

- [x] 4.1 `components/VisitorFormModal.tsx`: añadir `TabBar` interno con tabs `Detalles` / `Vehículos`; el tab Vehículos monta el panel (edición → live; alta → borrador).
- [x] 4.2 Modo borrador en el alta: acumular vehículos en estado (`DraftVehicle[]`) y persistirlos contra el visitante recién creado en `onSuccess` (`Promise.allSettled`), sin obligar a guardar antes.
- [x] 4.3 Retirar el campo de matrícula suelto del formulario de visitante (Input, `FormState`, `toRequestBody`) y la clave i18n `visitors.form.licensePlate`.
- [x] 4.4 i18n (es/en): bloque `visitors.vehicles` (tabLabel, saveFirst, empty, columnas, labels, validación, acciones, títulos/cuerpo de modales) + `visitors.form.tabs` (details/vehicles).

## 5. Tests y Quality Gate

- [x] 5.1 Backend: tests del CRUD — alta con solo matrícula (201), alta completa (201), matrícula vacía (400), duplicado por visitante (409), editar (200), borrar (204), listar (200), no-ADMIN (403), vehículo ajeno/inexistente (404). `VisitorVehicleControllerTest` (13) + `VisitorVehicleServiceTest` (10).
- [x] 5.2 Frontend: `VisitorVehiclesPanel.test.tsx` (render, validación matrícula, alta + invalidación, borrado con diálogo) y `VisitorFormModal.test.tsx` (tab Vehículos en borrador sin guardar antes; alta de visitante + persistencia de vehículos en borrador).
- [x] 5.3 Verificación integral: backend (paquete visitor `mvn test` verde, 94/94) y frontend (`tsc` + `npm run build` + tests de los componentes afectados) verdes; 0 violations nuevas de Sonar; endpoints de vehículos documentados vía springdoc.
