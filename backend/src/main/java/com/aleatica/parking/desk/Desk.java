package com.aleatica.parking.desk;

import com.aleatica.parking.resource.BookableResource;
import com.aleatica.parking.resource.ResourceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Entidad de persistencia de un puesto de oficina (tabla {@code dbo.desks}).
 *
 * <p>Segundo recurso reservable del sistema: un {@code number} unico en el rango 1-65,
 * una {@link DeskCategory}, coordenadas relativas ({@code coord_x}/{@code coord_y} como
 * porcentaje 0-100 del plano) y un indicador {@code active}. Es un adaptador de salida:
 * nunca se expone en la capa web (S4684); el controlador trabaja con DTOs.</p>
 *
 * <p>Materializa la abstraccion de dominio {@link BookableResource} con tipo
 * {@link ResourceType#DESK}: su {@code id} es el {@code resource_id} al que apuntan
 * {@code fixed_assignments}, {@code requests} y {@code releases} cuando
 * {@code resource_type = DESK}, reutilizando integramente el ciclo de reserva.</p>
 *
 * <p>{@code equals}/{@code hashCode} se apoyan en la <em>business key</em> estable
 * {@code number} (unica en BD), no en el {@code id} autogenerado.</p>
 */
@Entity
@Table(name = "desks")
public class Desk implements BookableResource {

    private static final String LABEL_FORMAT = "D-%02d";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "number", nullable = false)
    private Integer number;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 15)
    private DeskCategory category;

    @Column(name = "coord_x", nullable = false, precision = 5, scale = 2)
    private BigDecimal coordX;

    @Column(name = "coord_y", nullable = false, precision = 5, scale = 2)
    private BigDecimal coordY;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    /** Constructor sin argumentos requerido por JPA. */
    protected Desk() {
        // JPA
    }

    /**
     * Da de alta un nuevo puesto activo (caso de uso de creacion).
     *
     * <p>El {@code created_at} lo fija la base de datos ({@code DEFAULT SYSUTCDATETIME()});
     * el puesto nace con {@code active = true}.</p>
     *
     * @param number   numero unico del puesto (1-65)
     * @param category categoria del puesto
     * @param coordX   coordenada X (porcentaje 0-100)
     * @param coordY   coordenada Y (porcentaje 0-100)
     * @return el puesto nuevo, aun no persistido
     */
    public static Desk create(Integer number, DeskCategory category, BigDecimal coordX, BigDecimal coordY) {
        Desk desk = new Desk();
        desk.number = number;
        desk.category = category;
        desk.coordX = coordX;
        desk.coordY = coordY;
        desk.active = true;
        return desk;
    }

    public Long getId() {
        return id;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Para un puesto, el {@code resource_id} coincide con su {@code id}.</p>
     */
    @Override
    public Long getResourceId() {
        return id;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Un puesto es siempre de tipo {@link ResourceType#DESK}.</p>
     */
    @Override
    public ResourceType getResourceType() {
        return ResourceType.DESK;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Etiqueta humana derivada del numero (p. ej. {@code D-05}).</p>
     */
    @Override
    public String getLabel() {
        return number == null ? null : LABEL_FORMAT.formatted(number);
    }

    public Integer getNumber() {
        return number;
    }

    public DeskCategory getCategory() {
        return category;
    }

    public void setCategory(DeskCategory category) {
        this.category = category;
    }

    public BigDecimal getCoordX() {
        return coordX;
    }

    public void setCoordX(BigDecimal coordX) {
        this.coordX = coordX;
    }

    public BigDecimal getCoordY() {
        return coordY;
    }

    public void setCoordY(BigDecimal coordY) {
        this.coordY = coordY;
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
        if (!(other instanceof Desk desk)) {
            return false;
        }
        return number != null && number.equals(desk.number);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(number);
    }
}
