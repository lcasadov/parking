package com.aleatica.parking.visitor;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Entidad de persistencia de un vehiculo de un visitante (tabla {@code dbo.visitor_vehicles}).
 *
 * <p>Relacion 1:N con {@link Visitor} por {@code visitor_id}: un visitante puede tener varios
 * vehiculos. La matricula ({@code license_plate}) es obligatoria y unica por visitante; marca,
 * modelo y color son opcionales. Es un adaptador de salida: nunca se expone en la capa web
 * (S4684); el controlador trabaja con DTOs.</p>
 */
@Entity
@Table(name = "visitor_vehicles")
public class VisitorVehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "visitor_id", nullable = false)
    private Long visitorId;

    @Column(name = "license_plate", nullable = false, length = 15)
    private String licensePlate;

    @Column(name = "brand", length = 60)
    private String brand;

    @Column(name = "model", length = 60)
    private String model;

    @Column(name = "color", length = 30)
    private String color;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected VisitorVehicle() {
        // Requerido por JPA.
    }

    /**
     * Crea un vehiculo nuevo para un visitante. La matricula ya debe venir normalizada
     * (trim + mayusculas) por el servicio; marca/modelo/color pueden ser {@code null}.
     */
    public static VisitorVehicle create(
            Long visitorId, String licensePlate, String brand, String model, String color) {
        VisitorVehicle vehicle = new VisitorVehicle();
        vehicle.visitorId = visitorId;
        vehicle.licensePlate = licensePlate;
        vehicle.brand = brand;
        vehicle.model = model;
        vehicle.color = color;
        return vehicle;
    }

    /** Aplica los cambios de una edicion (matricula ya normalizada por el servicio). */
    public void update(String licensePlate, String brand, String model, String color) {
        this.licensePlate = licensePlate;
        this.brand = brand;
        this.model = model;
        this.color = color;
    }

    public Long getId() {
        return id;
    }

    public Long getVisitorId() {
        return visitorId;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public String getBrand() {
        return brand;
    }

    public String getModel() {
        return model;
    }

    public String getColor() {
        return color;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
