package com.aleatica.parking.parkingspace;

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
 * <p>{@code equals}/{@code hashCode} se apoyan en la <em>business key</em> estable
 * {@code label} (unica en BD), no en el {@code id} autogenerado.</p>
 */
@Entity
@Table(name = "parking_spaces")
public class ParkingSpace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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
     * Da de alta una nueva plaza activa (caso de uso de creacion).
     *
     * <p>El {@code created_at} lo fija la base de datos ({@code DEFAULT SYSUTCDATETIME()});
     * la plaza nace con {@code active = true}.</p>
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

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

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
