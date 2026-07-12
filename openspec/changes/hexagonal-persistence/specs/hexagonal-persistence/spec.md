# Spec delta — hexagonal-persistence

## ADDED Requirements

### Requirement: El modelo de dominio está libre de framework de persistencia
Cada agregado del backend SHALL exponer un modelo de dominio (POJO o `record`) sin anotaciones de JPA/Hibernate ni dependencias de `jakarta.persistence` ni de Spring Data. La entidad JPA de persistencia SHALL ser una clase separada del modelo de dominio.

#### Scenario: El modelo de dominio no importa framework de persistencia
- **WHEN** se inspecciona una clase del modelo de dominio de cualquier agregado (p. ej. `request.domain.Request`)
- **THEN** no contiene anotaciones `@Entity`/`@Table`/`@Id`/`@Column` ni imports de `jakarta.persistence`, `org.hibernate` o `org.springframework.data`

#### Scenario: Existe una entidad JPA separada por agregado
- **WHEN** se revisa el paquete de infraestructura de un agregado
- **THEN** existe una entidad `@Entity` distinta del modelo de dominio, con un mapper que convierte entre ambos en ambos sentidos

### Requirement: Cada agregado expone un puerto de repositorio de dominio
Cada agregado SHALL definir en su capa de dominio una interfaz de puerto de repositorio (sin framework) que declare las operaciones de persistencia que necesita su lógica de negocio, y SHALL existir un adaptador que la implemente envolviendo el `JpaRepository` de Spring Data.

#### Scenario: Puerto de repositorio en el dominio
- **WHEN** se revisa el dominio de un agregado (p. ej. `request.domain`)
- **THEN** existe una interfaz `RequestRepositoryPort` (o equivalente) que devuelve y acepta modelos de dominio, no entidades JPA

#### Scenario: Adaptador de persistencia implementa el puerto
- **WHEN** se revisa la infraestructura del agregado
- **THEN** existe un adaptador (`@Component`/`@Repository`) que implementa el puerto, delega en el `JpaRepository` y traduce entidad↔dominio vía el mapper

### Requirement: Los servicios de aplicación dependen de puertos, no de Spring Data
Los servicios de la capa de aplicación SHALL depender únicamente de puertos de dominio y operar con modelos de dominio. NO SHALL inyectar interfaces `JpaRepository` ni manipular entidades JPA directamente.

#### Scenario: El servicio no conoce Spring Data
- **WHEN** se inspecciona un servicio de aplicación (p. ej. `RequestService`)
- **THEN** sus dependencias inyectadas son puertos de dominio y sus parámetros/retornos son modelos de dominio o DTOs, sin referencias a `JpaRepository` ni a la entidad JPA

### Requirement: Se preservan las garantías de concurrencia y transaccionalidad
El refactor SHALL preservar intactas las garantías de concurrencia y transaccionalidad existentes: los índices únicos filtrados (`UX_requests_space_date_approved`, `UX_requests_desk_date_pending`, `UX_releases_space_date`, `UX_visitor_reservations_space_date`), la traducción de violación de índice único a HTTP 409, el reintento aplicativo (`ConcurrencyRetry`) y los límites de transacción de cada caso de uso.

#### Scenario: Colisión de aprobación concurrente sigue devolviendo 409
- **WHEN** dos administradores aprueban solicitudes que asignarían la misma plaza en la misma fecha de forma concurrente
- **THEN** una tiene éxito y la otra recibe HTTP 409 (igual que antes del refactor)

#### Scenario: La suite de integración sigue en verde
- **WHEN** se ejecuta `mvn verify` (179 tests de integración con Testcontainers + unitarios)
- **THEN** todos pasan sin cambios de comportamiento observable

### Requirement: El comportamiento observable no cambia
El refactor SHALL ser transparente para los consumidores: mismos endpoints, mismos contratos de API (`docs/openapi.yaml`), mismos códigos de estado y cuerpos de respuesta, mismo esquema de base de datos y migraciones.

#### Scenario: Contrato de API inalterado
- **WHEN** se comparan las respuestas de la API antes y después del refactor para las mismas peticiones
- **THEN** son idénticas (status, headers relevantes y cuerpo)
