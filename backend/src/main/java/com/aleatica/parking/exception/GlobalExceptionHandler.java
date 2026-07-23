package com.aleatica.parking.exception;

import com.aleatica.parking.audit.application.InvalidDateRangeException;
import com.aleatica.parking.auth.application.AuthenticationFailedException;
import com.aleatica.parking.export.application.ExportRateLimitExceededException;
import com.aleatica.parking.export.application.UnsupportedExportFormatException;
import com.aleatica.parking.auth.application.InvalidCurrentPasswordException;
import com.aleatica.parking.auth.application.PasswordPolicyException;
import com.aleatica.parking.fixedassignment.application.InvalidDayOfWeekException;
import com.aleatica.parking.release.application.NoFixedAssignmentException;
import com.aleatica.parking.release.application.PastReleaseCancellationException;
import com.aleatica.parking.release.application.ReleaseDateInPastException;
import com.aleatica.parking.release.application.ResourceAlreadyReleasedException;
import com.aleatica.parking.request.application.DuplicatePendingRequestException;
import com.aleatica.parking.request.application.NoAvailabilityException;
import com.aleatica.parking.request.application.OutsideRequestWindowException;
import com.aleatica.parking.request.application.RejectionReasonRequiredException;
import com.aleatica.parking.request.application.RequestStateException;
import com.aleatica.parking.request.application.ResourceSelectionRequiredException;
import com.aleatica.parking.request.application.SpaceUnavailableException;
import com.aleatica.parking.visitor.application.PastVisitorReservationCancellationException;
import com.aleatica.parking.visitor.application.SpaceNotAvailableForReservationException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.persistence.EntityNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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
    private static final String CODE_NO_AVAILABILITY = "NO_AVAILABILITY";
    private static final String CODE_RELEASE_IN_PAST = "RELEASE_DATE_IN_PAST";
    private static final String CODE_NO_FIXED_ASSIGNMENT = "NO_FIXED_ASSIGNMENT";
    private static final String CODE_RESOURCE_RELEASED = "RESOURCE_ALREADY_RELEASED";
    private static final String CODE_RELEASE_NOT_CANCELLABLE = "RELEASE_NOT_CANCELLABLE";
    private static final String CODE_RESERVATION_NOT_CANCELLABLE = "VISITOR_RESERVATION_NOT_CANCELLABLE";
    private static final String CODE_RATE_LIMITED = "RATE_LIMIT_EXCEEDED";
    private static final String FIELD_FORMAT = "format";
    private static final String MSG_UNSUPPORTED_FORMAT =
            "Formato de exportacion no soportado; use csv o xlsx";

    private static final String FIELD_NEW_PASSWORD = "newPassword";
    private static final String FIELD_CURRENT_PASSWORD = "currentPassword";
    private static final String FIELD_LOGIN = "login";
    private static final String FIELD_EMAIL = "email";
    private static final String FIELD_LABEL = "label";
    private static final String FIELD_NUMBER = "number";
    private static final String FIELD_DAYS_OF_WEEK = "daysOfWeek";
    private static final String FIELD_PARKING_SPACE_ID = "parkingSpaceId";
    private static final String FIELD_EMPLOYEE_ID = "employeeId";
    private static final String FIELD_REQUESTED_DATE = "requestedDate";
    private static final String FIELD_REJECTION_REASON = "rejectionReason";
    private static final String FIELD_RESOURCE_ID = "resourceId";
    private static final String FIELD_RELEASE_DATE = "releaseDate";
    private static final String FIELD_NATIONAL_ID = "nationalId";
    private static final String FIELD_DATE_WINDOW = "from";

    private static final String INDEX_LOGIN = "ux_employees_login";
    private static final String INDEX_EMAIL = "ux_employees_email";
    private static final String INDEX_LABEL = "ux_parking_spaces_label";
    private static final String INDEX_DESK_NUMBER = "ux_desks_number";
    private static final String INDEX_FIXED_SPACE_DAY = "ux_fixed_assignments_space_day_active";
    private static final String INDEX_FIXED_EMPLOYEE_DAY = "ux_fixed_assignments_employee_day_active";
    private static final String INDEX_REQUEST_PENDING = "ux_requests_employee_date_pending";
    private static final String INDEX_REQUEST_APPROVED = "ux_requests_space_date_approved";
    private static final String INDEX_REQUEST_DESK_PENDING = "ux_requests_desk_date_pending";
    private static final String INDEX_RELEASE_SPACE_DATE = "ux_releases_space_date";
    private static final String INDEX_VISITOR_NATIONAL_ID = "ux_visitors_national_id";
    private static final String INDEX_VISITOR_RESERVATION_SPACE_DATE =
            "ux_visitor_reservations_space_date";

    private static final String MSG_VALIDATION = "La solicitud contiene datos invalidos";
    private static final String MSG_PARAM_MISSING = "El parametro es obligatorio";
    private static final String MSG_PARAM_MALFORMED = "El parametro tiene un formato invalido";
    private static final String MSG_FORBIDDEN = "No tiene permisos para realizar esta operacion";
    private static final String MSG_NOT_FOUND = "Recurso no encontrado";
    private static final String MSG_INTERNAL = "Se ha producido un error interno";
    private static final String MSG_CONFLICT = "El recurso ya existe o viola una restriccion de unicidad";
    private static final String MSG_CONCURRENCY_CONFLICT =
            "La operacion no se pudo completar por un conflicto de concurrencia; reintente";
    private static final String MSG_LOGIN_TAKEN = "El login ya esta en uso";
    private static final String MSG_EMAIL_TAKEN = "El email ya esta en uso";
    private static final String MSG_LABEL_TAKEN = "La etiqueta ya esta en uso";
    private static final String MSG_DESK_NUMBER_TAKEN = "El numero de puesto ya esta en uso";
    private static final String MSG_SPACE_DAY_TAKEN =
            "La plaza ya esta asignada a otro empleado ese dia de la semana";
    private static final String MSG_EMPLOYEE_DAY_TAKEN =
            "El empleado ya tiene un recurso asignado ese dia de la semana";
    private static final String MSG_REQUEST_PENDING_TAKEN =
            "Ya existe una solicitud pendiente para esa fecha";
    private static final String MSG_SPACE_APPROVED_TAKEN =
            "La plaza ya esta asignada a otra solicitud aprobada esa fecha";
    private static final String MSG_RESOURCE_RELEASED_TAKEN =
            "El recurso ya esta liberado para esa fecha";
    private static final String MSG_DESK_PENDING_TAKEN =
            "El puesto no esta disponible para la fecha solicitada";
    private static final String MSG_NATIONAL_ID_TAKEN =
            "Ya existe un visitante con ese documento de identidad";
    private static final String MSG_RESERVATION_SPACE_TAKEN =
            "La plaza no esta disponible para la fecha de la reserva";

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
            new IndexRule(INDEX_DESK_NUMBER, FIELD_NUMBER, MSG_DESK_NUMBER_TAKEN, CODE_CONFLICT),
            new IndexRule(INDEX_FIXED_SPACE_DAY, FIELD_PARKING_SPACE_ID, MSG_SPACE_DAY_TAKEN, CODE_CONFLICT),
            new IndexRule(INDEX_FIXED_EMPLOYEE_DAY, FIELD_EMPLOYEE_ID, MSG_EMPLOYEE_DAY_TAKEN, CODE_CONFLICT),
            new IndexRule(INDEX_REQUEST_PENDING, null, MSG_REQUEST_PENDING_TAKEN, CODE_REQUEST_PENDING),
            new IndexRule(INDEX_REQUEST_DESK_PENDING, null, MSG_DESK_PENDING_TAKEN,
                    CODE_SPACE_UNAVAILABLE),
            new IndexRule(INDEX_REQUEST_APPROVED, FIELD_PARKING_SPACE_ID, MSG_SPACE_APPROVED_TAKEN,
                    CODE_SPACE_UNAVAILABLE),
            new IndexRule(INDEX_RELEASE_SPACE_DATE, FIELD_PARKING_SPACE_ID, MSG_RESOURCE_RELEASED_TAKEN,
                    CODE_RESOURCE_RELEASED),
            new IndexRule(INDEX_VISITOR_NATIONAL_ID, FIELD_NATIONAL_ID, MSG_NATIONAL_ID_TAKEN,
                    CODE_CONFLICT),
            new IndexRule(INDEX_VISITOR_RESERVATION_SPACE_DATE, FIELD_PARKING_SPACE_ID,
                    MSG_RESERVATION_SPACE_TAKEN, CODE_SPACE_UNAVAILABLE));

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
     * Traduce la ausencia de un parametro de consulta obligatorio (p. ej. {@code date} o
     * {@code weekStart}) a {@code 400} con el nombre del parametro en {@code fields}.
     *
     * @param ex excepcion de parametro de consulta ausente
     * @return {@link ApiError} con estado 400 y el parametro en conflicto
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParam(MissingServletRequestParameterException ex) {
        Map<String, String> fields = Map.of(ex.getParameterName(), MSG_PARAM_MISSING);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiError.of(CODE_VALIDATION, MSG_VALIDATION, fields));
    }

    /**
     * Traduce un parametro con tipo/formato invalido (p. ej. {@code date} o {@code weekStart}
     * no parseables como {@code YYYY-MM-DD}, o un path variable no numerico) a {@code 400}
     * con el nombre del parametro en {@code fields}.
     *
     * @param ex excepcion de tipo de argumento no coincidente
     * @return {@link ApiError} con estado 400 y el parametro en conflicto
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        Map<String, String> fields = Map.of(ex.getName(), MSG_PARAM_MALFORMED);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiError.of(CODE_VALIDATION, MSG_VALIDATION, fields));
    }

    /**
     * Traduce un cuerpo de peticion no parseable (JSON malformado o valor de enum invalido,
     * p. ej. una {@code category} fuera del dominio) a {@code 400}. Sin este manejador, un
     * enum invalido escaparia como {@code 500}. Si Jackson identifica el campo en conflicto
     * (via {@link InvalidFormatException}), se incluye en {@code fields}.
     *
     * @param ex excepcion de cuerpo no legible
     * @return {@link ApiError} con estado 400 (y el campo malformado si se conoce)
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException ex) {
        Map<String, String> fields = Map.of();
        if (ex.getCause() instanceof InvalidFormatException ife && !ife.getPath().isEmpty()) {
            String field = ife.getPath().get(ife.getPath().size() - 1).getFieldName();
            if (field != null) {
                fields = Map.of(field, MSG_PARAM_MALFORMED);
            }
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiError.of(CODE_VALIDATION, MSG_VALIDATION, fields));
    }

    /**
     * Traduce una ventana temporal invalida ({@code from} posterior a {@code to}) en una
     * consulta de auditoria o de logins a {@code 400} con el detalle en {@code from}
     * (spec audit-retention, Req 1/2).
     *
     * @param ex excepcion de ventana temporal invalida
     * @return {@link ApiError} con estado 400 y detalle por campo
     */
    @ExceptionHandler(InvalidDateRangeException.class)
    public ResponseEntity<ApiError> handleInvalidDateRange(InvalidDateRangeException ex) {
        Map<String, String> fields = Map.of(FIELD_DATE_WINDOW, ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiError.of(CODE_VALIDATION, ex.getMessage(), fields));
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
     * Red de seguridad para un conflicto de concurrencia no resuelto por el reintento
     * acotado ({@link com.aleatica.parking.concurrency.ConcurrencyRetry}): traduce una
     * victima de deadlock o un fallo de adquisicion de bloqueo (SQL Server error 1205,
     * {@code SQLState 40001}) a {@code 409} en lugar de {@code 500} (issue #57).
     *
     * <p>Solo surge en carreras de insercion concurrente: en la via normal el reintento
     * ya produce el 409 especifico via {@link #handleDataIntegrity} o la comprobacion
     * previa del caso de uso; este manejador cubre el caso patologico de reintentos
     * agotados. Al colocarse por debajo de {@link DataIntegrityViolationException} (tipos
     * hermanos), no altera el mapeo de la violacion de indice unico.</p>
     *
     * @param ex excepcion de fallo de concurrencia (deadlock/bloqueo)
     * @return {@link ApiError} con estado 409
     */
    @ExceptionHandler(ConcurrencyFailureException.class)
    public ResponseEntity<ApiError> handleConcurrencyFailure(ConcurrencyFailureException ex) {
        LOG.warn("Conflicto de concurrencia no resuelto por reintento: {}",
                ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of(CODE_CONFLICT, MSG_CONCURRENCY_CONFLICT, null));
    }

    /**
     * Traduce una fecha de solicitud pasada (anterior a hoy) a {@code 400}
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
     * Traduce la ausencia de plaza libre en el alta automatica (modo {@code AUTOMATIC}) a
     * {@code 409} con {@code error = NO_AVAILABILITY}: no hay ninguna plaza libre para la fecha
     * y la solicitud no se crea (design §D4).
     *
     * @param ex excepcion de falta de disponibilidad en auto-asignacion
     * @return {@link ApiError} con estado 409
     */
    @ExceptionHandler(NoAvailabilityException.class)
    public ResponseEntity<ApiError> handleNoAvailability(NoAvailabilityException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of(CODE_NO_AVAILABILITY, ex.getMessage()));
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
     * Traduce la ausencia del recurso obligatorio en la asignacion puntual del admin para
     * {@code DESK} a {@code 400} con {@code error = VALIDATION_ERROR} y el detalle en
     * {@code resourceId} (capability {@code admin-punctual-assignment}).
     *
     * @param ex excepcion de recurso obligatorio no indicado
     * @return {@link ApiError} con estado 400 y detalle por campo
     */
    @ExceptionHandler(ResourceSelectionRequiredException.class)
    public ResponseEntity<ApiError> handleResourceSelectionRequired(ResourceSelectionRequiredException ex) {
        Map<String, String> fields = Map.of(FIELD_RESOURCE_ID, ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiError.of(CODE_VALIDATION, ex.getMessage(), fields));
    }

    /**
     * Traduce una fecha de liberacion anterior a hoy a {@code 400} con
     * {@code error = RELEASE_DATE_IN_PAST} y el detalle en {@code releaseDate} (spec Req 1).
     *
     * @param ex excepcion de fecha de liberacion en el pasado
     * @return {@link ApiError} con estado 400 y detalle por campo
     */
    @ExceptionHandler(ReleaseDateInPastException.class)
    public ResponseEntity<ApiError> handleReleaseDateInPast(ReleaseDateInPastException ex) {
        Map<String, String> fields = Map.of(FIELD_RELEASE_DATE, ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiError.of(CODE_RELEASE_IN_PAST, ex.getMessage(), fields));
    }

    /**
     * Traduce la ausencia (o ambiguedad) de una asignacion fija activa del recurso para
     * el dia de la fecha a liberar a {@code 409} con {@code error = NO_FIXED_ASSIGNMENT}
     * (spec Req 1).
     *
     * @param ex excepcion de ausencia de asignacion fija
     * @return {@link ApiError} con estado 409
     */
    @ExceptionHandler(NoFixedAssignmentException.class)
    public ResponseEntity<ApiError> handleNoFixedAssignment(NoFixedAssignmentException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of(CODE_NO_FIXED_ASSIGNMENT, ex.getMessage()));
    }

    /**
     * Traduce la comprobacion previa de recurso ya liberado esa fecha a {@code 409} con
     * {@code error = RESOURCE_ALREADY_RELEASED} y el detalle en {@code parkingSpaceId}
     * (spec Req 4). El mismo codigo cubre la violacion del indice unico bajo concurrencia.
     *
     * @param ex excepcion de recurso ya liberado
     * @return {@link ApiError} con estado 409 y detalle por campo
     */
    @ExceptionHandler(ResourceAlreadyReleasedException.class)
    public ResponseEntity<ApiError> handleResourceAlreadyReleased(ResourceAlreadyReleasedException ex) {
        Map<String, String> fields = Map.of(FIELD_PARKING_SPACE_ID, ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of(CODE_RESOURCE_RELEASED, ex.getMessage(), fields));
    }

    /**
     * Traduce el intento de cancelar una liberacion de fecha pasada a {@code 409} con
     * {@code error = RELEASE_NOT_CANCELLABLE} (spec Req 2).
     *
     * @param ex excepcion de cancelacion de liberacion pasada
     * @return {@link ApiError} con estado 409
     */
    @ExceptionHandler(PastReleaseCancellationException.class)
    public ResponseEntity<ApiError> handlePastReleaseCancellation(PastReleaseCancellationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of(CODE_RELEASE_NOT_CANCELLABLE, ex.getMessage()));
    }

    /**
     * Traduce la indisponibilidad de la plaza al crear una reserva de visitante (plaza
     * inactiva, asignacion fija no liberada, solicitud aprobada u otra reserva esa fecha)
     * a {@code 409} con {@code error = SPACE_NOT_AVAILABLE} y el detalle en
     * {@code parkingSpaceId} (spec Req 2). El mismo codigo cubre la violacion del indice
     * unico {@code UX_visitor_reservations_space_date} bajo concurrencia.
     *
     * @param ex excepcion de plaza no disponible para la reserva
     * @return {@link ApiError} con estado 409 y detalle por campo
     */
    @ExceptionHandler(SpaceNotAvailableForReservationException.class)
    public ResponseEntity<ApiError> handleReservationSpaceUnavailable(
            SpaceNotAvailableForReservationException ex) {
        Map<String, String> fields = Map.of(FIELD_PARKING_SPACE_ID, ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of(CODE_SPACE_UNAVAILABLE, ex.getMessage(), fields));
    }

    /**
     * Traduce el intento de anular una reserva de visitante de fecha pasada a {@code 400}
     * con {@code error = VISITOR_RESERVATION_NOT_CANCELLABLE}: solo se anulan reservas
     * futuras (spec Req 3).
     *
     * @param ex excepcion de anulacion de reserva pasada
     * @return {@link ApiError} con estado 400
     */
    @ExceptionHandler(PastVisitorReservationCancellationException.class)
    public ResponseEntity<ApiError> handlePastReservationCancellation(
            PastVisitorReservationCancellationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiError.of(CODE_RESERVATION_NOT_CANCELLABLE, ex.getMessage()));
    }

    /**
     * Traduce un formato de exportacion no soportado (p. ej. {@code pdf}) a {@code 400} con el
     * detalle en {@code fields.format} (spec exports Req 3: validacion del formato).
     *
     * @param ex excepcion de formato de exportacion no soportado
     * @return {@link ApiError} con estado 400 y detalle por campo
     */
    @ExceptionHandler(UnsupportedExportFormatException.class)
    public ResponseEntity<ApiError> handleUnsupportedFormat(UnsupportedExportFormatException ex) {
        Map<String, String> fields = Map.of(FIELD_FORMAT, MSG_UNSUPPORTED_FORMAT);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiError.of(CODE_VALIDATION, MSG_UNSUPPORTED_FORMAT, fields));
    }

    /**
     * Traduce la superacion del limite de tasa de exportaciones (5/min por usuario) a
     * {@code 429 Too Many Requests} (spec exports Req 4; {@code docs/security-design.md} §7).
     *
     * @param ex excepcion de limite de exportaciones superado
     * @return {@link ApiError} con estado 429
     */
    @ExceptionHandler(ExportRateLimitExceededException.class)
    public ResponseEntity<ApiError> handleRateLimited(ExportRateLimitExceededException ex) {
        LOG.warn("Limite de exportaciones superado: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ApiError.of(CODE_RATE_LIMITED, ex.getMessage()));
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
