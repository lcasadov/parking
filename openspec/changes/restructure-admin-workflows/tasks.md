# Tasks — restructure-admin-workflows

Orden por fases del design (F1 quick wins solo-UI → F2 asignación puntual + Ocupación accionable → F3 generalización DESK → F4 AGENCIA). Cada tarea debe pasar el Quality Gate (lint/tests/build sin errores, cobertura ≥80%/75%).

## 1. Fase 1 — Quick wins solo-frontend (sin cambios de backend)

- [x] 1.1 Reorganizar la navegación del ADMIN a 9 destinos en grupos Operativa/Gestión (`AdminLayout.tsx`, `paths.ts`, `AppRoutes.tsx`)
- [x] 1.2 Reorganizar el portal del EMPLOYEE a 4 destinos con "Mi Semana" como ruta índice (`EmployeeLayout.tsx`, `AppRoutes.tsx`)
- [x] 1.3 Fusionar "Plazas" + "Puestos" en el destino "Recursos" con pestañas, montando los componentes de página existentes
- [x] 1.4 Fusionar "Auditoría" + "Accesos" en el destino "Registros" con pestañas
- [x] 1.5 Fusionar "Mis asignaciones fijas" + "Mis liberaciones" en "Mis plazas" con pestañas
- [x] 1.6 Añadir el conmutador de tipo de recurso (plaza/puesto) en la vista de disponibilidad y pasar `resourceType` a `GET /availability`
- [x] 1.7 Mostrar la etiqueta/número de negocio (no el id interno) en disponibilidad y en "Mis asignaciones fijas"
- [x] 1.8 Corregir la liberación de recurso fijo para que envíe el `resourceType` correcto (bug: puesto liberado como PARKING) en `MyFixedAssignmentsPage`/`ReleaseResourceModal`
- [x] 1.9 Indicar en el modal de solicitud, en modo MANUAL, que el puesto elegido es una "preferencia" (no reserva garantizada) — `CreateRequestModal`
- [x] 1.10 Añadir confirmación antes de crear una solicitud de puesto desde el plano (`FloorPlanPage`, click y lista móvil)
- [x] 1.11 Actualizar rutas viejas → nuevas (redirecciones) y ajustar selectores de tests E2E afectados
- [x] 1.12 Ejecutar Quality Gate frontend (`npm run lint && npm test && npm run build`) y corregir hallazgos

## 2. Fase 2 — Asignación puntual del admin (backend nuevo) + Ocupación accionable

- [x] 2.1 Definir el contrato del endpoint de asignación puntual en `docs/openapi.yaml` (`POST /requests/admin`: `{ employeeId, requestedDate, resourceType, resourceId? }` → 201 `Request` APPROVED; 409 conflicto/NO_AVAILABILITY; 403 no-admin)
- [x] 2.2 Implementar el servicio de asignación puntual reutilizando la validación de disponibilidad y la auto-asignación por categoría/planta; crear `Request` `APPROVED` con `resolved_by_id` = admin
- [x] 2.3 Registrar la asignación puntual en `audit_log` con el admin como actor
- [x] 2.4 Tests backend del endpoint (happy path plaza/puesto, auto-asignación, 409 ocupado, 409 NO_AVAILABILITY, 403 EMPLOYEE) con cobertura ≥80%/75%
- [x] 2.5 Convertir la rejilla de "Ocupación" (fusión de calendario semanal + disponibilidad) en accionable: celda `FREE` → asignar; celda ocupada → liberar
- [x] 2.6 Asignación inline fija: precargar `GET /fixed-assignments/employee/{id}` y reenviar el conjunto de días con `PUT /fixed-assignments/employee/{id}` (evitar borrado accidental de días)
- [x] 2.7 Asignación inline puntual: invocar el endpoint nuevo (2.1) desde la celda para una fecha concreta
- [x] 2.8 Liberación inline desde celda ocupada (liberación administrativa o admin-cancel según origen) con manejo de 409 en contexto
- [x] 2.9 Tests frontend de la Ocupación accionable (asignar fija, asignar puntual, liberar, conflicto 409)

## 3. Fase 3 — Generalización PARKING/DESK en las vistas

- [x] 3.1 Extender `GET /calendar/admin` con `resourceType` (default PARKING) para servir la vista semanal de puestos; actualizar `docs/openapi.yaml`
- [x] 3.2 Tests backend del calendario semanal de puestos (estados y titular por día)
- [x] 3.3 Añadir el conmutador plaza/puesto a la vista semanal de "Ocupación" (frontend)
- [x] 3.4 Extender `MyWeekDay`/`GET /calendar/my-week` para llevar el estado de ambos recursos por día; actualizar `docs/openapi.yaml`
- [x] 3.5 Actualizar "Mi Semana" (frontend) para mostrar plaza y puesto por día y resolver acciones para ambos (fin del "sin plaza" engañoso)
- [x] 3.6 Tests frontend de "Mi Semana" multi-recurso

## 4. Fase 4 — Ampliación del rol AGENCIA

- [x] 4.1 Dar a AGENCIA acceso al destino "Liberar" con ambos pivotes (por-empleado y por-fecha) — rutas y autorización frontend
- [x] 4.2 Ampliar la autorización backend de AGENCIA al pivote por-fecha y a la vista de ocupación de solo lectura, manteniendo fail-closed en `/releases`, `/releases/mine`, `DELETE /releases/{id}`
- [x] 4.3 Añadir a AGENCIA el historial de sus propias liberaciones administrativas (vista de solo lectura)
- [x] 4.4 Tests de autorización de AGENCIA (permitido: pivotes de liberación, ocupación read-only, historial propio; denegado: endpoints del portal de empleado)
- [x] 4.5 Documentar el rol AGENCIA en `README.md` (glosario y matriz de permisos)

## 5. Cierre

- [x] 5.1 Actualizar `docs/ux-flows.md` y `docs/ui-screens.md` con la nueva IA y los flujos accionables
- [x] 5.2 Verificación adversarial (build + tests + lint + probes de auth/boundary) sin regresiones ni violations Sonar nuevas
- [x] 5.3 Reality-check de los user journeys clave (admin asigna puntual desde Ocupación; empleado ve plaza+puesto en Mi Semana; AGENCIA libera por fecha)
- [x] 5.4 Resolver las Open Questions del design con negocio (RESUELTO 2026-07-23: email con plantilla propia; sin límite de ventana; asignación puntual solo ADMIN)

## 6. Follow-ups derivados de las decisiones

- [x] 6.1 Notificación por email al empleado en la asignación puntual, con plantilla propia y envío resiliente (email_outbox), disparada desde `RequestService.adminAssign`
- [x] 6.2 Lectura EMPLOYEE-safe del número/label de plaza (`GET /parking-spaces/{id}`) y uso en "Mis asignaciones fijas" para mostrar "P-08" en vez de "Plaza fija" (simétrico al patrón de puestos)

## 7. Recurso distinto por día en la asignación fija (rejilla + modal)

- [x] 7.1 Corregir `mergeFixedAssignmentDays` para emparejar por `resourceId` + `resourceType` (usar los días del recurso de la propia celda, no consolidar por tipo)
- [ ] 7.2 Tests de la rejilla: asignar un recurso a un día preserva otros días con recurso distinto; conflicto mismo-tipo-mismo-día → 409
- [x] 7.3 Rediseñar el modal de empleado (`EmployeeFormModal`) para permitir un mapa día→recurso por tipo (no un único recurso por tipo); prefill correcto cuando ya hay recursos distintos por día
- [ ] 7.4 Tests del modal: configurar y guardar días con recursos distintos; prefill que no colapsa a un recurso por tipo
- [ ] 7.5 Actualizar `docs/ui-screens.md` (modal de empleado) reflejando la selección por día
