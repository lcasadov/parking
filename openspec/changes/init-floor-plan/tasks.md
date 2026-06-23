# Tasks: init-floor-plan

> **Orden TDD estricto (Red → Green → Refactor).** Primero se escriben los tests (deben fallar), luego la implementación mínima para que pasen, luego el refactor. Ninguna tarea de implementación se aborda sin un test rojo previo. (Coherente con las oleadas 2-3 del flujo `apply` y `docs/TESTING-STRATEGY.md`.)

## 1. Tests primero — RED (deben fallar antes de implementar)
- [ ] 1.1 `should_return_desk_states_and_positions_when_date_is_valid` (Requirement 1, camino feliz).
- [ ] 1.2 `should_return_400_outside_request_window_when_date_is_past_or_beyond_14d` (Requirement 1, ventana).
- [ ] 1.3 `should_not_reveal_other_holders_when_marking_own_desk_as_mine` (Requirement 1, MINE vs ASSIGNED).
- [ ] 1.4 `should_create_request_when_clicking_a_free_desk` (Requirement 2, camino feliz).
- [ ] 1.5 `should_return_409_when_requesting_a_non_free_desk` (Requirement 2, disponibilidad).
- [ ] 1.6 `should_return_409_request_already_pending_when_duplicate_pending_for_same_date` (Requirement 2, unicidad).
- [ ] 1.7 `should_persist_coordinates_when_admin_updates_desk_position` (Requirement 3, camino feliz).
- [ ] 1.8 `should_return_400_when_coordinates_out_of_range` (Requirement 3, validación).
- [ ] 1.9 `should_return_403_when_employee_updates_desk_position` (Requirement 3, autorización).
- [ ] 1.10 `should_return_409_when_two_employees_request_same_free_desk_concurrently` (caso límite de concurrencia).
- [ ] 1.11 Cada scenario BDD del spec cubierto por ≥1 test (nombres `should..._when...`).

## 2. Implementación — GREEN (lo mínimo para que los tests pasen)
- [ ] 2.1 Endpoint `GET /floor-plan?date={ISO_DATE}` (operationId `getFloorPlan`): proyección única de los 65 puestos con `deskId`, `deskNumber`, `category`, `coordX`, `coordY`, `state`.
- [ ] 2.2 Cálculo de `state` por puesto/fecha (`FREE`/`ASSIGNED`/`REQUESTED`/`MINE`/`RELEASED`) reutilizando la disponibilidad de `BookableResource` (`ResourceType = DESK`), sin N+1.
- [ ] 2.3 Validación de la fecha contra la ventana hoy..+14d → 400 `OUTSIDE_REQUEST_WINDOW`.
- [ ] 2.4 Endpoint `POST /floor-plan/desks/{deskId}/request` (operationId `requestDeskFromFloorPlan`): delega en el caso de uso de creación de `Request` (`resourceType = DESK`); solo si el puesto está `FREE`.
- [ ] 2.5 Endpoint `PUT /floor-plan/desks/{deskId}/position` (operationId `updateDeskPosition`): persiste `coord_x`/`coord_y` con validación 0-100; solo `ADMIN`.
- [ ] 2.6 Autorización por rol: ver plano = autenticado; editar posiciones = `ADMIN` (403 si `EMPLOYEE`).
- [ ] 2.7 No revelar titulares ajenos: puestos de terceros como `ASSIGNED`, el propio como `MINE`.
- [ ] 2.8 Manejo de errores uniforme (`ApiError { error, message, fields, timestamp }`): 400 ventana/coordenadas, 409 disponibilidad/unicidad, 403 autorización.
- [ ] 2.9 Añadir los tres endpoints `/floor-plan` a `docs/openapi.yaml`.

## 3. Refactor
- [ ] 3.1 Con los tests en verde: extraer métodos (complejidad < 15), eliminar duplicación y aplicar `docs/SONAR-STANDARDS.md`, sin cambiar comportamiento.

## 4. Frontend — mismo ciclo test-first (Vitest + RTL → implementación)
- [ ] 4.1 Vista de plano: render de la imagen de planta con un marcador por puesto posicionado por `coordX`/`coordY` (%), coloreado por `state` y distinción visual de `EXECUTIVE`.
- [ ] 4.2 Selector de fecha que recarga el plano (`getFloorPlan`).
- [ ] 4.3 Click en puesto `FREE` → solicitud directa (`requestDeskFromFloorPlan`) con feedback de éxito/conflicto.
- [ ] 4.4 Editor de arrastre (solo `ADMIN`): arrastrar marcadores y persistir posición (`updateDeskPosition`).
- [ ] 4.5 Estado vacío/atenuado para puestos sin coordenadas o inactivos.
