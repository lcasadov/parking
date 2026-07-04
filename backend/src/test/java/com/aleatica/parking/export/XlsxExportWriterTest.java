package com.aleatica.parking.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

/**
 * Tests unitarios del serializador XLSX (Apache POI SXSSF): fichero de solo cabecera cuando no
 * hay filas (1.10), contenido legible de vuelta con XSSF, sanitizacion de formulas (1.11) y
 * {@code Content-Type} OpenXML.
 */
class XlsxExportWriterTest {

    private final XlsxExportWriter writer = new XlsxExportWriter();

    @Test
    void shouldDeclareXlsxContentTypeAndExtension() {
        // Assert
        assertThat(writer.format()).isEqualTo(ExportFormat.XLSX);
        assertThat(writer.contentType())
                .isEqualTo("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        assertThat(writer.extension()).isEqualTo("xlsx");
    }

    @Test
    void shouldEmitHeaderOnlyFile_whenDatasetIsEmpty() throws IOException {
        // Arrange (1.10)
        ExportTable table = new ExportTable("employees", List.of("id", "login"), List.of());

        // Act
        byte[] bytes = write(table);

        // Assert: una unica fila (la cabecera)
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheetAt(0);
            assertThat(sheet.getLastRowNum()).isZero();
            Row header = sheet.getRow(0);
            assertThat(header.getCell(0).getStringCellValue()).isEqualTo("id");
            assertThat(header.getCell(1).getStringCellValue()).isEqualTo("login");
        }
    }

    @Test
    void shouldWriteHeaderAndRows_whenDatasetHasContent() throws IOException {
        // Arrange
        ExportTable table = new ExportTable("employees",
                List.of("id", "name"), List.of(List.of("1", "Juan"), List.of("2", "Ana")));

        // Act
        byte[] bytes = write(table);

        // Assert
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheetAt(0);
            assertThat(sheet.getLastRowNum()).isEqualTo(2);
            assertThat(sheet.getRow(1).getCell(1).getStringCellValue()).isEqualTo("Juan");
            assertThat(sheet.getRow(2).getCell(1).getStringCellValue()).isEqualTo("Ana");
        }
    }

    @Test
    void shouldSanitizeFormulaPrefixes_whenWritingXlsx() throws IOException {
        // Arrange (1.11)
        ExportTable table = new ExportTable("data",
                List.of("value"), List.of(List.of("=1+1")));

        // Act
        byte[] bytes = write(table);

        // Assert: la celda de texto se guarda con el prefijo apostrofo (no como formula)
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertThat(wb.getSheetAt(0).getRow(1).getCell(0).getStringCellValue())
                    .isEqualTo("'=1+1");
        }
    }

    private byte[] write(ExportTable table) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            writer.write(table, out);
            return out.toByteArray();
        }
    }
}
