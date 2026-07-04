package com.aleatica.parking.exception;

import com.aleatica.parking.auth.application.AuthenticationFailedException;
import com.aleatica.parking.auth.application.InvalidCurrentPasswordException;
import com.aleatica.parking.auth.application.PasswordPolicyException;
import com.aleatica.parking.fixedassignment.application.InvalidDayOfWeekException;
import com.aleatica.parking.request.application.DuplicatePendingRequestException;
import com.aleatica.parking.request.application.OutsideRequestWindowException;
import com.aleatica.parking.request.application.RejectionReasonRequiredException;
import com.aleatica.parking.request.application.RequestStateException;
import com.aleatica.parking.request.application.SpaceUnavailableException;
import jakarta.persistence.EntityNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Manejador global de errores de la API.
 *
 * <p>Centraliza la traduccion de excepciones a respuestas HTTP con el cuerpo
 * uniforme {@link ApiError}, evitando {@code try/catch} dispersos (S6813/S112).
 * Nunca devuelve stack traces ni detalles internos al cliente (OWASP API8 /
 * logging seguro); el detalle se registra en el log del servidor.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String CODE_VALIDATION = "VALIDATION_ERROR";
    private static final String CODE_FORBIDDEN = "FORBIDDEN";
    private static final String CODE_NOT_FOUND = "NOT_FOUND";
    private static final String CODE_NOT_IMPLEMENTED = "NOT_IMPLEMENTED";
    private static final String CODE_INTERNAL = "INTERNAL_ERROR";
    private static final String CODE_UNAUTHORIZED = "UNAUTHORIZED";
    private static final String CODE_PASSWORD_POLICY = "PASSWORD_POLICY_VIOLATION";
    private static final String CODE_CONFLICT = "CONFLICT";
    private static final String CODE_OUTSIDE_WINDOW = "OUTSIDE_REQUEST_WINDOW";
    private static final String CODE_REQUEST_PENDING = "REQUEST_ALREADY_PENDING";
    private static final String CODE_SPACE_UNAVAILABLE = "SPACE_NOT_AVAILABLE";

    private static final String FIELD_NEW_PASSWORD = "newPassword";
    private static final String FIELD_CURRENT_PASSWORD = "currentPassword";
    private static final String FIELD_LOGIN = "login";
    private static final String FIELD_EMAIL = "email";
    private static final String FIELD_LABEL = "label";
    private static final String FIELD_DAYS_OF_WEEK = "daysOfWeek";
    private static final String FIELD_PARKING_SPACE_ID = "parkingSpaceId";
    private static final String FIELD_EMPLOYEE_ID = "employeeId";
    private static final String FIELD_REQUESTED_DATE = "requestedDate";
    private static final String FIELD_REJECTION_REASON = "rejectionReason";

    private static final String INDEX_LOGIN = "ux_employees_login";
    private static final String INDEX_EMAIL = "ux_employees_email";
    private static final String INDEX_LABEL = "ux_parking_spaces_label";
    private static final String INDEX_FIXED_SPACE_DAY = "ux_fixed_assignments_space_day_active";
    private static final String INDEX_FIXED_EMPLOYEE_DAY = "ux_fixed_assignments_employee_day_active";
    private static final String INDEX_REQUEST_PENDING = "ux_requests_employee_date_pending";
    private static final String INDEX_REQUEST_APPROVED = "ux_requests_space_date_approved";

    private static final String MSG_VALIDATION = "La solicitud contiene datos invalidos";
    private static final String MSG_FORBIDDEN = "No tiene permisos para realizar esta operacion";
    private static final String MSG_NOT_FOUND = "Recurso no encontrado";
    private static final String MSG_INTERNAL = "Se ha producido un error interno";
    private static final String MSG_CONFLICT = "El recurso ya existe o viola una restriccion de unicidad";
    private static final String MSG_LOGIN_TAKEN = "El login ya esta en uso";
    private static final String MSG_EMAIL_TAKEN = "El email ya esta en uso";
    private static final String MSG_LABEL_TAKEN = "La etiqueta ya esta en uso";
    private static final String MSG_SPACE_DAY_TAKEN =
            "La plaza ya esta asignada a otro empleado ese dia de la semana";
    private static final String MSG_EMPLOYEE_DAY_TAKEN =
            "El empleado ya tiene un recurso asignado ese dia de la semana";
    private static final String MSG_REQUEST_PENDING_TAKEN =
            "Ya existe una solicitud pendiente para esa fecha";
    private static final String MSG_SPACE_APPROVED_TAKEN =
            "La plaza ya esta asignada a otra solicitud aprobada esa fecha";

    /**
     * Regla de traduccion de una violacion de indice unico de BD (por el fragmento del
     * nombre del indice) al cuerpo {@link ApiError}: campo en conflicto (o {@code null}),
     * mensaje y codigo de error de negocio.
     *
     * @param indexFragment fragmento del nombre del indice (en minusculas)
     * @param field         campo en conflicto; {@code null} si no aplica
     * @param message       mensaje legible por humanos
     * @param code          codigo de error de negocio
     */
    private record IndexRule(String indexFragment, String field, String message, String code) {
    }

    private static final List<IndexRule> INDEX_RULES = List.of(
            new IndexRule(INDEX_LOGIN, FIELD_LOGIN, MSG_LOGIN_TAKEN, CODE_CONFLICT),
            new IndexRule(INDEX_EMAIL, FIELD_EMAIL, MSG_EMAIL_TAKEN, CODE_CONFLICT),
            new IndexRule(INDEX_LABEL, FIELD_LABEL, MSG_LABEL_TAKEN, CODE_CONFLICT),
            new IndexRule(INDEX_FIXED_SPACE_DAY, FIELD_PARKING_SPACE_ID, MSG_SPACE_DAY_TAKEN, CODE_CONFLICT),
            new IndexRule(INDEX_FIXED_EMPLOYEE_DAY, FIELD_EMPLOYEE_ID, MSG_EMPLOYEE_DAY_TAKEN, CODE_CONFLICT),
            new IndexRule(INDEX_REQUEST_PENDING, null, MSG_REQUEST_PENDING_TAKEN, CODE_REQUEST_PENDING),
            new IndexRule(INDEX_REQUEST_APPROVED, FIELD_PARKING_SPACE_ID, MSG_SPACE_APPROVED_TAKEN,
                    CODE_SPACE_UNAVAILABLE));

    /**
     * Traduce errores de validacion de DTO de entrada a {@code 400 Bad Request}.
     *
     * @param ex excepcion de validacion lanzada por {@code @Valid}
     * @return {@link ApiError} con el detalle por campo y estado 400
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fields.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiError.of(CODE_VALIDATION, MSG_VALIDATION, fields));
    }

    /**
     * Traduce fallos de autorizacion a {@code 403 Forbidden} (fail closed).
     *
     * @param ex excepcion de acceso denegado
     * @return {@link ApiError} con estado 403
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
        LOG.warn("Acceso denegado: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiError.of(CODE_FORBIDDEN, MSG_FORBIDDEN));
    }

    /**
     * Traduce la ausencia de una entidad a {@code 404 Not Found}.
     *
     * <p>El cliente recibe un mensaje generico para no filtrar detalle interno
     * (OWASP API8); el mensaje real se registra en el log del servidor.</p>
     *
     * @param ex excepcion de entidad no encontrada
     * @return {@link ApiError} generico con estado 404
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(EntityNotFoundException ex) {
        LOG.warn("Recurso no encontrado: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.of(CODE_NOT_FOUND, MSG_NOT_FOUND));
    }

    /**
     * Traduce funcionalidad pendiente a {@code 501 Not Implemented}.
     *
     * @param ex excepcion de funcionalidad no implementada
     * @return {@link ApiError} con estado 501
     */
    @ExceptionHandler(NotImplementedException.class)
    public ResponseEntity<ApiError> handleNotImplemented(NotImplementedException ex) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(ApiError.of(CODE_NOT_IMPLEMENTED, ex.getMessage()));
    }

    /**
     * Traduce un fallo de autenticacion local a {@code 401}.
     *
     * <p>El mensaje es generico (no revela si el login existe): el detalle fino
     * solo viaja a {@code login_log} (security-design §2, OWASP API2).</p>
     *
     * @param ex excepcion de autenticacion fallida
     * @return {@link ApiError} generico con estado 401
     */
    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<ApiError> handleAuthenticationFailed(AuthenticationFailedException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiError.of(CODE_UNAUTHORIZED, ex.getMessage()));
    }

    /**
     * Traduce el incumplimiento de la politica de contrasena a {@code 400} con
     * el detalle de las violaciones en el campo {@code newPassword}.
     *
     * @param ex excepcion con la lista de violaciones de politica
     * @return {@link ApiError} con estado 400 y detalle por campo
     */
    @ExceptionHandler(PasswordPolicyException.class)
    public ResponseEntity<ApiError> handlePasswordPolicy(PasswordPolicyException ex) {
        Map<String, String> fields =
                Map.of(FIELD_NEW_PASSWORD, String.join("; ", ex.getViolations()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiError.of(CODE_PASSWORD_POLICY, ex.getMessage(), fields));
    }

    /**
     * Traduce una {@code currentPassword} incorrecta en el cambio de contrasena a
     * {@code 400} con el detalle en el campo {@code currentPassword}.
     *
     * @param ex excepcion de contrasena actual invalida
     * @return {@link ApiError} con estado 400 y detalle por campo
     */
    @ExceptionHandler(InvalidCurrentPasswordException.class)
    public ResponseEntity<ApiError> handleInvalidCurrentPassword(InvalidCurrentPasswordException ex) {
        Map<String, String> fields = Map.of(FIELD_CURRENT_PASSWORD, ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiError.of(CODE_VALIDATION, ex.getMessage(), fields));
    }

    /**
     * Traduce un dia de la semana fuera del rango 1-7 (verificacion del caso de uso)
     * a {@code 400} con el detalle en el campo {@code daysOfWeek}.
     *
     * @param ex excepcion de dia de la semana invalido
     * @return {@link ApiError} con estado 400 y detalle por campo
     */
    @ExceptionHandler(InvalidDayOfWeekException.class)
    public ResponseEntity<ApiError> handleInvalidDayOfWeek(InvalidDayOfWeekException ex) {
        Map<String, String> fields = Map.of(FIELD_DAYS_OF_WEEK, ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiError.of(CODE_VALIDATION, ex.getMessage(), fields));
    }

    /**
     * Traduce una colision de unicidad detectada en el caso de uso a {@code 409}
     * con el campo en conflicto (comprobacion previa, mensaje claro). Es unico para
     * todos los modulos: cada uno aporta su subtipo de {@link FieldConflictException}.
     *
     * @param ex excepcion de conflicto de campo unico
     * @return {@link ApiError} con estado 409 y detalle por campo
     */
    @ExceptionHandler(FieldConflictException.class)
    public ResponseEntity<ApiError> handleFieldConflict(FieldConflictException ex) {
        Map<String, String> fields = Map.of(ex.getField(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of(CODE_CONFLICT, ex.getMessage(), fields));
    }

    /**
     * Traduce una violacion de indice unico en BD a {@code 409} (red dura frente a
     * concurrencia). Deriva el campo en conflicto del nombre del indice
     * ({@code UX_employees_login} / {@code UX_employees_email}).
     *
     * @param ex excepcion de violacion de integridad de datos
     * @return {@link ApiError} con estado 409 y, si se identifica, el campo en conflicto
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex) {
        LOG.warn("Violacion de integridad: {}", ex.getMostSpecificCause().getMessage());
        String detail = ex.getMostSpecificCause().getMessage();
        String lowerDetail = detail == null ? "" : detail.toLowerCase(Locale.ROOT);
        for (IndexRule rule : INDEX_RULES) {
            if (lowerDetail.contains(rule.indexFragment())) {
                Map<String, String> fields =
                        rule.field() == null ? null : Map.of(rule.field(), rule.message());
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ApiError.of(rule.code(), rule.message(), fields));
            }
        }
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of(CODE_CONFLICT, MSG_CONFLICT, null));
    }

    /**
     * Traduce una fecha de solicitud fuera de la ventana hoy..hoy+14 a {@code 400}
     * con {@code error = OUTSIDE_REQUEST_WINDOW} y el detalle en {@code requestedDate}.
     *
     * @param ex excepcion de ventana de solicitud
     * @return {@link ApiError} con estado 400 y detalle por campo
     */
    @ExceptionHandler(OutsideRequestWindowException.class)
    public ResponseEntity<ApiError> handleOutsideWindow(OutsideRequestWindowException ex) {
        Map<String, String> fields = Map.of(FIELD_REQUESTED_DATE, ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiError.of(CODE_OUTSIDE_WINDOW, ex.getMessage(), fields));
    }

    /**
     * Traduce la comprobacion previa de solicitud {@code PENDING} duplicada a
     * {@code 409} con {@code error = REQUEST_ALREADY_PENDING}.
     *
     * @param ex excepcion de solicitud pendiente duplicada
     * @return {@link ApiError} con estado 409
     */
    @ExceptionHandler(DuplicatePendingRequestException.class)
    public ResponseEntity<ApiError> handleDuplicatePending(DuplicatePendingRequestException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of(CODE_REQUEST_PENDING, ex.getMessage()));
    }

    /**
     * Traduce una transicion de estado no permitida (cancelar/aprobar/rechazar una
     * solicitud que no esta en {@code PENDING}) a {@code 409}.
     *
     * @param ex excepcion de estado de la solicitud
     * @return {@link ApiError} con estado 409
     */
    @ExceptionHandler(RequestStateException.class)
    public ResponseEntity<ApiError> handleRequestState(RequestStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of(CODE_CONFLICT, ex.getMessage()));
    }

    /**
     * Traduce la indisponibilidad de plaza al aprobar (asignacion fija o solicitud ya
     * aprobada esa fecha) a {@code 409} con {@code error = SPACE_NOT_AVAILABLE}.
     *
     * @param ex excepcion de plaza no disponible
     * @return {@link ApiError} con estado 409
     */
    @ExceptionHandler(SpaceUnavailableException.class)
    public ResponseEntity<ApiError> handleSpaceUnavailable(SpaceUnavailableException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of(CODE_SPACE_UNAVAILABLE, ex.getMessage()));
    }

    /**
     * Traduce la ausencia del texto libre obligatorio en un rechazo {@code OTHER} a
     * {@code 400} con el detalle en {@code rejectionReason}.
     *
     * @param ex excepcion de motivo de rechazo requerido
     * @return {@link ApiError} con estado 400 y detalle por campo
     */
    @ExceptionHandler(RejectionReasonRequiredException.class)
    public ResponseEntity<ApiError> handleRejectionReasonRequired(RejectionReasonRequiredException ex) {
        Map<String, String> fields = Map.of(FIELD_REJECTION_REASON, ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiError.of(CODE_VALIDATION, ex.getMessage(), fields));
    }

    /**
     * Red de seguridad para cualquier error no controlado: {@code 500} sin filtrar detalles.
     *
     * @param ex excepcion no controlada
     * @return {@link ApiError} generico con estado 500
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        LOG.error("Error no controlado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.of(CODE_INTERNAL, MSG_INTERNAL));
    }
}
