# Tasks: init-parking-spaces

> **Orden TDD estricto (Red → Green → Refactor).** Primero se escriben los tests (deben fallar), luego la implementación mínima para que pasen, luego el refactor. Ninguna tarea de implementación se aborda sin un test rojo previo. (Coherente con las oleadas 2-3 del flujo `apply` y `docs/TESTING-STRATEGY.md`.)

## 1. Tests primero — RED (deben fallar antes de implementar)
- [x] 1.1 should_createSpace_when_labelIsNew (alta con label nuevo → 201).
- [x] 1.2 should_return409_when_labelAlreadyExists (alta con label duplicado).
- [x] 1.3 should_return400_when_labelIsEmptyOrInvalid (validación de `label`).
- [x] 1.4 should_updateLabel_when_requestIsValid (edición de label válida).
- [x] 1.5 should_excludeFromAvailability_when_spaceIsDeactivated (desactivar excluye de disponibilidad).
- [x] 1.6 should_return409_when_updatingToLabelUsedByAnotherSpace (edición a label en uso).
- [x] 1.7 should_return404_when_updatingNonExistentSpace (edición de plaza inexistente).
- [x] 1.8 should_adjustTotal_when_adminConfiguresSpaces (configuración del total válida).
- [x] 1.9 should_return400_when_configureTotalIsNegative (configuración con total inválido).
- [x] 1.10 should_return403_when_employeeManagesSpaces (empleado intenta gestionar plazas).
- [x] 1.11 should_return409_when_concurrentInsertSameLabel (alta concurrente del mismo label, vía índice único).
- [x] 1.12 Cada scenario BDD del spec cubierto por ≥1 test (`@WebMvcTest` para HTTP + tests de caso de uso).

## 2. Implementación — GREEN (lo mínimo para que los tests pasen)
- [x] 2.1 Entidad `ParkingSpace` (`label`, `active`, `created_at`) + repositorio (puerto + adaptador JPA).
- [x] 2.2 Caso de uso de alta (`createParkingSpace`): validación de `label`, comprobación de unicidad y mapeo de colisión a 409.
- [x] 2.3 Caso de uso de edición (`updateParkingSpace`): cambio de `label` con unicidad y toggle de `active`; 404 si no existe.
- [x] 2.4 Caso de uso de configuración masiva (`configureParkingSpaces`): ajuste del total preservando histórico; validación `total >= 0`.
- [x] 2.5 Caso de uso de listado (`listParkingSpaces`): paginación + filtro opcional `active`.
- [x] 2.6 Controllers REST: `GET/POST /parking-spaces`, `PUT /parking-spaces/{id}`, `POST /parking-spaces/configure`.
- [x] 2.7 Autorización `ADMIN` en los cuatro endpoints (403 para `EMPLOYEE`).
- [x] 2.8 Manejo de errores uniforme (`ApiError { error, message, fields, timestamp }`): 400, 403, 404, 409.
- [x] 2.9 Índice único `UX_parking_spaces_label` y traducción de violación a 409.

## 3. Refactor
- [x] 3.1 Con los tests en verde: extraer métodos (complejidad < 15), eliminar duplicación y aplicar `docs/SONAR-STANDARDS.md`, sin cambiar comportamiento. Manejador de conflicto unificado (`FieldConflictException`) para evitar duplicación de handlers (S4144).

## 4. Frontend — mismo ciclo test-first (Vitest + RTL → implementación)
- [ ] 4.1 Vista de administración de plazas (listado paginado con filtro activa/inactiva), solo `ADMIN`.
- [ ] 4.2 Formulario de alta/edición de plaza (`label`, toggle `active`) con manejo de 400 (validación) y 409 (label duplicado).
- [ ] 4.3 Acción de configuración masiva del total (`total`) con manejo de 400.
- [ ] 4.4 Ocultar la sección de plazas a `EMPLOYEE` (RBAC en UI).
