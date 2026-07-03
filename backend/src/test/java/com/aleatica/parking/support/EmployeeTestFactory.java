package com.aleatica.parking.support;

import com.aleatica.parking.employee.AuthOrigin;
import com.aleatica.parking.employee.Employee;
import com.aleatica.parking.employee.Role;
import java.lang.reflect.Field;
import java.time.Instant;

/**
 * Fabrica de {@link Employee} para tests: la entidad solo tiene constructor
 * protegido (requisito JPA), asi que los campos se fijan por reflexion. Mantiene
 * el setup en un unico sitio (evita duplicacion S1192 entre tests).
 */
public final class EmployeeTestFactory {

    private EmployeeTestFactory() {
        // utilidad
    }

    /**
     * Construye un empleado activo y habilitado con el hash dado.
     *
     * @param id           id del empleado
     * @param login        login
     * @param email        email
     * @param passwordHash hash BCrypt (puede ser {@code null})
     * @param role         rol
     * @return el empleado construido
     */
    public static Employee active(Long id, String login, String email, String passwordHash, Role role) {
        Employee e = new Employee() {
            // subclase anonima solo para acceder al constructor protegido
        };
        set(e, "id", id);
        set(e, "firstName", "Test");
        set(e, "lastName", "User");
        set(e, "login", login);
        set(e, "email", email);
        set(e, "passwordHash", passwordHash);
        set(e, "role", role);
        set(e, "authOrigin", AuthOrigin.LOCAL);
        set(e, "enabled", true);
        set(e, "active", true);
        set(e, "failedLoginAttempts", 0);
        set(e, "passwordMustChange", false);
        set(e, "createdAt", Instant.parse("2026-01-01T00:00:00Z"));
        return e;
    }

    /**
     * Fija un campo por reflexion.
     *
     * @param target objeto destino
     * @param name   nombre del campo
     * @param value  valor a asignar
     */
    public static void set(Object target, String name, Object value) {
        try {
            Field field = Employee.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("No se pudo fijar el campo " + name, ex);
        }
    }
}
