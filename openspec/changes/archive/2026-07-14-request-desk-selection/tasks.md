## 1. Requisitos previos

- [x] 1.1 Verificar que `system-settings` (`approvalMode` MANUAL/AUTOMATIC) y `request-auto-assignment` (auto-aprobación de puesto elegido en `RequestService.create`) están aplicados; este change se apoya en ambos
- [x] 1.2 Confirmar que `RequestCreateRequest.resourceId` está en `docs/openapi.yaml` (contrato ya definido) y que `POST /floor-plan/desks/{deskId}/request` existe

## 2. Backend — coherencia de FloorPlanCommandService con el modo global (TDD, supersede #98)

- [x] 2.1 (Red) Test de `FloorPlanCommandService.requestDesk` en modo `MANUAL`: comportamiento actual intacto (nace `PENDING`, `RequestCreatedEvent`)
- [x] 2.2 (Red) Test de `requestDesk` en modo `AUTOMATIC`: el puesto pinchado se auto-aprueba (nace `APPROVED` con el puesto asignado, `resolvedById = null`, nota `auto`, `RequestApprovedEvent`)
- [x] 2.3 (Red) Test de disponibilidad/duplicado en ambos modos: puesto no libre → 409; duplicado `PENDING` → 409 `REQUEST_ALREADY_PENDING` (sin cambios respecto a hoy)
- [x] 2.4 (Green) Inyectar `SystemSettingsService` en `FloorPlanCommandService`; ramificar `requestDesk` por `approvalMode` tras validar ventana + disponibilidad + no-duplicado; extraer `createPendingDesk(...)` / `autoApproveDesk(...)` (complejidad cognitiva < 15, S3776)
- [x] 2.5 (Green) Reutilizar el evento y la semántica de auto-aprobación de `RequestService` (mismo actor `null`, nota `auto` como constante compartida — evitar literal duplicado S1192, `RequestApprovedEvent`)
- [x] 2.6 (Red→Green) Test de concurrencia: dos auto-aprobaciones del mismo puesto/fecha → la 2ª recibe 409 por el índice único filtrado `APPROVED` (índice sin cambios)
- [x] 2.7 (Opcional, aditivo) Añadir el `status` resultante (`APPROVED`/`PENDING`) al `DeskRequestResponse` para que el plano ajuste el mensaje; actualizar `docs/openapi.yaml` si se expone

## 3. Frontend — tipo y envío del resourceId (TDD)

- [x] 3.1 (Red) Test del hook/serializador de creación: la solicitud de PUESTO con puesto elegido incluye `resourceId` en el body de `POST /requests`; la de PLAZA no lo incluye
- [x] 3.2 (Green) Añadir `resourceId?: number` a `RequestCreateRequest` (`types/request.ts`) y propagarlo en `hooks/useRequests`/`api` de creación
- [x] 3.3 (Green) Ajustar `toastKeyForError`/`successToastKey` si procede para el feedback de auto-aprobación del puesto elegido

## 4. Frontend — botón "Seleccionar puesto" y número en el modal (TDD)

- [x] 4.1 (Red) Test de `CreateRequestModal`: con PUESTO seleccionado aparece el botón "Seleccionar puesto"; al elegir un puesto se muestra su **número** (`Desk.number`), nunca el id
- [x] 4.2 (Green) Añadir el botón "Seleccionar puesto" (solo cuando PUESTO está marcado), estado `selectedDesk = { id, number }`, visualización del número + acción de cambiar/quitar, e i18n ES/EN
- [x] 4.3 (Green) Incluir `resourceId = selectedDesk.id` en el `POST /requests` de la solicitud de puesto cuando hay puesto elegido; sin elegir, enviar sin `resourceId` (retrocompatible)

## 5. Frontend — plano como selector con feedback visual (TDD)

- [x] 5.1 (Red) Test del selector (`DeskPickerModal` reutilizando `FloorPlanSurface`): pinchar un puesto `FREE` invoca `onPick({ deskId, deskNumber })`, cierra el selector y NO llama a `POST /floor-plan/.../request`
- [x] 5.2 (Red) Test de feedback visual: el puesto seleccionado recibe la clase `floor-marker-selected` y `aria-pressed`/`aria-selected`; se muestra el mensaje `role="status"` "Puesto N seleccionado"
- [x] 5.3 (Green) Añadir el estado visual `SELECTED` en `utils/floorPlan`/`FloorPlanMarker` (clase `floor-marker-selected` con token del design-system, sin hex suelto) y la prop `selectedDeskId`
- [x] 5.4 (Green) Añadir el modo `select` al contenedor del plano (prop `mode: 'request' | 'select'`): en `select`, pinchar `FREE` invoca `onPick` en vez de crear la solicitud; reutilizar la fecha de la solicitud, sin lista móvil de solicitud directa
- [x] 5.5 (Green) Mensaje de confirmación accesible (`role="status"`) con el número del puesto e i18n ES/EN
- [x] 5.6 (Green) Cerrar el selector al elegir y devolver el puesto al `CreateRequestModal`

## 6. Verificación y cierre

- [x] 6.1 Cobertura ≥80% líneas / ≥75% branches en backend y frontend; 0 violations nuevas de Sonar; complejidad cognitiva < 15 en `requestDesk` (extraer métodos)
- [x] 6.2 Tests E2E (Playwright): (a) empleado elige un puesto concreto desde el plano en el modal, ve el número y envía la solicitud; (b) en modo `AUTOMATIC` el puesto elegido queda `APPROVED`; (c) en modo `AUTOMATIC` pinchar un puesto libre en el plano (solicitud directa) queda `APPROVED`; (d) feedback visual de selección visible (color + mensaje)
- [x] 6.3 `npm run lint && npm test && npm run build` y `mvn clean verify` en verde; `openspec validate request-desk-selection` sin errores
