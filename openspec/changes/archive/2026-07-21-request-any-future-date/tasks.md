# Tasks — request-any-future-date

## 1. Backend
- [x] 1.1 Quitar el límite superior en `RequestService.requireDateWithinWindow` (solo rechazar fecha < hoy)
- [x] 1.2 Ajustar constantes/mensajes/Javadoc (`WINDOW_DAYS`, MSG, OutsideRequestWindowException)
- [x] 1.3 Actualizar tests backend (unit + IT) al nuevo rango

## 2. Frontend
- [ ] 2.1 Quitar el tope `max` (hoy+14) del selector de fecha en `CreateRequestModal`
- [ ] 2.2 Permitir navegar a cualquier fecha futura en `FloorPlanDatebar` (y revisar `useCalendar`)
- [ ] 2.3 Actualizar `utils/requests.ts` (maxRequestDateIso/REQUEST_WINDOW_DAYS) y textos i18n es/en
- [ ] 2.4 Actualizar tests frontend afectados

## 3. Spec / Verificación
- [ ] 3.1 Requirement y escenarios de la ventana corregidos (delta MODIFIED)
- [ ] 3.2 Quality gate: `mvn clean verify` + `npm run lint && npm run build && npm run test` verdes
