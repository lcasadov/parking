package com.aleatica.parking.export;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Resolutor del {@link ExportWriterPort} adecuado por {@link ExportFormat}.
 *
 * <p>Indexa por formato todos los adaptadores del contexto (inyeccion por constructor de la
 * lista de {@link ExportWriterPort}), de modo que anadir un formato nuevo solo requiere un
 * adaptador mas, sin tocar este resolutor ni los controladores.</p>
 */
@Component
public class ExportWriters {

    private final Map<ExportFormat, ExportWriterPort> byFormat = new EnumMap<>(ExportFormat.class);

    /**
     * @param writers adaptadores de serializacion disponibles (uno por formato)
     */
    public ExportWriters(List<ExportWriterPort> writers) {
        for (ExportWriterPort writer : writers) {
            byFormat.put(writer.format(), writer);
        }
    }

    /**
     * Devuelve el adaptador que serializa el formato dado.
     *
     * @param format formato solicitado (ya validado)
     * @return el {@link ExportWriterPort} correspondiente
     * @throws IllegalStateException si no hay adaptador registrado para el formato
     */
    public ExportWriterPort forFormat(ExportFormat format) {
        ExportWriterPort writer = byFormat.get(format);
        if (writer == null) {
            throw new IllegalStateException("No hay adaptador de exportacion para el formato " + format);
        }
        return writer;
    }
}
