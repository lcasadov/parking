package com.aleatica.parking.desk.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.aleatica.parking.desk.Desk;
import com.aleatica.parking.desk.DeskCategory;
import com.aleatica.parking.desk.DeskRepository;
import com.aleatica.parking.desk.dto.DeskCreateRequest;
import com.aleatica.parking.desk.dto.DeskResponse;
import com.aleatica.parking.desk.dto.DeskUpdateRequest;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests unitarios de {@link DeskService} con el repositorio mockeado: alta con unicidad de
 * numero, edicion de categoria/coordenadas, activacion/desactivacion, 404 en puesto
 * inexistente y coordenadas por defecto al omitirse. No toca la base de datos.
 */
@ExtendWith(MockitoExtension.class)
class DeskServiceTest {

    private static final Long ID = 12L;
    private static final Integer NUMBER = 12;
    private static final BigDecimal X = new BigDecimal("30.5");
    private static final BigDecimal Y = new BigDecimal("47.0");

    @Mock
    private DeskRepository deskRepository;

    private DeskService newService() {
        return new DeskService(deskRepository);
    }

    @Test
    void shouldCreateDesk_whenNumberInRangeAndUnique() {
        // Arrange
        given(deskRepository.existsByNumber(NUMBER)).willReturn(false);
        given(deskRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        // Act
        DeskResponse created = newService().create(
                new DeskCreateRequest(NUMBER, DeskCategory.STANDARD, X, Y));

        // Assert
        assertThat(created.number()).isEqualTo(NUMBER);
        assertThat(created.category()).isEqualTo(DeskCategory.STANDARD);
        assertThat(created.active()).isTrue();
        verify(deskRepository).save(any());
    }

    @Test
    void shouldDefaultCoordinatesToCenter_whenOmittedOnCreate() {
        // Arrange
        given(deskRepository.existsByNumber(NUMBER)).willReturn(false);
        given(deskRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        // Act: sin coordenadas -> centro 50/50
        DeskResponse created = newService().create(
                new DeskCreateRequest(NUMBER, DeskCategory.STANDARD, null, null));

        // Assert
        assertThat(created.coordX()).isEqualByComparingTo("50");
        assertThat(created.coordY()).isEqualByComparingTo("50");
    }

    @Test
    void shouldThrowConflict_whenCreatingWithDuplicateNumber() {
        // Arrange
        given(deskRepository.existsByNumber(NUMBER)).willReturn(true);

        // Act / Assert
        assertThatThrownBy(() -> newService().create(
                new DeskCreateRequest(NUMBER, DeskCategory.STANDARD, X, Y)))
                .isInstanceOf(DeskConflictException.class);
        verify(deskRepository, never()).save(any());
    }

    @Test
    void shouldUpdateCategory_whenAdminSetsExecutive() {
        // Arrange
        Desk desk = Desk.create(NUMBER, DeskCategory.STANDARD, X, Y);
        given(deskRepository.findById(ID)).willReturn(Optional.of(desk));
        given(deskRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        // Act
        DeskResponse updated = newService().update(
                ID, new DeskUpdateRequest(DeskCategory.EXECUTIVE, null, null));

        // Assert: categoria cambiada, coordenadas conservadas al omitirse
        assertThat(updated.category()).isEqualTo(DeskCategory.EXECUTIVE);
        assertThat(updated.coordX()).isEqualByComparingTo(X);
    }

    @Test
    void shouldUpdateCoordinates_whenProvided() {
        // Arrange
        Desk desk = Desk.create(NUMBER, DeskCategory.STANDARD, X, Y);
        given(deskRepository.findById(ID)).willReturn(Optional.of(desk));
        given(deskRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        // Act
        DeskResponse updated = newService().update(
                ID, new DeskUpdateRequest(DeskCategory.STANDARD, new BigDecimal("10"), new BigDecimal("20")));

        // Assert
        assertThat(updated.coordX()).isEqualByComparingTo("10");
        assertThat(updated.coordY()).isEqualByComparingTo("20");
    }

    @Test
    void shouldThrowNotFound_whenUpdatingUnknownDesk() {
        // Arrange
        given(deskRepository.findById(ID)).willReturn(Optional.empty());

        // Act / Assert
        assertThatThrownBy(() -> newService().update(
                ID, new DeskUpdateRequest(DeskCategory.STANDARD, null, null)))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void shouldDeactivateDesk_whenActivationSetFalse() {
        // Arrange
        Desk desk = Desk.create(NUMBER, DeskCategory.STANDARD, X, Y);
        given(deskRepository.findById(ID)).willReturn(Optional.of(desk));
        given(deskRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        // Act
        DeskResponse updated = newService().setActivation(ID, false);

        // Assert
        assertThat(updated.active()).isFalse();
    }

    @Test
    void shouldThrowNotFound_whenGettingUnknownDesk() {
        // Arrange
        given(deskRepository.findById(ID)).willReturn(Optional.empty());

        // Act / Assert
        assertThatThrownBy(() -> newService().get(ID)).isInstanceOf(EntityNotFoundException.class);
    }
}
