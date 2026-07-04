package com.aleatica.parking.request.application;

/**
 * Se lanza al rechazar una solicitud con {@code reasonCode = OTHER} sin un texto libre
 * valido ({@code rejectionReason} ausente o de menos de 5 caracteres).
 *
 * <p>El manejador global la traduce a {@code 400 Bad Request} con
 * {@code error = VALIDATION_ERROR} y {@code fields.rejectionReason} (spec Req 5). Es
 * una regla cruzada entre campos, por eso vive en el caso de uso y no en
 * {@code @Valid}.</p>
 */
public class RejectionReasonRequiredException extends RuntimeException {

    /**
     * @param message mensaje legible por humanos
     */
    public RejectionReasonRequiredException(String message) {
        super(message);
    }
}
