package com.aleatica.parking.export;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aleatica.parking.export.application.UnsupportedExportFormatException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests unitarios de la resolucion del parametro {@code format} (spec Req 3): {@code csv}/
 * {@code xlsx} validos (case-insensitive), {@code xlsx} por defecto al omitirse, y rechazo de
 * cualquier otro valor con {@link UnsupportedExportFormatException}.
 */
class ExportFormatTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void shouldDefaultToXlsx_whenParamMissingOrBlank(String param) {
        // Act / Assert (1.8)
        assertThat(ExportFormat.fromParam(param)).isEqualTo(ExportFormat.XLSX);
    }

    @ParameterizedTest
    @ValueSource(strings = {"csv", "CSV", " Csv "})
    void shouldResolveCsv_whenParamIsCsvAnyCase(String param) {
        // Act / Assert
        assertThat(ExportFormat.fromParam(param)).isEqualTo(ExportFormat.CSV);
    }

    @ParameterizedTest
    @ValueSource(strings = {"xlsx", "XLSX", " Xlsx "})
    void shouldResolveXlsx_whenParamIsXlsxAnyCase(String param) {
        // Act / Assert
        assertThat(ExportFormat.fromParam(param)).isEqualTo(ExportFormat.XLSX);
    }

    @ParameterizedTest
    @ValueSource(strings = {"pdf", "json", "xml", "txt"})
    void shouldThrow_whenFormatIsUnsupported(String param) {
        // Act / Assert (1.7)
        assertThatThrownBy(() -> ExportFormat.fromParam(param))
                .isInstanceOf(UnsupportedExportFormatException.class);
    }
}
