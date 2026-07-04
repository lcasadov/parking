package com.aleatica.parking.export;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Adaptador que serializa una {@link ExportTable} a CSV (RFC 4180) en streaming.
 *
 * <p>Escribe cabecera y filas directamente sobre el {@code OutputStream} de la respuesta
 * (envuelto en un {@link BufferedWriter}), sin construir la cadena completa en memoria. Cada
 * celda de datos se sanea contra inyeccion de formulas ({@link FormulaSanitizer}) y luego se
 * escapa segun RFC 4180 (entrecomillado y duplicado de comillas). Emite un BOM UTF-8 para que
 * Excel reconozca la codificacion de los caracteres no ASCII (acentos).</p>
 */
@Component
public class CsvExportWriter implements ExportWriterPort {

    private static final String CONTENT_TYPE = "text/csv;charset=UTF-8";
    private static final String EXTENSION = "csv";
    private static final String SEPARATOR = ",";
    private static final String LINE_END = "\r\n";
    private static final String QUOTE = "\"";
    private static final String ESCAPED_QUOTE = "\"\"";
    private static final String BOM = "﻿";

    @Override
    public ExportFormat format() {
        return ExportFormat.CSV;
    }

    @Override
    public String contentType() {
        return CONTENT_TYPE;
    }

    @Override
    public String extension() {
        return EXTENSION;
    }

    @Override
    public void write(ExportTable table, OutputStream out) throws IOException {
        // try-with-resources (S2095): el writer se vacia y se cierra al terminar. Es la ultima
        // escritura del ciclo de vida de la respuesta, por lo que cerrar el flujo es seguro.
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
            writer.write(BOM);
            writeHeaderRow(writer, table.headers());
            for (List<String> row : table.rows()) {
                writeDataRow(writer, row);
            }
        }
    }

    private void writeHeaderRow(Writer writer, List<String> headers) throws IOException {
        writeRow(writer, headers, false);
    }

    private void writeDataRow(Writer writer, List<String> cells) throws IOException {
        writeRow(writer, cells, true);
    }

    private void writeRow(Writer writer, List<String> cells, boolean sanitize) throws IOException {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) {
                line.append(SEPARATOR);
            }
            String cell = sanitize ? FormulaSanitizer.sanitize(cells.get(i)) : cells.get(i);
            line.append(escape(cell));
        }
        line.append(LINE_END);
        writer.write(line.toString());
    }

    private String escape(String value) {
        String safe = value == null ? "" : value;
        return QUOTE + safe.replace(QUOTE, ESCAPED_QUOTE) + QUOTE;
    }
}
