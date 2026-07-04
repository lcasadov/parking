package com.aleatica.parking.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests unitarios del serializador CSV: fichero de solo cabecera cuando no hay filas (1.10),
 * sanitizacion de inyeccion de formulas (1.11), escape RFC 4180 y {@code Content-Type}.
 */
class CsvExportWriterTest {

    private static final String BOM = "﻿";
    private final CsvExportWriter writer = new CsvExportWriter();

    @Test
    void shouldDeclareCsvContentTypeAndExtension() {
        // Assert
        assertThat(writer.format()).isEqualTo(ExportFormat.CSV);
        assertThat(writer.contentType()).startsWith("text/csv");
        assertThat(writer.extension()).isEqualTo("csv");
    }

    @Test
    void shouldEmitHeaderOnlyFile_whenDatasetIsEmpty() throws IOException {
        // Arrange (1.10): tabla sin filas
        ExportTable table = new ExportTable("employees", List.of("id", "login"), List.of());

        // Act
        String content = write(table);

        // Assert: solo la cabecera, sin filas de datos
        assertThat(content).isEqualTo(BOM + "\"id\",\"login\"\r\n");
    }

    @Test
    void shouldWriteHeaderAndRows_whenDatasetHasContent() throws IOException {
        // Arrange
        ExportTable table = new ExportTable("employees",
                List.of("id", "name"),
                List.of(List.of("1", "Juan"), List.of("2", "Ana")));

        // Act
        String content = write(table);

        // Assert
        assertThat(content).isEqualTo(BOM
                + "\"id\",\"name\"\r\n"
                + "\"1\",\"Juan\"\r\n"
                + "\"2\",\"Ana\"\r\n");
    }

    @Test
    void shouldSanitizeFormulaPrefixes_whenWritingCsv() throws IOException {
        // Arrange (1.11): una celda con formula maliciosa
        ExportTable table = new ExportTable("data",
                List.of("value"),
                List.of(List.of("=SUM(1+1)"), List.of("+cmd"), List.of("@evil")));

        // Act
        String content = write(table);

        // Assert: cada celda peligrosa lleva el prefijo apostrofo dentro del entrecomillado
        assertThat(content)
                .contains("\"'=SUM(1+1)\"")
                .contains("\"'+cmd\"")
                .contains("\"'@evil\"")
                .doesNotContain("\"=SUM");
    }

    @Test
    void shouldEscapeQuotes_whenCellContainsQuotes() throws IOException {
        // Arrange
        ExportTable table = new ExportTable("data",
                List.of("name"), List.of(List.of("Ana \"A\" Ruiz")));

        // Act
        String content = write(table);

        // Assert: las comillas internas se duplican (RFC 4180)
        assertThat(content).contains("\"Ana \"\"A\"\" Ruiz\"");
    }

    @Test
    void shouldRenderNullCellAsEmpty_whenValueIsNull() throws IOException {
        // Arrange
        ExportTable table = new ExportTable("data",
                List.of("a", "b"), List.of(java.util.Arrays.asList("x", null)));

        // Act
        String content = write(table);

        // Assert
        assertThat(content).contains("\"x\",\"\"\r\n");
    }

    private String write(ExportTable table) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            writer.write(table, out);
            return out.toString(StandardCharsets.UTF_8);
        }
    }
}
