package com.aleatica.parking.parkingspace.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
 * alta con unicidad, edicion con colision excluyendo la propia plaza, toggle de
 * {@code active}, 404 en plaza inexistente, configuracion masiva (subida/bajada)
 * y listado paginado. No toca la base de datos.
 */
@ExtendWith(MockitoExtension.class)
class ParkingSpaceServiceTest {

    private static final Long ID = 5L;
    private static final String LABEL = "P-08";
    private static final String OTHER_LABEL = "P-09";

    @Mock
    private ParkingSpaceRepository parkingSpaceRepository;

    private ParkingSpaceService newService() {
        return new ParkingSpaceService(parkingSpaceRepository);
    }

    @Test
    void shouldCreateSpace_whenLabelIsNew() {
        // Arrange
        given(parkingSpaceRepository.existsByLabel(LABEL)).willReturn(false);
        given(parkingSpaceRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        // Act
        ParkingSpaceResponse created = newService().create(new ParkingSpaceRequest(LABEL, null));

        // Assert
        assertThat(created.label()).isEqualTo(LABEL);
        assertThat(created.active()).isTrue();
        verify(parkingSpaceRepository).save(any());
    }

    @Test
    void shouldThrowConflict_whenCreatingWithExistingLabel() {
        // Arrange
        given(parkingSpaceRepository.existsByLabel(LABEL)).willReturn(true);

        // Act / Assert
        assertThatThrownBy(() -> newService().create(new ParkingSpaceRequest(LABEL, null)))
                .isInstanceOf(ParkingSpaceConflictException.class);
        verify(parkingSpaceRepository, never()).save(any());
    }

    @Test
    void shouldUpdateLabel_whenRequestIsValid() {
        // Arrange
        ParkingSpace space = ParkingSpace.create(LABEL);
        given(parkingSpaceRepository.findById(ID)).willReturn(java.util.Optional.of(space));
        given(parkingSpaceRepository.existsByLabelAndIdNot(OTHER_LABEL, ID)).willReturn(false);
        given(parkingSpaceRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        // Act
        ParkingSpaceResponse updated =
                newService().update(ID, new ParkingSpaceRequest(OTHER_LABEL, true));

        // Assert
        assertThat(updated.label()).isEqualTo(OTHER_LABEL);
        assertThat(updated.active()).isTrue();
    }

    @Test
    void shouldDeactivateSpace_whenUpdatingWithActiveFalse() {
        // Arrange
        ParkingSpace space = ParkingSpace.create(LABEL);
        given(parkingSpaceRepository.findById(ID)).willReturn(java.util.Optional.of(space));
        given(parkingSpaceRepository.existsByLabelAndIdNot(LABEL, ID)).willReturn(false);
        given(parkingSpaceRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        // Act
        ParkingSpaceResponse updated =
                newService().update(ID, new ParkingSpaceRequest(LABEL, false));

        // Assert
        assertThat(updated.active()).isFalse();
    }

    @Test
    void shouldThrowConflict_whenUpdatingToLabelUsedByAnotherSpace() {
        // Arrange
        given(parkingSpaceRepository.findById(ID))
                .willReturn(java.util.Optional.of(ParkingSpace.create(LABEL)));
        given(parkingSpaceRepository.existsByLabelAndIdNot(OTHER_LABEL, ID)).willReturn(true);

        // Act / Assert
        assertThatThrownBy(() -> newService().update(ID, new ParkingSpaceRequest(OTHER_LABEL, true)))
                .isInstanceOf(ParkingSpaceConflictException.class);
        verify(parkingSpaceRepository, never()).save(any());
    }

    @Test
    void shouldThrowNotFound_whenUpdatingNonExistentSpace() {
        // Arrange
        given(parkingSpaceRepository.findById(ID)).willReturn(java.util.Optional.empty());

        // Act / Assert
        assertThatThrownBy(() -> newService().update(ID, new ParkingSpaceRequest(LABEL, true)))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void shouldCreateMissingSpaces_whenConfiguringHigherTotal() {
        // Arrange: 1 plaza activa, se pide un total de 3 -> crea 2
        given(parkingSpaceRepository.findByActiveTrueOrderByIdAsc())
                .willReturn(new ArrayList<>(List.of(ParkingSpace.create(LABEL))));
        given(parkingSpaceRepository.existsByLabel(any())).willReturn(false);
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
        ParkingSpace first = ParkingSpace.create("P-001");
        ParkingSpace second = ParkingSpace.create("P-002");
        ParkingSpace third = ParkingSpace.create("P-003");
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
                        ParkingSpace.create("P-001"), ParkingSpace.create("P-002"))));
        given(parkingSpaceRepository.findAll(any(org.springframework.data.domain.Sort.class)))
                .willReturn(List.of());

        // Act
        newService().configure(2);

        // Assert
        verify(parkingSpaceRepository, never()).save(any());
    }

    @Test
    void shouldReturnPagedSpaces_whenListingWithFilter() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 20);
        given(parkingSpaceRepository.search(true, pageable))
                .willReturn(new PageImpl<>(List.of(ParkingSpace.create(LABEL))));

        // Act
        PageResponse<ParkingSpaceResponse> page = newService().list(true, pageable);

        // Assert
        assertThat(page.content()).hasSize(1);
        assertThat(page.content().get(0).label()).isEqualTo(LABEL);
    }
}
