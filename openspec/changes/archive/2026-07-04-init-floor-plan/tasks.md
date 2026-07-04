# Tasks: init-floor-plan

> **Orden TDD estricto (Red → Green → Refactor).** Primero se escriben los tests (deben fallar), luego la implementación mínima para que pasen, luego el refactor. Ninguna tarea de implementación se aborda sin un test rojo previo. (Coherente con las oleadas 2-3 del flujo `apply` y `docs/TESTING-STRATEGY.md`.)

## 1. Tests primero — RED (deben fallar antes de implementar)
- [x] 1.1 `shouldReturnDeskStatesAndPositions_whenDateIsValid` (Requirement 1, camino feliz). — `FloorPlanIT`
- [x] 1.2 `shouldReturn400OutsideRequestWindow_whenDateIsPastOrBeyond14d` (Requirement 1, ventana). — `FloorPlanIT`
- [x] 1.3 `shouldNotRevealOtherHolders_whenMarkingOwnDeskAsMine` (Requirement 1, MINE vs ASSIGNED). — `FloorPlanIT` + `FloorPlanQueryServiceTest`
- [x] 1.4 `shouldCreateRequest_whenClickingAFreeDesk` (Requirement 2, camino feliz). — `FloorPlanIT`
- [x] 1.5 `shouldReturn409_whenRequestingANonFreeDesk` (Requirement 2, disponibilidad). — `FloorPlanIT`
- [x] 1.6 `shouldReturn409RequestAlreadyPending_whenDuplicatePendingForSameDate` (Requirement 2, unicidad). — `FloorPlanIT`
- [x] 1.7 `shouldPersistCoordinates_whenAdminUpdatesDeskPosition` (Requirement 3, camino feliz). — `FloorPlanIT`
- [x] 1.8 `shouldReturn400_whenCoordinatesOutOfRange` (Requirement 3, validación). — `FloorPlanIT` + `FloorPlanControllerTest`
- [x] 1.9 `shouldReturn403_whenEmployeeUpdatesDeskPosition` (Requirement 3, autorización). — `FloorPlanIT` + `FloorPlanControllerTest`
- [x] 1.10 `shouldReturn409_whenTwoEmployeesRequestSameFreeDeskConcurrently` (caso límite de concurrencia). — `FloorPlanIT`
- [x] 1.11 Cada scenario BDD del spec cubierto por ≥1 test (`FloorPlanIT` cubre los 8 scenarios; unit tests refuerzan ramas).

## 2. Implementación — GREEN (lo mínimo para que los tests pasen)
- [x] 2.1 Endpoint `GET /floor-plan?date={ISO_DATE}` (operationId `getFloorPlan`): proyección única con `deskId`, `deskNumber`, `category`, `coordX`, `coordY`, `state`. — `FloorPlanController` + `FloorPlanQueryService`
- [x] 2.2 Cálculo de `state` por puesto/fecha (`FREE`/`ASSIGNED`/`REQUESTED`/`MINE`/`RELEASED`) sobre `ResourceType = DESK`, sin N+1 (carga por rango, ensamblado en memoria).
- [x] 2.3 Validación de la fecha contra la ventana hoy..+14d → 400 `OUTSIDE_REQUEST_WINDOW`.
- [x] 2.4 Endpoint `POST /floor-plan/desks/{deskId}/request` (operationId `requestDeskFromFloorPlan`): reutiliza el ciclo de `Request` (`resourceType = DESK`) fijando el puesto desde la creación; solo si el puesto está `FREE`.
- [x] 2.5 Endpoint `PUT /floor-plan/desks/{deskId}/position` (operationId `updateDeskPosition`): persiste `coord_x`/`coord_y` con validación 0-100; solo `ADMIN`.
- [x] 2.6 Autorización por rol: ver plano = autenticado; editar posiciones = `ADMIN` (403 si `EMPLOYEE`).
- [x] 2.7 No revelar titulares ajenos: puestos de terceros como `ASSIGNED`/`REQUESTED`, el propio como `MINE`.
- [x] 2.8 Manejo de errores uniforme (`ApiError`): 400 ventana/coordenadas, 409 disponibilidad/unicidad, 403 autorización (reutiliza `GlobalExceptionHandler`).
- [x] 2.9 Añadir los tres endpoints `/floor-plan` a `docs/openapi.yaml` (tag `Floor Plan`).

## 3. Refactor
- [x] 3.1 Con los tests en verde: servicios separados (query/command) con complejidad < 15, constantes para literales repetidos y `orElseThrow`/try-free, aplicando `docs/SONAR-STANDARDS.md`.

## 4. Frontend — mismo ciclo test-first (Vitest + RTL → implementación)
- [ ] 4.1 Vista de plano: render de la imagen de planta con un marcador por puesto posicionado por `coordX`/`coordY` (%), coloreado por `state` y distinción visual de `EXECUTIVE`.
- [ ] 4.2 Selector de fecha que recarga el plano (`getFloorPlan`).
- [ ] 4.3 Click en puesto `FREE` → solicitud directa (`requestDeskFromFloorPlan`) con feedback de éxito/conflicto.
- [ ] 4.4 Editor de arrastre (solo `ADMIN`): arrastrar marcadores y persistir posición (`updateDeskPosition`).
- [ ] 4.5 Estado vacío/atenuado para puestos sin coordenadas o inactivos.
