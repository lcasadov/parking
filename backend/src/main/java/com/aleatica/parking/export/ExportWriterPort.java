package com.aleatica.parking.export;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Puerto de salida de serializacion de una {@link ExportTable} a un formato concreto.
 *
 * <p>Arquitectura hexagonal: el dominio produce la {@link ExportTable} y este adaptador la
 * escribe <strong>en streaming</strong> sobre el {@code OutputStream} de la respuesta, fila a
 * fila, sin materializar todo el fichero en memoria (evita OOM con datasets grandes). Cada
 * adaptador declara su {@link #format()}, su {@link #contentType()} y su {@link #extension()};
 * el {@link ExportWriters} resuelve el adecuado por formato.</p>
 */
public interface ExportWriterPort {

    /**
     * @return el formato que este adaptador serializa
     */
    ExportFormat format();

    /**
     * @return el {@code Content-Type} del fichero producido (p. ej. {@code text/csv})
     */
    String contentType();

    /**
     * @return la extension de fichero (sin punto), p. ej. {@code "csv"} o {@code "xlsx"}
     */
    String extension();

    /**
     * Serializa la tabla al {@code OutputStream} dado (streaming; no cierra el stream).
     *
     * @param table tabla a serializar (posiblemente vacia: solo cabecera)
     * @param out   flujo de salida de la respuesta
     * @throws IOException si falla la escritura
     */
    void write(ExportTable table, OutputStream out) throws IOException;
}
