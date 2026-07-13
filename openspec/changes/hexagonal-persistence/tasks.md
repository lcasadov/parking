# Tasks — hexagonal-persistence

> Ejecución por agregado (D7). Tras CADA agregado: `mvn verify` (unitarios + IT Testcontainers) en verde antes de pasar al siguiente. No mergear a `develop` hasta validación completa.
>
> **ALCANCE ACOTADO (decidido 2026-07-12):** tras validar el coste/riesgo real con la plantilla
> `request`, se acota la opción B a los agregados con **dominio rico + invariantes de concurrencia
> que poseen su persistencia**: `request` ✅, `release`, `fixedassignment`. Los módulos CRUD
> (`desk`, `parkingspace`, `employee`, `visitor`, `auditlog`, `loginlog`, `emailoutbox`) se dejan
> como Spring pragmático (entidad JPA = dominio) — best practice para CRUD. Grupos 5–10 quedan
> **fuera de alcance** en este change.

## 1. Andamiaje transversal

- [x] 1.1 Añadir dependencia de test `com.tngtech.archunit:archunit-junit5` en `backend/pom.xml` (scope test)
- [x] 1.2 Crear `HexagonalArchitectureTest`: `..domain..` sin `jakarta.persistence`/`org.hibernate`/`org.springframework.data`; `..application..` sin `JpaRepository` ni entidades JPA (inicialmente acotado a los paquetes migrados)
- [x] 1.3 Definir la convención de paquetes objetivo (`domain`/`application`/`infrastructure`) y documentarla en un `package-info.java` de referencia

## 2. Agregado `request` (plantilla de referencia — dominio más rico)

- [x] 2.1 Modelo de dominio `request.domain.Request` (con transiciones de estado) + enums de dominio + `RequestRepositoryPort`
- [x] 2.2 Infraestructura: entidad JPA `request.infrastructure.RequestEntity` + `RequestJpaRepository` (Spring Data) + `RequestPersistenceAdapter` (implementa el puerto) + mapper entidad↔dominio
- [x] 2.3 Refactor `RequestService` para depender del puerto y operar con dominio; mapeo dominio↔DTO en application; `@Transactional` permanece en el servicio
- [x] 2.4 Verificar índice `UX_requests_space_date_approved`/`UX_requests_desk_date_pending` → 409 y `ConcurrencyRetry` intactos
- [x] 2.5 Tests unitarios de servicio contra un fake del puerto (sin Testcontainers) + mapper; `mvn verify` verde; ampliar ArchUnit a `request`

## 3. Agregado `release`

- [x] 3.1 Dominio `Release` + `ReleaseRepositoryPort`
- [x] 3.2 Infraestructura: `ReleaseEntity` + `ReleaseJpaRepository` + adaptador + mapper
- [x] 3.3 Refactor `ReleaseService`; preservar `UX_releases_space_date` → 409
- [x] 3.4 `mvn verify` verde; ArchUnit ampliado

## 4. Agregado `fixedassignment`

- [x] 4.1 Dominio `FixedAssignment` + `FixedAssignmentRepositoryPort`
- [x] 4.2 Infraestructura: entidad + JpaRepository + adaptador + mapper
- [x] 4.3 Refactor `FixedAssignmentService` (set semanal por empleado)
- [x] 4.4 `mvn verify` verde; ArchUnit ampliado

## 5. Agregado `desk`

- [ ] 5.1 Dominio `Desk` + `DeskRepositoryPort` (conservar `DeskResourceResolver`)
- [ ] 5.2 Infraestructura: entidad + JpaRepository + adaptador + mapper
- [ ] 5.3 Refactor `DeskService` (incl. activación dedicada)
- [ ] 5.4 `mvn verify` verde; ArchUnit ampliado

## 6. Agregado `parkingspace`

- [ ] 6.1 Dominio `ParkingSpace` + `ParkingSpaceRepositoryPort` (conservar `ParkingSpaceResourceResolver`)
- [ ] 6.2 Infraestructura: entidad + JpaRepository + adaptador + mapper
- [ ] 6.3 Refactor `ParkingSpaceService` (incl. `configure` alta masiva)
- [ ] 6.4 `mvn verify` verde; ArchUnit ampliado

## 7. Agregado `employee`

- [ ] 7.1 Dominio `Employee` + `EmployeeRepositoryPort`
- [ ] 7.2 Infraestructura: entidad + JpaRepository + adaptador + mapper
- [ ] 7.3 Refactor `EmployeeService` + integración con `auth` (login por `EmployeeRepository`)
- [ ] 7.4 `mvn verify` verde; ArchUnit ampliado

## 8. Agregado `visitor` + `visitorreservation`

- [ ] 8.1 Dominio `Visitor` y `VisitorReservation` + puertos respectivos
- [ ] 8.2 Infraestructura: entidades + JpaRepositories + adaptadores + mappers
- [ ] 8.3 Refactor servicios; preservar `UX_visitor_reservations_space_date` → 409
- [ ] 8.4 `mvn verify` verde; ArchUnit ampliado

## 9. Agregados `auditlog` y `loginlog`

- [ ] 9.1 Dominio `AuditLog`, `LoginLog` + puertos; reconciliar con `LoginLogRecorder`/`JdbcLoginLogRecorder` existentes
- [ ] 9.2 Infraestructura: entidades + JpaRepositories + adaptadores + mappers
- [ ] 9.3 Refactor consultas de auditoría/accesos + purga (`RetentionPurgePort` ya existe)
- [ ] 9.4 `mvn verify` verde; ArchUnit ampliado

## 10. Agregado `emailoutbox`

- [ ] 10.1 Dominio `EmailOutbox` + `EmailOutboxRepositoryPort`
- [ ] 10.2 Infraestructura: entidad + JpaRepository + adaptador + mapper
- [ ] 10.3 Refactor `NotificationDeliveryService`/`PendingEmailStore` (reintento outbox); `EmailSenderPort` ya existe
- [ ] 10.4 `mvn verify` verde; ArchUnit ampliado

## 11. Cierre (alcance acotado: request · release · fixedassignment)

- [x] 11.1 `HexagonalArchitectureTest` cubriendo los agregados migrados (request, release, fixedassignment); los CRUD quedan fuera por decisión de alcance
- [x] 11.2 `mvn verify` verde tras cada agregado (unitarios + 177 IT Testcontainers, 0 fallos)
- [x] 11.3 Actualizar `docs/architecture.md §3.3` (hexagonalidad de persistencia real: por complejidad, no uniforme)
- [x] 11.4 Revisión de tests: comportamiento preservado (0 regresiones); contratos de API intactos
