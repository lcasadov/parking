package com.aleatica.parking.exception;

import jakarta.persistence.EntityNotFoundException;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final String MSG_VALIDATION = "La solicitud contiene datos invalidos";
    private static final String MSG_FORBIDDEN = "No tiene permisos para realizar esta operacion";
    private static final String MSG_NOT_FOUND = "Recurso no encontrado";
    private static final String MSG_INTERNAL = "Se ha producido un error interno";

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
