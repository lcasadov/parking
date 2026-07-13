## 1. Requisitos previos (dependencias)

- [ ] 1.1 Verificar que los changes `parking-space-floors` (planta = número/1000 en la plaza) y `employee-category` (categoría del empleado) están aplicados; sin ellos, este change no puede implementarse
- [ ] 1.2 Confirmar el enum de categorías (CEO, CONSEJO, DIRECTOR_N1, DIRECTOR_N2, GERENTE, MANDO_INTERMEDIO, EMPLEADO) y el acceso a la planta de la plaza desde `AvailabilityService`

## 2. Backend — capability system-settings (TDD)

- [ ] 2.1 (Red) Test de migración/persistencia: la tabla `system_settings` existe con fila única `id=1` sembrada a `MANUAL` y `CHECK (id=1)`
- [ ] 2.2 Migración `V16__system_settings.sql`: tabla de fila única (`id TINYINT PK CHECK id=1`, `approval_mode CHECK IN (MANUAL,AUTOMATIC) DEFAULT MANUAL`, `updated_by_id FK`, `updated_at`), seed `(1,'MANUAL')`
- [ ] 2.3 (Red) Tests de `SystemSettingsService.approvalMode()` (default MANUAL) y `updateApprovalMode(...)` (persiste valor + updated_by/at)
- [ ] 2.4 Dominio/entidad + puerto/adaptador de persistencia de `system-settings` (patrón hexagonal, fila única)
- [ ] 2.5 `SystemSettingsService` (green): leer y actualizar el modo global
- [ ] 2.6 (Red) Tests de controlador: `GET /admin/settings` (200 admin / 403 employee), `PUT /admin/settings` (200 admin, 400 valor inválido, 403 employee)
- [ ] 2.7 Controlador admin `GET`/`PUT /admin/settings` + DTOs + validación del valor + RBAC ADMIN
- [ ] 2.8 (Red→Green) Registro en auditoría del cambio de modo global

## 3. Backend — auto-asignación de plaza por categoría/planta (TDD)

- [ ] 3.1 (Red) Tests del algoritmo `autoAssignParkingSpace(category, date)`: categoría alta → planta más alta; categoría base → planta más baja; fallback a la siguiente planta; orden estable dentro de la planta; `null` cuando no hay ninguna plaza libre
- [ ] 3.2 Consulta de plazas LIBRES por planta reutilizando la disponibilidad consolidada de `AvailabilityService` (activa, sin asignación fija vigente/liberada, sin `APPROVED`, sin reserva de visitante), filtrando por planta = número/1000
- [ ] 3.3 `isHighCategory(category)` (CEO, CONSEJO, DIRECTOR_N1, DIRECTOR_N2) y construcción del orden de preferencia de plantas (descendente vs ascendente) sobre el rango real de plantas
- [ ] 3.4 (Green) Implementar `autoAssignParkingSpace` con fallback total y orden determinista

## 4. Backend — flujo de creación ramificado por modo (TDD)

- [ ] 4.1 (Red) Tests de `RequestService.create()` en modo `MANUAL`: comportamiento actual intacto (nace `PENDING`, `resource_id = NULL`)
- [ ] 4.2 (Red) Tests de `create()` en modo `AUTOMATIC` / PLAZA: nace `APPROVED` con plaza auto-asignada; 409 `NO_AVAILABILITY` cuando no hay ninguna plaza libre (sin crear solicitud)
- [ ] 4.3 (Red) Tests de `create()` en modo `AUTOMATIC` / PUESTO: nace `APPROVED` con el puesto elegido; 409 si el puesto no está disponible
- [ ] 4.4 (Green) Ramificar `RequestService.create()` según `approvalMode`: MANUAL sin cambios; AUTOMATIC bifurca PARKING (auto-asignación + approve en misma transacción) y DESK (approve del puesto elegido)
- [ ] 4.5 Publicar `RequestApprovedEvent` en las auto-aprobaciones (reutilizar evento existente); mantener `RequestCreatedEvent` en MANUAL
- [ ] 4.6 (Red→Green) Test de concurrencia: dos auto-asignaciones a la misma plaza/fecha → la 2ª recibe 409 por el índice único filtrado `UX_requests_space_date_approved` (índice V8 sin cambios)

## 5. Backend — rechazo posterior que libera el recurso (TDD)

- [ ] 5.1 (Red) Test de dominio `Request`: transición `APPROVED → REJECTED` permitida; `PENDING → REJECTED` sigue permitida; `REJECTED`/`CANCELLED` siguen sin admitir transición
- [ ] 5.2 (Green) Ampliar la máquina de estados/`Request.reject(...)` para aceptar rechazo desde `APPROVED` y dejar el recurso liberado
- [ ] 5.3 (Red) Test de `RequestService.reject()`: rechazar una solicitud `APPROVED` la deja `REJECTED`, el recurso vuelve a estar disponible para la fecha, y se registra en auditoría
- [ ] 5.4 (Green) Adaptar `RequestService.reject()` para no exigir `PENDING` (aceptar `PENDING` o `APPROVED`); reutilizar `RequestRejectedEvent` y el registro de auditoría
- [ ] 5.5 (Red→Green) Test de disponibilidad: tras `APPROVED → REJECTED`, la plaza reaparece en `GET /availability?date=F` (fila deja de cumplir el filtro `WHERE status='APPROVED'`)

## 6. Frontend — configuración admin y UX de solicitud (TDD)

- [ ] 6.1 (Red) Test de la pantalla admin de ajustes: muestra el modo actual y permite cambiarlo (toggle MANUAL/AUTOMATIC) contra `GET`/`PUT /admin/settings`
- [ ] 6.2 (Green) Pantalla/sección admin de configuración global del parámetro `approvalMode` con RBAC (solo ADMIN) e i18n
- [ ] 6.3 (Red) Test de la UX de solicitud del empleado en modo `AUTOMATIC`: feedback de asignación/aprobación inmediata para plaza y para puesto elegido; manejo del 409 `NO_AVAILABILITY`
- [ ] 6.4 (Green) Adaptar el flujo de solicitud del empleado para reflejar la auto-aprobación (sin pantalla de espera de ADMIN) en modo automático, conservando el flujo PENDING en modo manual

## 7. Verificación y cierre

- [ ] 7.1 Cobertura ≥80% líneas / ≥75% branches en backend y frontend; 0 violations nuevas de Sonar; complejidad cognitiva < 15 en el algoritmo de asignación (extraer métodos si procede)
- [ ] 7.2 Tests E2E (Playwright): (a) admin conmuta a AUTOMATIC; (b) empleado de categoría alta recibe plaza de planta alta; (c) empleado base recibe plaza de planta baja; (d) admin rechaza una solicitud auto-aprobada y el recurso se libera
- [ ] 7.3 `npm run lint && npm test && npm run build` y `mvn clean verify` en verde; `openspec validate request-auto-assignment` sin errores
