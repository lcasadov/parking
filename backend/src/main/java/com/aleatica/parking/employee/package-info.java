/**
 * Modulo de empleados: entidad de persistencia {@link com.aleatica.parking.employee.Employee},
 * sus enumerados de dominio ({@link com.aleatica.parking.employee.Role},
 * {@link com.aleatica.parking.employee.AuthOrigin}) y el repositorio JPA.
 *
 * <p>El alta/edicion/baja de empleados (endpoints {@code /employees}) la aporta
 * el change funcional {@code employees}; este modulo nace con lo imprescindible
 * para que {@code auth-local} pueda autenticar.</p>
 */
package com.aleatica.parking.employee;
