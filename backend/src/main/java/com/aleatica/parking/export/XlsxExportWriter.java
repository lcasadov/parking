package com.aleatica.parking.export;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Component;

/**
 * Adaptador que serializa una {@link ExportTable} a XLSX en streaming (Apache POI SXSSF).
 *
 * <p>Usa {@link SXSSFWorkbook} con ventana deslizante de filas ({@link #ROW_ACCESS_WINDOW}):
 * las filas por encima de la ventana se vuelcan a disco, de modo que la huella de memoria es
 * constante e independiente del tamano del dataset (evita OOM). Todas las celdas se escriben
 * como texto (los datos ya vienen renderizados) y se sanean contra inyeccion de formulas
 * ({@link FormulaSanitizer}), defensa en profundidad tambien en XLSX.</p>
 */
@Component
public class XlsxExportWriter implements ExportWriterPort {

    private static final String CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String EXTENSION = "xlsx";
    private static final String SHEET_NAME = "export";
    private static final int ROW_ACCESS_WINDOW = 100;

    @Override
    public ExportFormat format() {
        return ExportFormat.XLSX;
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
        // try-with-resources (S2095): SXSSFWorkbook.close() libera recursos; dispose() elimina los
        // ficheros temporales de la ventana deslizante tras el volcado.
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(ROW_ACCESS_WINDOW)) {
            SXSSFSheet sheet = workbook.createSheet(SHEET_NAME);
            int rowIndex = 0;
            writeRow(sheet, rowIndex++, table.headers(), false);
            for (List<String> row : table.rows()) {
                writeRow(sheet, rowIndex++, row, true);
            }
            workbook.write(out);
            workbook.dispose();
        }
    }

    private void writeRow(SXSSFSheet sheet, int rowIndex, List<String> cells, boolean sanitize) {
        Row row = sheet.createRow(rowIndex);
        for (int i = 0; i < cells.size(); i++) {
            Cell cell = row.createCell(i);
            String value = cells.get(i) == null ? "" : cells.get(i);
            cell.setCellValue(sanitize ? FormulaSanitizer.sanitize(value) : value);
        }
    }
}
