/**
 * Modulo de visitantes: las entidades de persistencia
 * {@link com.aleatica.parking.visitor.Visitor} y
 * {@link com.aleatica.parking.visitor.VisitorReservation}, sus repositorios JPA, los casos
 * de uso ({@link com.aleatica.parking.visitor.application.VisitorService} y
 * {@link com.aleatica.parking.visitor.application.VisitorReservationService}) y los
 * adaptadores web ({@link com.aleatica.parking.visitor.VisitorController} y
 * {@link com.aleatica.parking.visitor.VisitorReservationController}).
 *
 * <p>Una ficha {@code Visitor} representa a una persona externa (sin cuenta ni acceso a la
 * app); su {@code nationalId} es la clave natural unica (409 en colision, garantizada por
 * {@code UX_visitors_national_id}). Una {@code VisitorReservation} ocupa una plaza para una
 * fecha, dejandola NO disponible ese dia (solo plazas, nunca puestos). La disponibilidad se
 * comprueba transaccionalmente (plaza inactiva, asignacion fija no liberada, solicitud
 * APPROVED u otra reserva) y la unicidad plaza+fecha la garantiza
 * {@code UX_visitor_reservations_space_date} (409, incluida la concurrencia). Editar la
 * ficha afecta solo a futuras reservas (la reserva referencia al visitante por id, sin
 * denormalizar). La anulacion es un borrado fisico de la fila futura ({@code ClockPort}).
 * Todo el modulo esta reservado al {@code ADMIN}. La auditoria se dispara
 * {@code AFTER_COMMIT} via un puerto de log (consolidacion de {@code audit-retention}, B10).
 * Alimenta {@code availability-calendar} (B7).</p>
 */
package com.aleatica.parking.visitor;
