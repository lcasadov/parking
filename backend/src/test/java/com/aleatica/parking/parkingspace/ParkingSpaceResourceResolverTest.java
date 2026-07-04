package com.aleatica.parking.parkingspace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.aleatica.parking.resource.BookableResource;
import com.aleatica.parking.resource.ResourceType;
import java.lang.reflect.Field;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests unitarios de {@link ParkingSpaceResourceResolver}: el adaptador PARKING del
 * puerto {@code ResourceResolverPort} resuelve una referencia {@code (resource_id,
 * PARKING)} a la misma {@code ParkingSpace} que materializa el recurso. No toca la BD.
 */
@ExtendWith(MockitoExtension.class)
class ParkingSpaceResourceResolverTest {

    private static final Long SPACE_ID = 8L;

    @Mock
    private ParkingSpaceRepository parkingSpaceRepository;

    private ParkingSpaceResourceResolver resolver() {
        return new ParkingSpaceResourceResolver(parkingSpaceRepository);
    }

    @Test
    void shouldSupportParkingType() {
        // Act / Assert
        assertThat(resolver().supportedType()).isEqualTo(ResourceType.PARKING);
    }

    @Test
    void shouldResolveSameParkingSpace_whenResourceIdExists() {
        // Arrange
        ParkingSpace space = space(SPACE_ID, "P-08");
        given(parkingSpaceRepository.findById(SPACE_ID)).willReturn(Optional.of(space));

        // Act
        Optional<BookableResource> resolved = resolver().resolve(SPACE_ID);

        // Assert
        assertThat(resolved).containsSame(space);
        assertThat(resolved.orElseThrow().getResourceId()).isEqualTo(SPACE_ID);
        assertThat(resolved.orElseThrow().getResourceType()).isEqualTo(ResourceType.PARKING);
        assertThat(resolved.orElseThrow().getLabel()).isEqualTo("P-08");
    }

    @Test
    void shouldReturnEmpty_whenResourceIdUnknown() {
        // Arrange
        given(parkingSpaceRepository.findById(SPACE_ID)).willReturn(Optional.empty());

        // Act / Assert
        assertThat(resolver().resolve(SPACE_ID)).isEmpty();
    }

    @Test
    void shouldReportExistence_fromRepository() {
        // Arrange
        given(parkingSpaceRepository.existsById(SPACE_ID)).willReturn(true);

        // Act / Assert
        assertThat(resolver().exists(SPACE_ID)).isTrue();
    }

    private static ParkingSpace space(Long id, String label) {
        ParkingSpace space = ParkingSpace.create(label);
        try {
            Field field = ParkingSpace.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(space, id);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("No se pudo fijar el id de la plaza", ex);
        }
        return space;
    }
}
