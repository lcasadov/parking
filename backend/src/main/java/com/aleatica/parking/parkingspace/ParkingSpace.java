package com.aleatica.parking.parkingspace;

import com.aleatica.parking.resource.BookableResource;
import com.aleatica.parking.resource.ResourceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Entidad de persistencia de una plaza de parking (tabla {@code dbo.parking_spaces}).
 *
 * <p>Es el recurso reservable vivo del nucleo de parking: un {@code label} unico y
 * humano (p. ej. {@code P-08}) y un indicador {@code active}. Es un adaptador de
 * salida: nunca se expone en la capa web (S4684); el controlador trabaja con DTOs.</p>
 *
 * <p>Materializa la abstraccion de dominio {@link BookableResource} con tipo
 * {@link ResourceType#PARKING}: su {@code id} es el {@code resource_id} al que apuntan
 * {@code fixed_assignments}, {@code requests} y {@code releases} tras el refactor a
 * recurso generico.</p>
 *
 * <p>{@code equals}/{@code hashCode} se apoyan en la <em>business key</em> estable
 * {@code label} (unica en BD), no en el {@code id} autogenerado.</p>
 */
@Entity
@Table(name = "parking_spaces")
public class ParkingSpace implements BookableResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "number", nullable = false)
    private Integer number;

    @Column(name = "label", nullable = false, length = 20)
    private String label;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    /** Constructor sin argumentos requerido por JPA. */
    protected ParkingSpace() {
        // JPA
    }

    /**
     * Da de alta una nueva plaza activa a partir de su {@code number} (caso de uso
     * de creacion). El {@code label} se deriva del numero y la planta queda
     * implicita como {@code number / 1000} (no se persiste, design §Decision 1).
     *
     * <p>El {@code created_at} lo fija la base de datos ({@code DEFAULT SYSUTCDATETIME()});
     * la plaza nace con {@code active = true}.</p>
     *
     * @param number numero unico de la plaza ({@code >= 1000})
     * @return la plaza nueva, aun no persistida
     */
    public static ParkingSpace create(int number) {
        ParkingSpace space = new ParkingSpace();
        space.setNumber(number);
        space.active = true;
        return space;
    }

    /**
     * Da de alta una plaza activa a partir de un {@code label} textual (fabrica de
     * compatibilidad para escenarios que aun operan por etiqueta; el numero queda
     * sin fijar). El alta/edicion por API usa {@link #create(int)}.
     *
     * @param label etiqueta unica de la plaza
     * @return la plaza nueva, aun no persistida
     */
    public static ParkingSpace create(String label) {
        ParkingSpace space = new ParkingSpace();
        space.label = label;
        space.active = true;
        return space;
    }

    public Long getId() {
        return id;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Para una plaza, el {@code resource_id} coincide con su {@code id}.</p>
     */
    @Override
    public Long getResourceId() {
        return id;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Una plaza es siempre de tipo {@link ResourceType#PARKING}.</p>
     */
    @Override
    public ResourceType getResourceType() {
        return ResourceType.PARKING;
    }

    public Integer getNumber() {
        return number;
    }

    /**
     * Fija el numero de la plaza y deriva su {@code label} textual del numero.
     *
     * @param number numero de la plaza
     */
    public void setNumber(int number) {
        this.number = number;
        this.label = Integer.toString(number);
    }

    /**
     * Planta a la que pertenece la plaza, DERIVADA del numero
     * ({@code number / 1000}); no se persiste. Devuelve {@code null} si la plaza
     * aun no tiene numero asignado.
     *
     * @return la planta derivada, o {@code null} si no hay numero
     */
    public Integer floor() {
        return number == null ? null : number / 1000;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Para una plaza la planta es la derivada del numero ({@link #floor()}).</p>
     */
    @Override
    public Integer getFloor() {
        return floor();
    }

    @Override
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ParkingSpace space)) {
            return false;
        }
        return label != null && label.equals(space.label);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(label);
    }
}
