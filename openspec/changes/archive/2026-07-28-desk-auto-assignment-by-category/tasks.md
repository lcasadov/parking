# Tasks — desk-auto-assignment-by-category

> Verificación backend con **JDK 21** (`JAVA_HOME=.../openjdk@21`); tests unitarios
> con `mvn test` (sin Docker). ITs (Testcontainers) en el gate final.
> Tras recompilar, **reiniciar el backend dev** (evita 500 de clases obsoletas).

## 1. Backend — auto-asignación de puesto
- [x] 1.1 `AvailabilityService`: `freeDesksForDate(date)` → `List<Desk>` libres (activo, sin fija no liberada, sin APPROVED/visita ese día), con su `DeskCategory`. Espejo de `freeParkingSpacesForDate`.
- [x] 1.2 `RequestService.autoAssignDesk(category, date)`: alto → `EXECUTIVE` libre y si no `STANDARD`; no-alto → solo `STANDARD`; orden estable por número. `Optional<Desk>`.
- [x] 1.3 Rama de creación `DESK` en `AUTOMATIC` **sin `resourceId`**: usar `autoAssignDesk`; si vacío → `createPending` (no `RESOURCE_SELECTION_REQUIRED`, no `409`). Con `resourceId` → `chosenDesk` como hoy.
- [x] 1.4 `request-waitlist`: la promoción de `DESK` reutiliza `autoAssignDesk`/la regla de categoría (no promover no-alto a `EXECUTIVE`).
- [x] 1.5 `docs/openapi.yaml`: `resourceId` opcional para `DESK`; documentar auto-asignación por categoría.

## 2. Backend — tests
- [x] 2.1 Unitarios (Mockito, sin Docker): alto→EXECUTIVE; alto→STANDARD si no EXECUTIVE; no-alto→solo STANDARD; sin válido→PENDING; con `resourceId`→chosenDesk intacto.
- [ ] 2.2 ITs (Testcontainers, gate final): creación end-to-end por categoría + fallback pendiente.

## 3. Frontend — reserva rápida sin mapa
- [x] 3.1 `CreateRequestModal`: eliminar `DeskPickerModal`, el botón "elegir puesto", `selectedDesk` y el envío de `resourceId` de puesto. La solicitud de puesto viaja sin `resourceId`.
- [x] 3.2 Rediseño limpio del modal (ya a pantalla completa): fecha + plaza/puesto, avisos auto/pendiente y disponibilidad; mobile-first.
- [x] 3.3 i18n es/en (claves del selector huérfanas quedan sin uso, no molestan). MSW/hooks/tests.
- [x] 3.4 `lint` + `test` + `build` verdes.

## 4. Gate final
- [ ] 4.1 `JAVA_HOME=<jdk21> mvn -f backend/pom.xml clean verify` (BUILD SUCCESS + cobertura) + reinicio del backend dev.
- [ ] 4.2 Frontend `lint` + `test` + `build`.
