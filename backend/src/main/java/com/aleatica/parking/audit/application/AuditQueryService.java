package com.aleatica.parking.audit.application;

import com.aleatica.parking.audit.AuditLogRepository;
import com.aleatica.parking.audit.dto.AuditLogEntryResponse;
import com.aleatica.parking.employee.dto.PageResponse;
import java.time.Instant;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso de consulta del rastro de auditoria funcional, reservado al rol {@code ADMIN}.
 *
 * <p>Arquitectura hexagonal: la logica no depende de la web y nunca devuelve entidades JPA
 * (convierte a DTO antes de salir, OWASP API3). Valida la ventana temporal
 * ({@code from <= to}) antes de consultar; la restriccion de rol la aplica el controlador
 * ({@code @PreAuthorize}).</p>
 */
@Service
public class AuditQueryService {

    private static final String MSG_INVALID_WINDOW =
            "La fecha 'from' no puede ser posterior a 'to'";

    private final AuditLogRepository auditLogRepository;

    /**
     * @param auditLogRepository repositorio del rastro de auditoria
     */
    public AuditQueryService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Consulta paginada del rastro de auditoria con filtros por actor, accion y ventana.
     *
     * @param actorEmployeeId actor a filtrar; {@code null} para no filtrar
     * @param action          accion a filtrar; {@code null} para no filtrar
     * @param from            limite inferior de {@code occurred_at}; {@code null} para no filtrar
     * @param to              limite superior de {@code occurred_at}; {@code null} para no filtrar
     * @param pageable        pagina, tamano y orden solicitados
     * @return pagina de entradas de auditoria (DTO) con sus metadatos
     * @throws InvalidDateRangeException si {@code from} es posterior a {@code to}
     */
    @Transactional(readOnly = true)
    public PageResponse<AuditLogEntryResponse> list(
            Long actorEmployeeId, String action, Instant from, Instant to, Pageable pageable) {
        requireValidWindow(from, to);
        return PageResponse.from(
                auditLogRepository.search(actorEmployeeId, action, from, to, pageable),
                AuditLogEntryResponse::from);
    }

    private static void requireValidWindow(Instant from, Instant to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidDateRangeException(MSG_INVALID_WINDOW);
        }
    }
}
