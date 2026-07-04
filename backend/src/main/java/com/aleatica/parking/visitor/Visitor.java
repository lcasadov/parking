package com.aleatica.parking.visitor;

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
 * Entidad de persistencia de una ficha de visitante (tabla {@code dbo.visitors}).
 *
 * <p>Representa a una persona externa (no empleada) sin cuenta ni acceso a la app. Su
 * {@code national_id} (DNI/pasaporte) es la clave natural unica para reusar la ficha
 * entre visitas (data-model §3.6). Los datos de la ficha se editan sin reescribir las
 * reservas ya creadas: {@code VisitorReservation} referencia al visitante por id y no
 * denormaliza sus campos, por lo que una edicion nunca es retroactiva.</p>
 *
 * <p>Es un adaptador de salida: nunca se expone en la capa web (S4684); el controlador
 * trabaja con DTOs. La referencia al {@code ADMIN} creador se guarda como identificador
 * ({@code Long}) en lugar de {@code @ManyToOne}, evitando por diseno consultas N+1.</p>
 */
@Entity
@Table(name = "visitors")
public class Visitor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 150)
    private String lastName;

    @Column(name = "national_id", nullable = false, length = 20)
    private String nationalId;

    @Column(name = "license_plate", length = 15)
    private String licensePlate;

    @Column(name = "company", length = 150)
    private String company;

    @Column(name = "usual_reason", length = 255)
    private String usualReason;

    @Column(name = "created_by_id", nullable = false)
    private Long createdById;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    /** Constructor sin argumentos requerido por JPA. */
    protected Visitor() {
        // JPA
    }

    /**
     * Da de alta una ficha de visitante nueva (caso de uso de creacion).
     *
     * <p>El {@code created_at} lo fija la base de datos ({@code DEFAULT SYSUTCDATETIME()}).</p>
     *
     * @param firstName    nombre (obligatorio)
     * @param lastName     apellidos (obligatorio)
     * @param nationalId   documento de identidad, clave natural unica (obligatorio)
     * @param licensePlate matricula; {@code null} si no aplica
     * @param company      empresa; {@code null} si no aplica
     * @param usualReason  motivo habitual de visita; {@code null} si no aplica
     * @param createdById  {@code ADMIN} que crea la ficha
     * @return la ficha nueva, aun no persistida
     */
    public static Visitor create(
            String firstName, String lastName, String nationalId, String licensePlate,
            String company, String usualReason, Long createdById) {
        Visitor visitor = new Visitor();
        visitor.firstName = firstName;
        visitor.lastName = lastName;
        visitor.nationalId = nationalId;
        visitor.licensePlate = licensePlate;
        visitor.company = company;
        visitor.usualReason = usualReason;
        visitor.createdById = createdById;
        return visitor;
    }

    /**
     * Actualiza los datos editables de la ficha (el cambio afecta solo a futuras reservas,
     * ya que la reserva referencia al visitante por id y no denormaliza sus campos).
     *
     * @param firstName    nombre (obligatorio)
     * @param lastName     apellidos (obligatorio)
     * @param nationalId   documento de identidad, clave natural unica (obligatorio)
     * @param licensePlate matricula; {@code null} si no aplica
     * @param company      empresa; {@code null} si no aplica
     * @param usualReason  motivo habitual de visita; {@code null} si no aplica
     */
    public void update(
            String firstName, String lastName, String nationalId, String licensePlate,
            String company, String usualReason) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.nationalId = nationalId;
        this.licensePlate = licensePlate;
        this.company = company;
        this.usualReason = usualReason;
    }

    public Long getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getNationalId() {
        return nationalId;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public String getCompany() {
        return company;
    }

    public String getUsualReason() {
        return usualReason;
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
        if (!(other instanceof Visitor visitor)) {
            return false;
        }
        return id != null && id.equals(visitor.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
