package com.aleatica.parking.desk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.aleatica.parking.resource.BookableResource;
import com.aleatica.parking.resource.ResourceType;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests unitarios de {@link DeskResourceResolver}: el adaptador DESK del puerto
 * {@code ResourceResolverPort} resuelve una referencia {@code (resource_id, DESK)} al
 * mismo {@code Desk} que materializa el recurso. No toca la BD.
 */
@ExtendWith(MockitoExtension.class)
class DeskResourceResolverTest {

    private static final Long DESK_ID = 12L;

    @Mock
    private DeskRepository deskRepository;

    private DeskResourceResolver resolver() {
        return new DeskResourceResolver(deskRepository);
    }

    @Test
    void shouldSupportDeskType() {
        // Act / Assert
        assertThat(resolver().supportedType()).isEqualTo(ResourceType.DESK);
    }

    @Test
    void shouldResolveSameDesk_whenResourceIdExists() {
        // Arrange
        Desk desk = desk(DESK_ID, 5);
        given(deskRepository.findById(DESK_ID)).willReturn(Optional.of(desk));

        // Act
        Optional<BookableResource> resolved = resolver().resolve(DESK_ID);

        // Assert
        assertThat(resolved).containsSame(desk);
        assertThat(resolved.orElseThrow().getResourceId()).isEqualTo(DESK_ID);
        assertThat(resolved.orElseThrow().getResourceType()).isEqualTo(ResourceType.DESK);
        assertThat(resolved.orElseThrow().getLabel()).isEqualTo("D-05");
    }

    @Test
    void shouldReturnEmpty_whenResourceIdUnknown() {
        // Arrange
        given(deskRepository.findById(DESK_ID)).willReturn(Optional.empty());

        // Act / Assert
        assertThat(resolver().resolve(DESK_ID)).isEmpty();
    }

    @Test
    void shouldReportExistence_fromRepository() {
        // Arrange
        given(deskRepository.existsById(DESK_ID)).willReturn(true);

        // Act / Assert
        assertThat(resolver().exists(DESK_ID)).isTrue();
    }

    private static Desk desk(Long id, int number) {
        Desk desk = Desk.create(number, DeskCategory.STANDARD, new BigDecimal("50"), new BigDecimal("50"));
        try {
            Field field = Desk.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(desk, id);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("No se pudo fijar el id del puesto", ex);
        }
        return desk;
    }
}
