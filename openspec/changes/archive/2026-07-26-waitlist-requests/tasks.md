# Tasks — waitlist-requests

> Estado: pendiente de implementar. Marcar `[x]` a medida que se completa.
> Verificación backend con **JDK 21** (`JAVA_HOME=/opt/homebrew/opt/openjdk@21/...`); los ITs (Testcontainers) se ejecutan en el gate final para no saturar la máquina de desarrollo.

## 1. Backend — modelo y creación
- [x] 1.1 Migración `V27__request_waitlisted.sql`: `waitlisted BIT NOT NULL DEFAULT 0` en `dbo.requests`.
- [x] 1.2 Dominio `Request` + `RequestEntity` + `RequestMapper`: campo `waitlisted` (bool).
- [x] 1.3 `RequestCreateRequest`: campo opcional `waitlist` (default false).
- [x] 1.4 `RequestService.create`: en AUTOMÁTICO sin hueco → si `waitlist` crea `PENDING waitlisted=true`, si no `409 NO_AVAILABILITY`; en MANUAL marca `waitlisted` = (sin disponibilidad al crear).
- [x] 1.5 `RequestResponse`: exponer `waitlisted`.

## 2. Backend — motor de promoción
- [x] 2.1 Caso de uso `promoteWaitlist(date, resourceType)`: selecciona en espera por categoría desc, luego `createdAt` asc (FIFO); en AUTOMÁTICO auto-asigna el recurso liberado y aprueba; en MANUAL emite aviso a admins.
- [x] 2.2 Reutilizar la prioridad por categoría de la auto-asignación existente.
- [x] 2.3 Enganchar `promoteWaitlist` en los orígenes de liberación: cancelación de `APPROVED` (`cancel`, `adminCancel` — evento ya existe), liberación de fija (reutiliza `ReleaseAuditEvent` ya existente, sin evento nuevo), liberación administrativa por fecha.
- [x] 2.4 Promover **un** recurso liberado → **una** solicitud; apoyarse en el índice único `(resource, date)` ante concurrencia.

## 3. Backend — notificaciones
- [x] 3.1 Notificación de promoción al empleado (reutiliza `REQUEST_APPROVED`).
- [x] 3.2 Aviso a admins de liberación con cola en MANUAL (tipo nuevo `WAITLIST_AVAILABLE`).

## 4. Backend — calendario
- [x] 4.1 `MyWeekDay` + servicio my-week: exponer `waitlisted` del recurso del día (plaza y puesto).

## 5. Backend — API y tests
- [x] 5.1 `GlobalExceptionHandler`/controlador: `409 NO_AVAILABILITY` condicional al opt-in (ya condicional por construcción; javadoc actualizado).
- [x] 5.2 `docs/openapi.yaml`: `waitlist` en la creación, `waitlisted` en `Request`/`MyWeekDay`.
- [x] 5.3 Tests unitarios (Mockito, sin Docker): creación con/ sin opt-in en ambos modos; orden de promoción (categoría, FIFO); manual no auto-asigna. `BUILD SUCCESS`, 721 tests, 0 failures.
- [x] 5.4 ITs (Testcontainers, gate final, escritas pero NO ejecutadas): `WaitlistRequestsIT` — opt-in 200 vs 409; promoción end-to-end al cancelar/liberar; colas separadas plaza/puesto.

## 6. Frontend
- [x] 6.1 `types/request.ts`: `waitlisted?: boolean`; `RequestCreateRequest.waitlist?`.
- [x] 6.2 `CreateRequestModal`: banner honesto cuando 0 libres; tras `409 NO_AVAILABILITY`, ofrecer "apuntarme a la lista de espera" → reenviar con `waitlist:true`.
- [x] 6.3 `MyRequestsPage`: chip "En lista de espera" en filas `PENDING waitlisted`.
- [x] 6.4 `MyWeekPage` (orquestador): chip "En lista de espera" en el héroe/semana.
- [x] 6.5 API/hooks + MSW handlers; i18n es/en; tests (vitest); `lint` + `build`.

## 7. Gate final
- [x] 7.1 `JAVA_HOME=<jdk21> mvn -f backend/pom.xml clean verify` (BUILD SUCCESS + cobertura).
- [x] 7.2 Frontend `lint` + `test` + `build` verdes.
