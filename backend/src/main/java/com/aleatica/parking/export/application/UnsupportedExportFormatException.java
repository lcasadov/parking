package com.aleatica.parking.export.application;

/**
 * Se lanza cuando el parametro {@code format} de una exportacion no es {@code csv} ni
 * {@code xlsx} (p. ej. {@code pdf}).
 *
 * <p>El {@code GlobalExceptionHandler} la traduce a {@code 400} con el detalle en
 * {@code fields.format} (spec Req 3: validacion del formato). Lleva el valor rechazado para
 * componer el mensaje sin revelar detalle interno.</p>
 */
public class UnsupportedExportFormatException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final transient String requestedFormat;

    /**
     * @param requestedFormat valor recibido que no corresponde a ningun formato soportado
     */
    public UnsupportedExportFormatException(String requestedFormat) {
        super("Formato de exportacion no soportado: " + requestedFormat);
        this.requestedFormat = requestedFormat;
    }

    /**
     * @return el valor de {@code format} rechazado
     */
    public String getRequestedFormat() {
        return requestedFormat;
    }
}
