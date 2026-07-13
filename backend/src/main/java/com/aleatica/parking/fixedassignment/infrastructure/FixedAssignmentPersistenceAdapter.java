package com.aleatica.parking.fixedassignment.infrastructure;

import com.aleatica.parking.fixedassignment.domain.FixedAssignment;
import com.aleatica.parking.fixedassignment.domain.FixedAssignmentRepositoryPort;
import com.aleatica.parking.resource.ResourceType;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

/**
 * Adaptador de persistencia del agregado {@code fixedassignment} (arquitectura hexagonal,
 * change {@code hexagonal-persistence}): implementa el puerto de dominio
 * {@link FixedAssignmentRepositoryPort} delegando en el repositorio Spring Data
 * {@link FixedAssignmentJpaRepository} y traduciendo entidad&harr;dominio con
 * {@link FixedAssignmentMapper}.
 *
 * <p>No abre transacciones propias (design §D5): la frontera {@code @Transactional} permanece en
 * el servicio de aplicacion, de modo que {@link #saveAllAndFlush(List)} vuelca dentro de la
 * transaccion del caso de uso y la violacion de los indices unicos filtrados plaza/dia y
 * empleado/dia aflora como {@code DataIntegrityViolationException} para traducirse a 409.</p>
 */
@Repository
public class FixedAssignmentPersistenceAdapter implements FixedAssignmentRepositoryPort {

    private final FixedAssignmentJpaRepository jpaRepository;

    /**
     * @param jpaRepository repositorio Spring Data JPA de asignaciones fijas
     */
    public FixedAssignmentPersistenceAdapter(FixedAssignmentJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Page<FixedAssignment> findByActiveTrue(Pageable pageable) {
        return jpaRepository.findByActiveTrue(pageable).map(FixedAssignmentMapper::toDomain);
    }

    @Override
    public List<FixedAssignment> findByEmployeeIdAndActiveTrueOrderByDayOfWeekAsc(Long employeeId) {
        return jpaRepository.findByEmployeeIdAndActiveTrueOrderByDayOfWeekAsc(employeeId).stream()
                .map(FixedAssignmentMapper::toDomain)
                .toList();
    }

    @Override
    public List<FixedAssignment> findByEmployeeIdAndResourceTypeAndActiveTrueOrderByDayOfWeekAsc(
            Long employeeId, ResourceType resourceType) {
        return jpaRepository
                .findByEmployeeIdAndResourceTypeAndActiveTrueOrderByDayOfWeekAsc(employeeId, resourceType)
                .stream()
                .map(FixedAssignmentMapper::toDomain)
                .toList();
    }

    @Override
    public List<FixedAssignment> findByEmployeeIdAndResourceIdAndResourceTypeAndActiveTrue(
            Long employeeId, Long resourceId, ResourceType resourceType) {
        return jpaRepository
                .findByEmployeeIdAndResourceIdAndResourceTypeAndActiveTrue(
                        employeeId, resourceId, resourceType)
                .stream()
                .map(FixedAssignmentMapper::toDomain)
                .toList();
    }

    @Override
    public List<FixedAssignment> saveAll(List<FixedAssignment> assignments) {
        return jpaRepository.saveAll(assignments.stream().map(FixedAssignmentMapper::toEntity).toList())
                .stream()
                .map(FixedAssignmentMapper::toDomain)
                .toList();
    }

    @Override
    public List<FixedAssignment> saveAllAndFlush(List<FixedAssignment> assignments) {
        return jpaRepository
                .saveAllAndFlush(assignments.stream().map(FixedAssignmentMapper::toEntity).toList())
                .stream()
                .map(FixedAssignmentMapper::toDomain)
                .toList();
    }
}
