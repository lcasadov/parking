package com.aleatica.parking.audit;

import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Adaptador de salida de persistencia del rastro de auditoria (Spring Data JPA).
 *
 * <p>La consulta usa parametros vinculados null-tolerantes (sin concatenacion),
 * eliminando la inyeccion SQL por construccion (OWASP API / security-design §4).
 * El orden lo aporta el {@link Pageable} (por defecto {@code occurred_at} descendente).</p>
 */
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * Busqueda paginada del rastro de auditoria por actor, accion y ventana temporal.
     *
     * <p>Cada filtro es opcional: cuando su parametro es {@code null}, ese criterio no
     * se aplica. La ventana temporal es inclusiva en ambos extremos.</p>
     *
     * @param actorEmployeeId actor a filtrar; {@code null} para no filtrar
     * @param action          accion a filtrar (coincidencia exacta); {@code null} para no filtrar
     * @param from            limite inferior de {@code occurred_at} (inclusivo); {@code null} para no filtrar
     * @param to              limite superior de {@code occurred_at} (inclusivo); {@code null} para no filtrar
     * @param pageable        pagina, tamano y orden solicitados
     * @return pagina de entradas que cumplen los filtros
     */
    @Query("""
            SELECT a FROM AuditLog a
            WHERE (:actorEmployeeId IS NULL OR a.actorEmployeeId = :actorEmployeeId)
              AND (:action IS NULL OR a.action = :action)
              AND (:from IS NULL OR a.occurredAt >= :from)
              AND (:to IS NULL OR a.occurredAt <= :to)
            """)
    Page<AuditLog> search(
            @Param("actorEmployeeId") Long actorEmployeeId,
            @Param("action") String action,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);
}
