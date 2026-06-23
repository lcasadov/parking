package com.aleatica.parking.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Map;

/**
 * Cuerpo de error uniforme de la API.
 *
 * <p>Forma fijada por el contrato del proyecto: {@code { error, message, fields, timestamp }}.
 * Nunca expone stack traces ni detalles internos (OWASP API8 / logging seguro).</p>
 *
 * @param error     codigo de error de negocio estable y legible por maquina (p. ej. {@code NOT_IMPLEMENTED})
 * @param message   mensaje legible por humanos, sin informacion sensible
 * @param fields    errores por campo en validaciones; {@code null} cuando no aplica
 * @param timestamp instante UTC en que se genero el error
 */
@Schema(description = "Respuesta de error estandar de la API parking")
public record ApiError(
        @Schema(description = "Codigo de error de negocio", example = "VALIDATION_ERROR")
        String error,

        @Schema(description = "Mensaje legible por el usuario", example = "La solicitud contiene datos invalidos")
        String message,

        @Schema(description = "Errores por campo (clave = campo, valor = motivo); null si no aplica")
        Map<String, String> fields,

        @Schema(description = "Instante UTC del error (ISO-8601)", example = "2026-06-20T10:15:30Z")
        Instant timestamp
) {

    /**
     * Crea un {@link ApiError} sin errores de campo.
     *
     * @param error   codigo de error de negocio
     * @param message mensaje legible por humanos
     * @return el error construido con {@code fields = null} y marca de tiempo actual
     */
    public static ApiError of(String error, String message) {
        return new ApiError(error, message, null, Instant.now());
    }

    /**
     * Crea un {@link ApiError} con errores por campo (validacion).
     *
     * @param error   codigo de error de negocio
     * @param message mensaje legible por humanos
     * @param fields  errores por campo
     * @return el error construido con marca de tiempo actual
     */
    public static ApiError of(String error, String message, Map<String, String> fields) {
        return new ApiError(error, message, fields, Instant.now());
    }
}
