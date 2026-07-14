package com.aleatica.parking.request.infrastructure;

import com.aleatica.parking.request.domain.Request;
import com.aleatica.parking.request.domain.RequestRepositoryPort;
import com.aleatica.parking.request.domain.RequestStatus;
import com.aleatica.parking.resource.ResourceType;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

/**
 * Adaptador de persistencia del agregado {@code request} (arquitectura hexagonal, change
 * {@code hexagonal-persistence}): implementa el puerto de dominio
 * {@link RequestRepositoryPort} delegando en el repositorio Spring Data
 * {@link RequestJpaRepository} y traduciendo entidad&harr;dominio con {@link RequestMapper}.
 *
 * <p>No abre transacciones propias (design §D5): la frontera {@code @Transactional} permanece en
 * el servicio de aplicacion, de modo que {@link #saveAndFlush(Request)} vuelca dentro de la
 * transaccion del caso de uso y las violaciones de indice unico filtrado afloran como
 * {@code DataIntegrityViolationException} para traducirse a 409. La operacion {@code save} sobre
 * un modelo con {@code id} presente resulta en un {@code merge} JPA (actualizacion), preservando
 * la semantica de las transiciones de estado.</p>
 */
@Repository
public class RequestPersistenceAdapter implements RequestRepositoryPort {

    private final RequestJpaRepository jpaRepository;

    /**
     * @param jpaRepository repositorio Spring Data JPA de solicitudes
     */
    public RequestPersistenceAdapter(RequestJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Request> findById(Long id) {
        return jpaRepository.findById(id).map(RequestMapper::toDomain);
    }

    @Override
    public Request save(Request request) {
        return RequestMapper.toDomain(jpaRepository.save(RequestMapper.toEntity(request)));
    }

    @Override
    public Request saveAndFlush(Request request) {
        return RequestMapper.toDomain(jpaRepository.saveAndFlush(RequestMapper.toEntity(request)));
    }

    @Override
    public boolean existsByEmployeeIdAndResourceTypeAndRequestedDateAndStatus(
            Long employeeId, ResourceType resourceType, LocalDate requestedDate, RequestStatus status) {
        return jpaRepository.existsByEmployeeIdAndResourceTypeAndRequestedDateAndStatus(
                employeeId, resourceType, requestedDate, status);
    }

    @Override
    public Page<Request> findByEmployeeId(Long employeeId, Pageable pageable) {
        return jpaRepository.findByEmployeeId(employeeId, pageable).map(RequestMapper::toDomain);
    }

    @Override
    public Page<Request> findByEmployeeIdAndStatus(
            Long employeeId, RequestStatus status, Pageable pageable) {
        return jpaRepository.findByEmployeeIdAndStatus(employeeId, status, pageable)
                .map(RequestMapper::toDomain);
    }

    @Override
    public Page<Request> findByStatusOrderByCreatedAtAsc(RequestStatus status, Pageable pageable) {
        return jpaRepository.findByStatusOrderByCreatedAtAsc(status, pageable)
                .map(RequestMapper::toDomain);
    }

    @Override
    public Page<Request> findByStatusOrderByCreatedAtDesc(RequestStatus status, Pageable pageable) {
        return jpaRepository.findByStatusOrderByCreatedAtDesc(status, pageable)
                .map(RequestMapper::toDomain);
    }

    @Override
    public Page<Request> findAllByOrderByCreatedAtDesc(Pageable pageable) {
        return jpaRepository.findAllByOrderByCreatedAtDesc(pageable).map(RequestMapper::toDomain);
    }
}
