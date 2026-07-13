## Context

Hoy la persistencia del backend es un layering clásico acoplado a JPA: los `@Entity` (`Request`, `Desk`, `Employee`…) son el modelo de dominio, los `*Repository` extienden `JpaRepository` de Spring Data, y los servicios de aplicación los inyectan directamente. Solo `auth` tiene una capa `domain/` con puertos (`ClockPort`) y adaptadores (`SystemClockAdapter`). Ya existen puertos para infraestructura transversal (email, export, auditoría, resolución de recurso, purga), así que el patrón es conocido en el repo; falta llevarlo a la persistencia de forma uniforme.

Restricciones duras: no cambia el esquema de BD ni las migraciones Flyway; se preservan los índices únicos filtrados de concurrencia, la traducción de `DataIntegrityViolationException` → 409 (`GlobalExceptionHandler`), el reintento aplicativo (`ConcurrencyRetry`) y las fronteras `@Transactional`. No cambia ningún contrato de API.

## Goals / Non-Goals

**Goals:**
- Modelo de dominio libre de framework + entidad JPA separada + mapper, por agregado.
- Puerto de repositorio en el dominio + adaptador de persistencia que envuelve Spring Data, por agregado.
- Servicios de aplicación dependientes de puertos y de modelos de dominio.
- Estructura `domain / application / infrastructure` homogénea en los 11 agregados.
- Reglas de arquitectura verificadas automáticamente (ArchUnit).

**Non-Goals:**
- Cambiar comportamiento, contratos de API, esquema de BD o migraciones.
- Refactorizar el frontend (no es hexagonal ni lo será aquí).
- Tocar los puertos de infraestructura ya existentes (clock, email, export, audit, resource, purge) salvo reubicarlos si procede.
- Introducir CQRS, event sourcing u otros patrones más allá de puertos-y-adaptadores.

## Decisions

### D1 — Layout por agregado
`<módulo>/domain` (modelo de dominio, puerto de repositorio, enums/value objects y excepciones de dominio) · `<módulo>/application` (servicio/casos de uso, excepciones de aplicación, mapeo dominio↔DTO) · `<módulo>/infrastructure` (entidad `@Entity`, interfaz `JpaRepository`, adaptador que implementa el puerto, mapper entidad↔dominio) · el `*Controller` y los `dto/` permanecen como adaptador web. Alternativa descartada: un único paquete plano (el actual) — no aísla el dominio.

### D2 — Mappers a mano, sin MapStruct
El mapeo entidad↔dominio y dominio↔DTO se escribe a mano (métodos estáticos `from(...)`/`toDomain(...)`, en línea con el estilo actual `CurrentUser.from(...)`). **Alternativa descartada: MapStruct** — añade dependencia + procesador de anotaciones y código generado; el mapeo aquí es plano y no lo justifica.

### D3 — Puerto de repositorio a medida del caso de uso
Cada puerto declara solo lo que su servicio necesita (`findById(Long): Optional<Domain>`, `save(Domain): Domain`, finders específicos que devuelven **modelos de dominio**). Los métodos derivados y `@Query` de Spring Data viven en la interfaz `JpaRepository` **dentro del adaptador**; el adaptador traduce a dominio. Alternativa descartada: exponer un puerto genérico tipo `CrudPort<T>` — filtra semántica de framework y no captura las consultas de negocio.

### D4 — Paginación: se conserva `Pageable`/`Page` de Spring en la frontera
`Pageable` y `Page<T>` son value types estables y ligeros; el puerto acepta `Pageable` y devuelve `Page<Domain>` (el adaptador mapea `Page<Entity>` → `Page<Domain>` con `.map(...)`). Alternativa descartada: un `PageResult` propio — reinventa sin ganancia y obliga a mapear en cada controller.

### D5 — Concurrencia y transacciones: sin cambios de mecanismo
Los índices únicos filtrados siguen en la BD; el `save` del adaptador sigue lanzando `DataIntegrityViolationException`, que `GlobalExceptionHandler` sigue traduciendo a 409; `@Transactional` permanece en los métodos del servicio de aplicación (la frontera transaccional NO baja al adaptador). `ConcurrencyRetry` envuelve el mismo caso de uso. Es el punto de mayor riesgo → se valida con los IT de concurrencia tras cada agregado.

### D6 — Enforcement con ArchUnit
Se añade una dependencia de test `com.tngtech.archunit` y una clase `HexagonalArchitectureTest` que verifica: (1) las clases de `..domain..` no dependen de `jakarta.persistence`, `org.hibernate` ni `org.springframework.data`; (2) los servicios de `..application..` no dependen de `JpaRepository` ni de las entidades JPA. Esto hace ejecutables los escenarios del spec. Alternativa descartada: solo revisión manual — no evita la regresión futura.

### D7 — Fasing por agregado, `request` como plantilla de referencia
Se implementa primero `request` (dominio más rico: máquina de estados + ventana + concurrencia), que fija el patrón; luego se replica en `release`, `fixedassignment`, `desk`, `parkingspace`, `employee`, `visitor`+`visitorreservation`, `auditlog`, `loginlog`, `emailoutbox`. Tras cada agregado se ejecuta `mvn verify` (unitarios + IT). Alternativa descartada: big-bang de los 11 a la vez — regresión ingobernable.

## Risks / Trade-offs

- **Regresión de semántica de persistencia (lazy loading, cascadas, dirty checking)** → Mitigación: mapper explícito por agregado + los 179 IT con Testcontainers ejecutados tras cada agregado; no se cambian las migraciones.
- **Romper una garantía de concurrencia** → Mitigación: no se toca el mecanismo (D5); los IT de concurrencia de `request`/`release`/`visitor` son gate por agregado.
- **Boilerplate y superficie de código nueva (11× dominio+entidad+mapper+puerto+adaptador)** → Trade-off aceptado: es el coste inherente de la hexagonalidad completa (decisión del stakeholder). Los mappers se cubren con tests unitarios; se revisa que la cobertura ≥80/75 se mantenga.
- **Esfuerzo alto (~35–55 días-persona) y multi-PR** → Mitigación: entrega por agregado; cada agregado puede ir en su propio commit/PR verificado. No se mergea a `develop` hasta validación completa.
- **`@Transactional` mal ubicado tras mover el `save` al adaptador** → Mitigación: la anotación se mantiene en el servicio; test de integración que cubra rollback.

## Migration Plan

1. Añadir dependencia de test ArchUnit + `HexagonalArchitectureTest` (inicialmente restringida a los agregados ya migrados).
2. Por cada agregado (empezando por `request`): crear modelo de dominio + puerto + entidad JPA de infraestructura + mapper + adaptador; refactorizar el servicio y el controller para usar dominio; ejecutar `mvn verify`; ampliar ArchUnit al paquete migrado.
3. Reordenar imports/paquetes y `package-info.java`.
4. Al completar los 11: ArchUnit cubre todo `com.aleatica.parking`; actualizar `docs/architecture.md §3.3`.
5. **Rollback**: al ir por agregado, revertir el commit del agregado afectado si un IT falla y no se estabiliza; el resto queda intacto.

## Open Questions

- ¿Ubicar los puertos de infraestructura ya existentes (`ClockPort`, `EmailSenderPort`, …) bajo el mismo esquema `infrastructure/` por consistencia, o dejarlos donde están? (Propuesta: dejarlos; están fuera del alcance de persistencia.)
- ¿Modelo de dominio como `record` inmutable o clase mutable? (Propuesta: `record` donde el agregado no requiera mutación in-place; clase con métodos de negocio donde haya transiciones de estado, p. ej. `Request`.)
