package com.aleatica.parking.employee;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.Objects;

/**
 * Entidad de persistencia de un empleado (tabla {@code dbo.employees}).
 *
 * <p>Concentra la identidad, las credenciales locales (Fase 1 / fallback) y el
 * estado de bloqueo por intentos fallidos. Es un adaptador de salida: nunca se
 * expone en la capa web (S4684); el controlador trabaja con DTOs.</p>
 *
 * <p>{@code equals}/{@code hashCode} se apoyan en la <em>business key</em>
 * estable {@code login} (unica en BD), no en el {@code id} autogenerado.</p>
 */
@Entity
@Table(name = "employees")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 150)
    private String lastName;

    @Column(name = "login", nullable = false, length = 100)
    private String login;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "password_hash", length = 72)
    private String passwordHash;

    @Column(name = "password_must_change", nullable = false)
    private boolean passwordMustChange;

    @Column(name = "department", length = 100)
    private String department;

    @Column(name = "mobile_phone", length = 30)
    private String mobilePhone;

    @Column(name = "license_plate", length = 15)
    private String licensePlate;

    @Column(name = "is_corporate", nullable = false)
    private boolean corporate;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_origin", nullable = false, length = 10)
    private AuthOrigin authOrigin = AuthOrigin.LOCAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 10)
    private Role role;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "locked_until")
    private Instant lockedUntil;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "last_password_change_at")
    private Instant lastPasswordChangeAt;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "updated_at")
    private Instant updatedAt;

    /** Constructor sin argumentos requerido por JPA. */
    protected Employee() {
        // JPA
    }

    /**
     * Da de alta un nuevo empleado activo y habilitado (caso de uso de creacion).
     *
     * <p>Fija la identidad inmutable ({@code login}) y los atributos autoritativos;
     * los campos opcionales (departamento, telefono, matricula, corporativo) se
     * asignan despues via setters. Nace con {@code active = true},
     * {@code enabled = true} y sin credencial local (Fase 2 la obtiene via reset).</p>
     *
     * @param firstName  nombre
     * @param lastName   apellidos
     * @param login      login unico (inmutable)
     * @param email      email unico
     * @param role       rol funcional
     * @param authOrigin origen de autenticacion
     * @return el empleado nuevo, aun no persistido
     */
    public static Employee register(
            String firstName, String lastName, String login,
            String email, Role role, AuthOrigin authOrigin) {
        Employee employee = new Employee();
        employee.firstName = firstName;
        employee.lastName = lastName;
        employee.login = login;
        employee.email = email;
        employee.role = role;
        employee.authOrigin = authOrigin;
        employee.enabled = true;
        employee.active = true;
        return employee;
    }

    /**
     * Indica si la cuenta esta bloqueada en un instante dado.
     *
     * @param now instante de referencia (UTC)
     * @return {@code true} si {@code lockedUntil} es posterior a {@code now}
     */
    public boolean isLockedAt(Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    /**
     * Indica si la cuenta puede iniciar sesion (no dada de baja ni deshabilitada).
     *
     * @return {@code true} si {@code active} y {@code enabled}
     */
    public boolean isLoginAllowed() {
        return active && enabled;
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

    public String getLogin() {
        return login;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public boolean isPasswordMustChange() {
        return passwordMustChange;
    }

    public void setPasswordMustChange(boolean passwordMustChange) {
        this.passwordMustChange = passwordMustChange;
    }

    public AuthOrigin getAuthOrigin() {
        return authOrigin;
    }

    public Role getRole() {
        return role;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isActive() {
        return active;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public void setFailedLoginAttempts(int failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(Instant lockedUntil) {
        this.lockedUntil = lockedUntil;
    }

    public Instant getLastPasswordChangeAt() {
        return lastPasswordChangeAt;
    }

    public void setLastPasswordChangeAt(Instant lastPasswordChangeAt) {
        this.lastPasswordChangeAt = lastPasswordChangeAt;
    }

    public String getDepartment() {
        return department;
    }

    public String getMobilePhone() {
        return mobilePhone;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public boolean isCorporate() {
        return corporate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public void setMobilePhone(String mobilePhone) {
        this.mobilePhone = mobilePhone;
    }

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }

    public void setCorporate(boolean corporate) {
        this.corporate = corporate;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Employee employee)) {
            return false;
        }
        return login != null && login.equals(employee.login);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(login);
    }
}
