package com.aleatica.parking.request.domain;

/**
 * Catalogo de motivos de rechazo de una solicitud (schema {@code RejectionReasonCode}
 * de la API, autoridad {@code docs/openapi.yaml}).
 *
 * <p>La UI muestra los codigos traducidos (ES/EN). Cuando el motivo es {@link #OTHER}
 * el texto libre {@code rejection_reason} (&ge;5 caracteres) es obligatorio; en los
 * demas codigos es opcional. Conjunto inicial pendiente de confirmar con negocio
 * (ver {@code docs/data-model.md} §3.5).</p>
 *
 * <p>Value object del dominio (arquitectura hexagonal, change {@code hexagonal-persistence}):
 * vive en {@code request.domain}, libre de framework.</p>
 */
public enum RejectionReasonCode {

    /** No hay disponibilidad de plaza para la fecha solicitada. */
    NO_AVAILABILITY,

    /** La solicitud queda fuera de la politica vigente. */
    OUTSIDE_POLICY,

    /** Otro motivo; exige texto libre explicativo (&ge;5 caracteres). */
    OTHER
}
