/**
 * Modulo de asignaciones fijas (arquitectura hexagonal, change
 * {@code hexagonal-persistence}): el modelo de dominio
 * {@link com.aleatica.parking.fixedassignment.domain.FixedAssignment} y su puerto de salida
 * {@link com.aleatica.parking.fixedassignment.domain.FixedAssignmentRepositoryPort}, el
 * adaptador de persistencia con la entidad JPA y su repositorio Spring Data
 * ({@code com.aleatica.parking.fixedassignment.infrastructure}), los casos de uso
 * ({@link com.aleatica.parking.fixedassignment.application.FixedAssignmentService})
 * y el adaptador web
 * ({@link com.aleatica.parking.fixedassignment.FixedAssignmentController}).
 *
 * <p>Una asignacion fija es un vinculo indefinido empleado&harr;plaza por dia de la
 * semana ({@code day_of_week} 1-7), vigente hasta que el {@code ADMIN} lo revoca. La
 * revocacion es logica (nunca fisica) para preservar el historico. La unicidad
 * plaza/dia y empleado/dia entre filas activas la garantizan indices unicos filtrados
 * de la BD (409 en conflicto); la consulta del {@code EMPLOYEE} aplica verificacion de
 * pertenencia (BOLA), no solo RBAC.</p>
 */
package com.aleatica.parking.fixedassignment;
