package com.aleatica.parking.visitor;

import com.aleatica.parking.resource.ResourceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Entidad de persistencia de una reserva de plaza para un visitante
 * (tabla {@code dbo.visitor_reservations}).
 *
 * <p>Ocupa una {@code ParkingSpace} para una {@code reservation_date}, dejandola NO
 * disponible ese dia en el calculo de disponibilidad (solo plazas, nunca puestos;
 * data-model §3.7). No tiene estados ni flujo de aprobacion: la crea directamente el
 * {@code ADMIN}. La anulacion es un borrado fisico de la fila futura.</p>
 *
 * <p>Es un adaptador de salida: nunca se expone en la capa web (S4684). Las referencias
 * a otras tablas se guardan como identificadores ({@code Long}) en lugar de
 * {@code @ManyToOne}, evitando por diseno consultas N+1 y manteniendo el agregado
 * desacoplado.</p>
 */
@Entity
@Table(name = "visitor_reservations")
public class VisitorReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "visitor_id", nullable = false)
    private Long visitorId;

    // Recurso reservado (plaza o puesto): polimórfico por tipo + id (patrón de V12 en
    // `requests`). Reemplaza la antigua `parking_space_id` para permitir reservar puestos.
    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 16)
    private ResourceType resourceType;

    @Column(name = "resource_id", nullable = false)
    private Long resourceId;

    @Column(name = "reservation_date", nullable = false)
    private LocalDate reservationDate;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "created_by_id", nullable = false)
    private Long createdById;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** Constructor sin argumentos requerido por JPA. */
    protected VisitorReservation() {
        // JPA
    }

    /**
     * Da de alta una reserva de un recurso (plaza o puesto) para un visitante en una fecha.
     *
     * @param visitorId       visitante para el que se reserva
     * @param resourceType    tipo de recurso reservado ({@code PARKING}/{@code DESK})
     * @param resourceId      identificador del recurso ocupado por la reserva
     * @param reservationDate fecha reservada
     * @param notes           anotaciones opcionales; {@code null} si no aplica
     * @param createdById     {@code ADMIN} que crea la reserva
     * @param now             instante de creacion (UTC)
     * @return la reserva nueva, aun no persistida
     */
    public static VisitorReservation create(
            Long visitorId, ResourceType resourceType, Long resourceId, LocalDate reservationDate,
            String notes, Long createdById, Instant now) {
        VisitorReservation reservation = new VisitorReservation();
        reservation.visitorId = visitorId;
        reservation.resourceType = resourceType;
        reservation.resourceId = resourceId;
        reservation.reservationDate = reservationDate;
        reservation.notes = notes;
        reservation.createdById = createdById;
        reservation.createdAt = now;
        return reservation;
    }

    public Long getId() {
        return id;
    }

    public Long getVisitorId() {
        return visitorId;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public LocalDate getReservationDate() {
        return reservationDate;
    }

    public String getNotes() {
        return notes;
    }

    public Long getCreatedById() {
        return createdById;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof VisitorReservation reservation)) {
            return false;
        }
        return id != null && id.equals(reservation.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
