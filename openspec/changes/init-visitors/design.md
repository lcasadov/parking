# Design: init-visitors

## Context
Los visitantes son personas externas a ALEATICA que no acceden a la aplicación.
El modelo separa **`Visitor`** (ficha reutilizable, dato vivo) de
**`VisitorReservation`** (instancia puntual, dato histórico) para permitir reusar
la ficha entre visitas sin duplicar datos personales. Solo el `ADMIN` opera sobre
ambas. Arquitectura hexagonal: la regla de disponibilidad vive en el dominio y no
depende de Spring.

## Goals
- CRUD de fichas con unicidad de `nationalId` para reuso fiable.
- Reservas que ocupan una plaza una fecha, integradas en el cálculo de
  disponibilidad como una condición más (solo plazas).
- Anulación segura: solo reservas futuras; edición de ficha sin reescribir
  historia.

## Decisions
- **Dos entidades** (`Visitor` / `VisitorReservation`): la ficha es reutilizable y
  no se purga; la reserva es histórica y se purga a 2 años. Separarlas evita
  duplicar PII y permite buscar por `nationalId`/`licensePlate`.
- **Unicidad `nationalId`**: índice único no filtrado `UX_visitors_national_id`
  (columna `NOT NULL`, global). Colisión → 409, validada por la BD (no solo en
  lectura) para resistir concurrencia.
- **Ocupación de plaza**: la creación de reserva valida disponibilidad
  transaccionalmente; el índice `IX_visitor_reservations_parking_space_id_date`
  soporta la consulta. Conflicto de disponibilidad → 409.
- **Solo plazas**: `VisitorReservation` referencia `parking_space_id`; nunca se
  asocia a `Desk` (los puestos no admiten reservas de visitante).
- **Sin estados ni email**: no hay máquina de estados ni evento de dominio hacia
  `notifications`; el admin crea/anula directamente.
- **Edición no destructiva**: `updateVisitor` modifica la ficha viva; las reservas
  pasadas conservan su contexto de creación (el cambio aplica solo a futuras).
- **Reloj inyectable** (`ClockPort`): para decidir "futura vs pasada" en
  `cancelVisitorReservation` y testear la ventana.

## Risks
- **Carrera de doble reserva** sobre la misma plaza/fecha → mitigada validando la
  disponibilidad dentro de la transacción y devolviendo 409 a la perdedora.
- **Reuso erróneo de ficha** por `nationalId` mal tecleado → mitigado con búsqueda
  previa por `nationalId`/`firstName`/`lastName`/`licensePlate` antes de crear.
- **Fuga de PII**: solo `ADMIN` accede; los empleados reciben 403. La purga a 2
  años aplica a las reservas, no a las fichas (dato vivo).

## Migration Plan
- Flyway: tablas `visitors` y `visitor_reservations` con sus FKs e índices
  (`UX_visitors_national_id`, `IX_visitor_reservations_parking_space_id_date`).
  Ver `docs/data-model.md` §3.6, §3.7 y §"Indexes".
- Sin migración de datos (capability nueva).
- La purga de `visitor_reservations` (`reservation_date < cutoffDate`, diaria) la
  implementa `audit-retention`; aquí solo se crean las tablas.
