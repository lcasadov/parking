## Why

La capa de persistencia del backend no cumple la arquitectura hexagonal que `docs/architecture.md §3` declara: los `@Entity` de JPA **son** el modelo de dominio y los servicios de aplicación dependen directamente de interfaces `JpaRepository` de Spring Data (no hay puertos de repositorio ni aislamiento del dominio). Esto acopla las reglas de negocio a Hibernate/JPA, obliga a Testcontainers para probar lógica de dominio, y contradice el ADR-04. Este cambio alinea el código con la arquitectura declarada, de forma uniforme en los 11 agregados.

## What Changes

- Separar, en cada agregado, el **modelo de dominio** (POJO/record sin framework) de la **entidad JPA de persistencia**, con un **mapper** entre ambos.
- Definir en el dominio un **puerto de repositorio** por agregado (interfaz sin framework) y un **adaptador de persistencia** que lo implementa envolviendo el `JpaRepository` de Spring Data.
- Refactorizar los **servicios de aplicación** para depender de los puertos y operar con modelos de dominio (no con entidades JPA).
- Homogeneizar la estructura `domain / application / infrastructure` en todos los módulos (hoy solo `auth` la tiene completa).
- **NO** se cambia ningún comportamiento observable: mismos endpoints, mismos contratos de API, misma semántica. Es un refactor interno.
- **Restricción dura**: se preservan intactos los **índices únicos filtrados de concurrencia** (`UX_requests_space_date_approved`, `UX_requests_desk_date_pending`, `UX_releases_space_date`, `UX_visitor_reservations_space_date`), la **traducción de violación de índice a 409** y la **semántica transaccional**.

## Capabilities

### New Capabilities
- `hexagonal-persistence`: requisito arquitectónico transversal — cada agregado del backend expone su persistencia a través de un puerto de dominio implementado por un adaptador; el modelo de dominio queda libre de anotaciones de framework; los servicios de aplicación no dependen de Spring Data ni de entidades JPA.

### Modified Capabilities
<!-- Ninguna. Este cambio NO altera requisitos funcionales ni comportamiento observable
     de ninguna capability existente (auth-local, requests, releases, etc.): los
     endpoints, contratos y reglas de negocio se preservan idénticos. Es un refactor
     de estructura interna de persistencia. -->

## Impact

- **Código backend** (`backend/src/main/java/com/aleatica/parking/**`): los 11 agregados — `employee`, `parkingspace`, `desk`, `request`, `release`, `fixedassignment`, `visitor` (+ `visitorreservation`), `audit` (`auditlog`, `loginlog`), `notification` (`emailoutbox`). ~11 entidades, 11 repositorios, 18 servicios, 16 controllers.
- **Tests** (`backend/src/test/**`, ~87 ficheros / ~15k LOC): los unitarios de servicio pasan a probar contra puertos en memoria (sin Testcontainers); los 179 tests de integración (Testcontainers) validan los adaptadores JPA de extremo a extremo.
- **Sin impacto** en: contratos de API (`docs/openapi.yaml`), esquema de BD / migraciones Flyway, frontend, seguridad, o cualquier comportamiento visible al usuario.
- **Docs**: al cerrar, `docs/architecture.md §3.3` deja de sobre-afirmar (pasa a describir la hexagonalidad real y completa de persistencia).
