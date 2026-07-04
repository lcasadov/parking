package com.aleatica.parking.audit.application;

import com.aleatica.parking.audit.LoginLogRepository;
import com.aleatica.parking.audit.dto.LoginLogEntryResponse;
import com.aleatica.parking.auth.domain.LoginResult;
import com.aleatica.parking.employee.dto.PageResponse;
import java.time.Instant;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso de consulta del registro de logins, reservado al rol {@code ADMIN}.
 *
 * <p>Arquitectura hexagonal: la logica no depende de la web y nunca devuelve entidades JPA
 * (convierte a DTO antes de salir, OWASP API3). Valida la ventana temporal
 * ({@code from <= to}); el valor de {@code result} fuera del enum lo rechaza el binding del
 * controlador (400). La restriccion de rol la aplica el controlador ({@code @PreAuthorize}).</p>
 */
@Service
public class LoginLogQueryService {

    private static final String MSG_INVALID_WINDOW =
            "La fecha 'from' no puede ser posterior a 'to'";

    private final LoginLogRepository loginLogRepository;

    /**
     * @param loginLogRepository repositorio del registro de logins
     */
    public LoginLogQueryService(LoginLogRepository loginLogRepository) {
        this.loginLogRepository = loginLogRepository;
    }

    /**
     * Consulta paginada del registro de logins con filtros por resultado y ventana.
     *
     * @param result   resultado a filtrar; {@code null} para no filtrar
     * @param from     limite inferior de {@code occurred_at}; {@code null} para no filtrar
     * @param to       limite superior de {@code occurred_at}; {@code null} para no filtrar
     * @param pageable pagina, tamano y orden solicitados
     * @return pagina de intentos de login (DTO) con sus metadatos
     * @throws InvalidDateRangeException si {@code from} es posterior a {@code to}
     */
    @Transactional(readOnly = true)
    public PageResponse<LoginLogEntryResponse> list(
            LoginResult result, Instant from, Instant to, Pageable pageable) {
        requireValidWindow(from, to);
        return PageResponse.from(
                loginLogRepository.search(result, from, to, pageable),
                LoginLogEntryResponse::from);
    }

    private static void requireValidWindow(Instant from, Instant to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidDateRangeException(MSG_INVALID_WINDOW);
        }
    }
}
