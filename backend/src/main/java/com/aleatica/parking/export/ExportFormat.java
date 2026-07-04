package com.aleatica.parking.export;

import com.aleatica.parking.export.application.UnsupportedExportFormatException;
import java.util.Locale;

/**
 * Formatos de exportacion soportados: {@code csv} y {@code xlsx} (nunca PDF).
 *
 * <p>El contrato ({@code docs/openapi.yaml}, parametro {@code ExportFormatParam}) fija el
 * enum {@code [csv, xlsx]} con {@code xlsx} por defecto. {@link #fromParam(String)} centraliza
 * la validacion del parametro de consulta: ausente o en blanco resuelve a {@link #XLSX}
 * (defecto); cualquier valor fuera del enum se rechaza con
 * {@link UnsupportedExportFormatException} (traducida a {@code 400} con {@code fields.format}).</p>
 */
public enum ExportFormat {

    /** Valores separados por comas ({@code text/csv}). */
    CSV,

    /** Hoja de calculo OpenXML ({@code application/vnd.openxmlformats-...spreadsheetml.sheet}). */
    XLSX;

    /** Formato por defecto cuando el parametro {@code format} se omite (contrato). */
    public static final ExportFormat DEFAULT = XLSX;

    /**
     * Resuelve el parametro de consulta {@code format} a un {@link ExportFormat}.
     *
     * @param param valor recibido; {@code null} o en blanco resuelve al defecto ({@link #XLSX})
     * @return el formato correspondiente
     * @throws UnsupportedExportFormatException si el valor no es {@code csv} ni {@code xlsx}
     */
    public static ExportFormat fromParam(String param) {
        if (param == null || param.isBlank()) {
            return DEFAULT;
        }
        String normalized = param.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "csv" -> CSV;
            case "xlsx" -> XLSX;
            default -> throw new UnsupportedExportFormatException(param);
        };
    }
}
