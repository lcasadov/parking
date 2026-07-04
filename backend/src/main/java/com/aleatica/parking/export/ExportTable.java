package com.aleatica.parking.export;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tabla tabular lista para serializar a CSV/XLSX (independiente del formato).
 *
 * <p>El dominio decide <em>que</em> datos y <em>para quien</em> y produce esta estructura ya
 * proyectada (sin campos sensibles de credenciales ni datos ajenos, segun cada caso de uso);
 * un {@link ExportWriterPort} la serializa fila a fila sobre el {@code OutputStream} de la
 * respuesta. Todas las celdas son cadenas ya renderizadas: el writer no reinterpreta tipos y
 * puede aplicar de forma uniforme la sanitizacion de formulas ({@link FormulaSanitizer}).</p>
 *
 * <p>Un dataset vacio ({@code rows} vacia) es valido: produce un fichero de solo cabecera
 * (edge case sin filas), no un error.</p>
 *
 * @param baseName nombre base del fichero (sin extension ni timestamp), p. ej. {@code "employees"}
 * @param headers  cabeceras de columna en orden
 * @param rows     filas de datos; cada fila con tantas celdas como {@code headers}
 */
public record ExportTable(String baseName, List<String> headers, List<List<String>> rows) {

    /**
     * Copia defensiva de cabeceras y filas para preservar la inmutabilidad del record. La copia
     * es tolerante a celdas {@code null} (a diferencia de {@link List#copyOf(java.util.Collection)}):
     * una celda nula representa un dato ausente (p. ej. {@code department} sin informar) y el writer
     * la renderiza como celda vacia.
     *
     * @param baseName nombre base del fichero
     * @param headers  cabeceras de columna
     * @param rows     filas de datos (las celdas pueden ser {@code null})
     */
    public ExportTable {
        headers = Collections.unmodifiableList(new ArrayList<>(headers));
        rows = rows.stream()
                .map(row -> Collections.unmodifiableList(new ArrayList<>(row)))
                .toList();
    }
}
