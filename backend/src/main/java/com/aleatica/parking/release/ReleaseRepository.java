package com.aleatica.parking.release;

import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Adaptador de salida de persistencia de liberaciones (Spring Data JPA).
 *
 * <p>Todas las consultas se derivan del nombre del metodo (parametros vinculados,
 * sin concatenacion), eliminando la inyeccion SQL por construccion (OWASP API /
 * security-design §4). La unicidad recurso+fecha la garantiza el indice unico de la BD
 * ({@code UX_releases_space_date}); {@link #existsByParkingSpaceIdAndReleaseDate} es la
 * primera capa (UX y mensaje claro), no la red dura frente a concurrencia.</p>
 */
public interface ReleaseRepository extends JpaRepository<Release, Long> {

    /**
     * Pagina de las liberaciones de un empleado (listado "mis liberaciones").
     *
     * @param employeeId empleado propietario
     * @param pageable   pagina y orden solicitados
     * @return pagina de liberaciones del empleado
     */
    Page<Release> findByEmployeeId(Long employeeId, Pageable pageable);

    /**
     * Indica si el recurso ya tiene una liberacion para la fecha (soporte de la
     * unicidad recurso+fecha: comprobacion previa antes del alta).
     *
     * @param parkingSpaceId recurso a comprobar
     * @param releaseDate    fecha liberada
     * @return {@code true} si ya existe una liberacion para ese recurso y fecha
     */
    boolean existsByParkingSpaceIdAndReleaseDate(Long parkingSpaceId, LocalDate releaseDate);
}
