package com.aleatica.parking.audit;

import com.aleatica.parking.auth.domain.LoginResult;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Adaptador de salida de persistencia del registro de logins (Spring Data JPA).
 *
 * <p>La consulta usa parametros vinculados null-tolerantes (sin concatenacion),
 * eliminando la inyeccion SQL por construccion (OWASP API / security-design §4).
 * El orden lo aporta el {@link Pageable} (por defecto {@code occurred_at} descendente).</p>
 */
public interface LoginLogRepository extends JpaRepository<LoginLog, Long> {

    /**
     * Busqueda paginada del registro de logins por resultado y ventana temporal.
     *
     * <p>Cada filtro es opcional: cuando su parametro es {@code null}, ese criterio no
     * se aplica. La ventana temporal es inclusiva en ambos extremos.</p>
     *
     * @param result   resultado a filtrar; {@code null} para no filtrar
     * @param from     limite inferior de {@code occurred_at} (inclusivo); {@code null} para no filtrar
     * @param to       limite superior de {@code occurred_at} (inclusivo); {@code null} para no filtrar
     * @param pageable pagina, tamano y orden solicitados
     * @return pagina de intentos que cumplen los filtros
     */
    @Query("""
            SELECT l FROM LoginLog l
            WHERE (:result IS NULL OR l.result = :result)
              AND (:from IS NULL OR l.occurredAt >= :from)
              AND (:to IS NULL OR l.occurredAt <= :to)
            """)
    Page<LoginLog> search(
            @Param("result") LoginResult result,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);
}
