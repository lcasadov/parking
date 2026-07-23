package com.aleatica.parking.parkingspace.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.aleatica.parking.employee.dto.PageResponse;
import com.aleatica.parking.parkingspace.ParkingSpace;
import com.aleatica.parking.parkingspace.ParkingSpaceRepository;
import com.aleatica.parking.parkingspace.dto.ParkingSpaceRequest;
import com.aleatica.parking.parkingspace.dto.ParkingSpaceResponse;
import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * Tests unitarios de {@link ParkingSpaceService} con el repositorio mockeado:
 * alta por numero con unicidad, edicion que recalcula label/planta y detecta
 * colision excluyendo la propia plaza, 404 en plaza inexistente, configuracion
 * masiva por numero (subida/bajada) y listado paginado con filtro por planta.
 * No toca la base de datos.
 */
@ExtendWith(MockitoExtension.class)
class ParkingSpaceServiceTest {

    private static final Long ID = 5L;
    private static final int NUMBER = 1007;
    private static final int OTHER_NUMBER = 2003;

    @Mock
    private ParkingSpaceRepository parkingSpaceRepository;

    private ParkingSpaceService newService() {
        return new ParkingSpaceService(parkingSpaceRepository);
    }

    @Test
    void shouldReturnSpace_whenGettingExistingSpace() {
        // Arrange
        given(parkingSpaceRepository.findById(ID))
                .willReturn(java.util.Optional.of(ParkingSpace.create(NUMBER)));

        // Act
        ParkingSpaceResponse found = newService().get(ID);

        // Assert
        assertThat(found.number()).isEqualTo(NUMBER);
        assertThat(found.label()).isEqualTo("1007");
    }

    @Test
    void shouldThrowNotFound_whenGettingNonExistentSpace() {
        // Arrange
        given(parkingSpaceRepository.findById(ID)).willReturn(java.util.Optional.empty());

        // Act / Assert
        assertThatThrownBy(() -> newService().get(ID)).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void shouldCreateSpace_whenNumberIsNew() {
        // Arrange
        given(parkingSpaceRepository.existsByNumber(NUMBER)).willReturn(false);
        given(parkingSpaceRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        // Act
        ParkingSpaceResponse created = newService().create(new ParkingSpaceRequest(NUMBER, null));

        // Assert: label y planta derivados del numero
        assertThat(created.number()).isEqualTo(NUMBER);
        assertThat(created.label()).isEqualTo("1007");
        assertThat(created.floor()).isEqualTo(1);
        assertThat(created.active()).isTrue();
        verify(parkingSpaceRepository).save(any());
    }

    @Test
    void shouldThrowConflict_whenCreatingWithExistingNumber() {
        // Arrange
        given(parkingSpaceRepository.existsByNumber(NUMBER)).willReturn(true);

        // Act / Assert
        assertThatThrownBy(() -> newService().create(new ParkingSpaceRequest(NUMBER, null)))
                .isInstanceOf(ParkingSpaceConflictException.class);
        verify(parkingSpaceRepository, never()).save(any());
    }

    @Test
    void shouldRecalculateLabelAndFloor_whenUpdatingNumber() {
        // Arrange
        ParkingSpace space = ParkingSpace.create(NUMBER);
        given(parkingSpaceRepository.findById(ID)).willReturn(java.util.Optional.of(space));
        given(parkingSpaceRepository.existsByNumberAndIdNot(OTHER_NUMBER, ID)).willReturn(false);
        given(parkingSpaceRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        // Act
        ParkingSpaceResponse updated =
                newService().update(ID, new ParkingSpaceRequest(OTHER_NUMBER, true));

        // Assert
        assertThat(updated.number()).isEqualTo(OTHER_NUMBER);
        assertThat(updated.label()).isEqualTo("2003");
        assertThat(updated.floor()).isEqualTo(2);
        assertThat(updated.active()).isTrue();
    }

    @Test
    void shouldDeactivateSpace_whenUpdatingWithActiveFalse() {
        // Arrange
        ParkingSpace space = ParkingSpace.create(NUMBER);
        given(parkingSpaceRepository.findById(ID)).willReturn(java.util.Optional.of(space));
        given(parkingSpaceRepository.existsByNumberAndIdNot(NUMBER, ID)).willReturn(false);
        given(parkingSpaceRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        // Act
        ParkingSpaceResponse updated =
                newService().update(ID, new ParkingSpaceRequest(NUMBER, false));

        // Assert
        assertThat(updated.active()).isFalse();
    }

    @Test
    void shouldThrowConflict_whenUpdatingToNumberUsedByAnotherSpace() {
        // Arrange
        given(parkingSpaceRepository.findById(ID))
                .willReturn(java.util.Optional.of(ParkingSpace.create(NUMBER)));
        given(parkingSpaceRepository.existsByNumberAndIdNot(OTHER_NUMBER, ID)).willReturn(true);

        // Act / Assert
        assertThatThrownBy(() -> newService().update(ID, new ParkingSpaceRequest(OTHER_NUMBER, true)))
                .isInstanceOf(ParkingSpaceConflictException.class);
        verify(parkingSpaceRepository, never()).save(any());
    }

    @Test
    void shouldThrowNotFound_whenUpdatingNonExistentSpace() {
        // Arrange
        given(parkingSpaceRepository.findById(ID)).willReturn(java.util.Optional.empty());

        // Act / Assert
        assertThatThrownBy(() -> newService().update(ID, new ParkingSpaceRequest(NUMBER, true)))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void shouldCreateMissingSpaces_whenConfiguringHigherTotal() {
        // Arrange: 1 plaza activa, se pide un total de 3 -> crea 2
        given(parkingSpaceRepository.findByActiveTrueOrderByIdAsc())
                .willReturn(new ArrayList<>(List.of(ParkingSpace.create(NUMBER))));
        given(parkingSpaceRepository.existsByNumber(any())).willReturn(false);
        given(parkingSpaceRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(parkingSpaceRepository.findAll(any(org.springframework.data.domain.Sort.class)))
                .willReturn(List.of());

        // Act
        newService().configure(3);

        // Assert: se guardaron las 2 plazas que faltaban
        verify(parkingSpaceRepository, times(2)).save(any());
    }

    @Test
    void shouldDeactivateSurplus_whenConfiguringLowerTotal() {
        // Arrange: 3 plazas activas, se pide un total de 1 -> desactiva las 2 de mayor id
        ParkingSpace first = ParkingSpace.create(1001);
        ParkingSpace second = ParkingSpace.create(1002);
        ParkingSpace third = ParkingSpace.create(1003);
        given(parkingSpaceRepository.findByActiveTrueOrderByIdAsc())
                .willReturn(new ArrayList<>(List.of(first, second, third)));
        given(parkingSpaceRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(parkingSpaceRepository.findAll(any(org.springframework.data.domain.Sort.class)))
                .willReturn(List.of(first, second, third));

        // Act
        newService().configure(1);

        // Assert: las dos ultimas quedan inactivas, la primera se conserva activa
        assertThat(first.isActive()).isTrue();
        assertThat(second.isActive()).isFalse();
        assertThat(third.isActive()).isFalse();
    }

    @Test
    void shouldNotChangeAnything_whenConfiguredTotalMatchesCurrent() {
        // Arrange: 2 activas, total 2 -> sin altas ni bajas
        given(parkingSpaceRepository.findByActiveTrueOrderByIdAsc())
                .willReturn(new ArrayList<>(List.of(
                        ParkingSpace.create(1001), ParkingSpace.create(1002))));
        given(parkingSpaceRepository.findAll(any(org.springframework.data.domain.Sort.class)))
                .willReturn(List.of());

        // Act
        newService().configure(2);

        // Assert
        verify(parkingSpaceRepository, never()).save(any());
    }

    @Test
    void shouldReturnPagedSpaces_whenListingWithoutFloorFilter() {
        // Arrange: sin planta -> rango de numero nulo (no se filtra por planta)
        Pageable pageable = PageRequest.of(0, 20);
        given(parkingSpaceRepository.search(eq(true), isNull(), isNull(), eq(pageable)))
                .willReturn(new PageImpl<>(List.of(ParkingSpace.create(NUMBER))));

        // Act
        PageResponse<ParkingSpaceResponse> page = newService().list(true, null, pageable);

        // Assert
        assertThat(page.content()).hasSize(1);
        assertThat(page.content().get(0).number()).isEqualTo(NUMBER);
        assertThat(page.content().get(0).floor()).isEqualTo(1);
    }

    @Test
    void shouldTranslateFloorToNumberRange_whenListingByFloor() {
        // Arrange: planta 2 -> rango [2000, 2999]
        Pageable pageable = PageRequest.of(0, 20);
        given(parkingSpaceRepository.search(isNull(), eq(2000), eq(2999), eq(pageable)))
                .willReturn(new PageImpl<>(List.of(ParkingSpace.create(2003))));

        // Act
        PageResponse<ParkingSpaceResponse> page = newService().list(null, 2, pageable);

        // Assert
        assertThat(page.content()).hasSize(1);
        assertThat(page.content().get(0).floor()).isEqualTo(2);
    }
}
