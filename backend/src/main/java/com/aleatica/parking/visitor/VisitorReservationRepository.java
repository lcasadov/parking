package com.aleatica.parking.visitor;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Adaptador de salida de persistencia de reservas de visitante (Spring Data JPA).
 *
 * <p>Las consultas usan parametros vinculados (sin concatenacion), eliminando la
 * inyeccion SQL por construccion (OWASP API / security-design §4). La unicidad
 * plaza+fecha la garantiza el indice unico de la BD
 * ({@code UX_visitor_reservations_space_date}); {@link #existsByParkingSpaceIdAndReservationDate}
 * es la primera capa (disponibilidad y mensaje claro), no la red dura frente a
 * concurrencia.</p>
 */
public interface VisitorReservationRepository extends JpaRepository<VisitorReservation, Long> {

    /**
     * Indica si la plaza ya tiene una reserva de visitante para la fecha (soporte de la
     * comprobacion previa de disponibilidad; consolidara availability-calendar B7).
     *
     * @param parkingSpaceId plaza a comprobar
     * @param reservationDate fecha reservada
     * @return {@code true} si ya existe una reserva para esa plaza y fecha
     */
    boolean existsByParkingSpaceIdAndReservationDate(Long parkingSpaceId, LocalDate reservationDate);

    /**
     * Reservas de visitante cuyo {@code reservation_date} cae dentro del intervalo
     * (extremos inclusive).
     *
     * <p>Carga por rango para el calculo de disponibilidad puntual y el ensamblado del
     * calendario ({@code availability-calendar}): una sola consulta para todo el intervalo,
     * evitando N+1.</p>
     *
     * @param start fecha inicial del intervalo (inclusive)
     * @param end   fecha final del intervalo (inclusive)
     * @return reservas del intervalo (posiblemente vacia)
     */
    List<VisitorReservation> findByReservationDateBetween(LocalDate start, LocalDate end);

    /**
     * Busqueda paginada de reservas con filtros opcionales por fecha y plaza. Cuando un
     * parametro es {@code null} ese filtro no se aplica; ambos van vinculados.
     *
     * @param reservationDate filtro por fecha; {@code null} para no filtrar
     * @param parkingSpaceId  filtro por plaza; {@code null} para no filtrar
     * @param pageable        pagina y orden solicitados
     * @return pagina de reservas que cumplen los filtros
     */
    @Query("""
            SELECT r FROM VisitorReservation r
            WHERE (:reservationDate IS NULL OR r.reservationDate = :reservationDate)
              AND (:parkingSpaceId IS NULL OR r.parkingSpaceId = :parkingSpaceId)
            """)
    Page<VisitorReservation> search(
            @Param("reservationDate") LocalDate reservationDate,
            @Param("parkingSpaceId") Long parkingSpaceId,
            Pageable pageable);
}
