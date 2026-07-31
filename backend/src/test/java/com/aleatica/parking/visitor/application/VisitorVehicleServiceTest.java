package com.aleatica.parking.visitor.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.aleatica.parking.visitor.VisitorRepository;
import com.aleatica.parking.visitor.VisitorVehicle;
import com.aleatica.parking.visitor.VisitorVehicleRepository;
import com.aleatica.parking.visitor.dto.VisitorVehicleRequest;
import com.aleatica.parking.visitor.dto.VisitorVehicleResponse;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests unitarios de {@link VisitorVehicleService} con repositorios mockeados: normalizacion de
 * matricula (trim + mayusculas), unicidad por visitante (409), pertenencia vehiculo↔visitante (404),
 * existencia del visitante (404) y saneado a null de marca/modelo/color en blanco. No toca BD
 * (change {@code visitor-vehicles}).
 */
@ExtendWith(MockitoExtension.class)
class VisitorVehicleServiceTest {

    private static final Long VISITOR_ID = 15L;
    private static final Long VEHICLE_ID = 3L;

    @Mock
    private VisitorVehicleRepository vehicleRepository;
    @Mock
    private VisitorRepository visitorRepository;

    private VisitorVehicleService service() {
        return new VisitorVehicleService(vehicleRepository, visitorRepository);
    }

    @Test
    void shouldListVehicles_whenVisitorExists() {
        given(visitorRepository.existsById(VISITOR_ID)).willReturn(true);
        given(vehicleRepository.findByVisitorIdOrderByIdAsc(VISITOR_ID))
                .willReturn(List.of(vehicle("1234ABC", "Seat")));

        List<VisitorVehicleResponse> result = service().list(VISITOR_ID);

        assertThat(result).singleElement()
                .satisfies(v -> assertThat(v.licensePlate()).isEqualTo("1234ABC"));
    }

    @Test
    void shouldThrowNotFound_whenListingForUnknownVisitor() {
        given(visitorRepository.existsById(VISITOR_ID)).willReturn(false);

        assertThatThrownBy(() -> service().list(VISITOR_ID))
                .isInstanceOf(EntityNotFoundException.class);
        verify(vehicleRepository, never()).findByVisitorIdOrderByIdAsc(any());
    }

    @Test
    void shouldNormalizePlateAndNullifyBlanks_onCreate() {
        given(visitorRepository.existsById(VISITOR_ID)).willReturn(true);
        given(vehicleRepository.existsByVisitorIdAndLicensePlate(VISITOR_ID, "1234ABC"))
                .willReturn(false);
        given(vehicleRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        service().create(VISITOR_ID, new VisitorVehicleRequest("  1234abc  ", "  ", "Leon", ""));

        ArgumentCaptor<VisitorVehicle> captor = ArgumentCaptor.forClass(VisitorVehicle.class);
        verify(vehicleRepository).save(captor.capture());
        VisitorVehicle saved = captor.getValue();
        assertThat(saved.getLicensePlate()).isEqualTo("1234ABC");
        assertThat(saved.getBrand()).isNull();
        assertThat(saved.getModel()).isEqualTo("Leon");
        assertThat(saved.getColor()).isNull();
    }

    @Test
    void shouldThrowConflict_whenCreatingDuplicatePlate() {
        given(visitorRepository.existsById(VISITOR_ID)).willReturn(true);
        given(vehicleRepository.existsByVisitorIdAndLicensePlate(VISITOR_ID, "1234ABC"))
                .willReturn(true);

        assertThatThrownBy(() ->
                service().create(VISITOR_ID, new VisitorVehicleRequest("1234ABC", null, null, null)))
                .isInstanceOf(VisitorVehicleConflictException.class);
        verify(vehicleRepository, never()).save(any());
    }

    @Test
    void shouldThrowNotFound_whenCreatingForUnknownVisitor() {
        given(visitorRepository.existsById(VISITOR_ID)).willReturn(false);

        assertThatThrownBy(() ->
                service().create(VISITOR_ID, new VisitorVehicleRequest("1234ABC", null, null, null)))
                .isInstanceOf(EntityNotFoundException.class);
        verify(vehicleRepository, never()).save(any());
    }

    @Test
    void shouldUpdateVehicle_whenBelongsToVisitor() {
        given(visitorRepository.existsById(VISITOR_ID)).willReturn(true);
        given(vehicleRepository.findByIdAndVisitorId(VEHICLE_ID, VISITOR_ID))
                .willReturn(Optional.of(vehicle("OLD123", "Seat")));
        given(vehicleRepository.existsByVisitorIdAndLicensePlateAndIdNot(VISITOR_ID, "5678XYZ", VEHICLE_ID))
                .willReturn(false);
        given(vehicleRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        VisitorVehicleResponse result = service().update(
                VISITOR_ID, VEHICLE_ID, new VisitorVehicleRequest("5678xyz", "Audi", null, "Negro"));

        assertThat(result.licensePlate()).isEqualTo("5678XYZ");
        assertThat(result.brand()).isEqualTo("Audi");
    }

    @Test
    void shouldThrowConflict_whenUpdatingToPlateOfAnotherVehicle() {
        given(visitorRepository.existsById(VISITOR_ID)).willReturn(true);
        given(vehicleRepository.findByIdAndVisitorId(VEHICLE_ID, VISITOR_ID))
                .willReturn(Optional.of(vehicle("OLD123", "Seat")));
        given(vehicleRepository.existsByVisitorIdAndLicensePlateAndIdNot(VISITOR_ID, "5678XYZ", VEHICLE_ID))
                .willReturn(true);

        assertThatThrownBy(() -> service().update(
                VISITOR_ID, VEHICLE_ID, new VisitorVehicleRequest("5678XYZ", null, null, null)))
                .isInstanceOf(VisitorVehicleConflictException.class);
        verify(vehicleRepository, never()).save(any());
    }

    @Test
    void shouldThrowNotFound_whenUpdatingForeignOrUnknownVehicle() {
        given(visitorRepository.existsById(VISITOR_ID)).willReturn(true);
        given(vehicleRepository.findByIdAndVisitorId(VEHICLE_ID, VISITOR_ID))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> service().update(
                VISITOR_ID, VEHICLE_ID, new VisitorVehicleRequest("5678XYZ", null, null, null)))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void shouldDeleteVehicle_whenBelongsToVisitor() {
        VisitorVehicle existing = vehicle("1234ABC", "Seat");
        given(visitorRepository.existsById(VISITOR_ID)).willReturn(true);
        given(vehicleRepository.findByIdAndVisitorId(VEHICLE_ID, VISITOR_ID))
                .willReturn(Optional.of(existing));

        service().delete(VISITOR_ID, VEHICLE_ID);

        verify(vehicleRepository).delete(existing);
    }

    @Test
    void shouldThrowNotFound_whenDeletingForeignOrUnknownVehicle() {
        given(visitorRepository.existsById(VISITOR_ID)).willReturn(true);
        given(vehicleRepository.findByIdAndVisitorId(VEHICLE_ID, VISITOR_ID))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> service().delete(VISITOR_ID, VEHICLE_ID))
                .isInstanceOf(EntityNotFoundException.class);
        verify(vehicleRepository, never()).delete(any());
    }

    private VisitorVehicle vehicle(String plate, String brand) {
        return VisitorVehicle.create(VISITOR_ID, plate, brand, "Leon", "Gris");
    }
}
