package com.aleatica.parking.systemsettings.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.aleatica.parking.auth.domain.ClockPort;
import com.aleatica.parking.employee.Employee;
import com.aleatica.parking.employee.EmployeeRepository;
import com.aleatica.parking.systemsettings.domain.ApprovalMode;
import com.aleatica.parking.systemsettings.domain.SystemSettings;
import com.aleatica.parking.systemsettings.domain.SystemSettingsRepositoryPort;
import com.aleatica.parking.systemsettings.dto.SystemSettingsResponse;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Tests unitarios de {@link SystemSettingsService} con un fake in-memory del puerto
 * {@link SystemSettingsRepositoryPort}: lectura del modo (default {@code MANUAL} si la fila no
 * existe), persistencia del cambio con trazabilidad (actor + marca de tiempo) y publicacion del
 * evento de auditoria. No toca la base de datos.
 */
@ExtendWith(MockitoExtension.class)
class SystemSettingsServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-14T09:00:00Z");
    private static final String ADMIN_LOGIN = "admin";
    private static final Long ADMIN_ID = 1L;

    private final InMemorySettingsRepository settingsRepository = new InMemorySettingsRepository();

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Captor
    private ArgumentCaptor<SystemSettingsAuditEvent> eventCaptor;

    private final ClockPort clock = () -> NOW;

    private SystemSettingsService newService() {
        return new SystemSettingsService(
                settingsRepository, employeeRepository, eventPublisher, clock);
    }

    @Test
    void shouldReturnManual_whenRowDoesNotExist() {
        // Arrange (store vacio)

        // Act / Assert: default retrocompatible
        assertThat(newService().approvalMode()).isEqualTo(ApprovalMode.MANUAL);
    }

    @Test
    void shouldReturnPersistedMode_whenRowExists() {
        // Arrange
        settingsRepository.seed(SystemSettings.restore(
                SystemSettings.SINGLETON_ID, ApprovalMode.AUTOMATIC, null, null, null, false, true, true,
                ADMIN_ID, NOW));

        // Act / Assert
        assertThat(newService().approvalMode()).isEqualTo(ApprovalMode.AUTOMATIC);
    }

    @Test
    void shouldPersistModeAndTrace_whenUpdating() {
        // Arrange
        givenAdmin();

        // Act
        SystemSettingsResponse result =
                newService().updateApprovalMode(ApprovalMode.AUTOMATIC, ADMIN_LOGIN);

        // Assert: valor + trazabilidad (actor + marca de tiempo)
        assertThat(result.approvalMode()).isEqualTo(ApprovalMode.AUTOMATIC);
        assertThat(result.updatedById()).isEqualTo(ADMIN_ID);
        assertThat(result.updatedAt()).isEqualTo(NOW);
        assertThat(settingsRepository.find().orElseThrow().getApprovalMode())
                .isEqualTo(ApprovalMode.AUTOMATIC);
    }

    @Test
    void shouldPublishAuditEvent_whenUpdating() {
        // Arrange
        givenAdmin();

        // Act
        newService().updateApprovalMode(ApprovalMode.AUTOMATIC, ADMIN_LOGIN);

        // Assert
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().settings().approvalMode()).isEqualTo(ApprovalMode.AUTOMATIC);
    }

    @Test
    void shouldReturnNullParkingAddress_whenRowDoesNotExist() {
        // Arrange (store vacio)

        // Act / Assert: sin configurar
        assertThat(newService().parkingAddress().parkingAddress()).isNull();
    }

    @Test
    void shouldReturnPersistedParkingAddress_whenRowExists() {
        // Arrange
        settingsRepository.seed(SystemSettings.restore(
                SystemSettings.SINGLETON_ID, ApprovalMode.MANUAL, "Av. de Europa 18, Alcobendas",
                null, null, false, true, true, ADMIN_ID, NOW));

        // Act / Assert
        assertThat(newService().parkingAddress().parkingAddress())
                .isEqualTo("Av. de Europa 18, Alcobendas");
    }

    @Test
    void shouldPersistParkingAddressAndTrace_whenUpdating() {
        // Arrange
        givenAdmin();

        // Act
        SystemSettingsResponse result =
                newService().updateParkingAddress("  Av. de Europa 18  ", null, null, ADMIN_LOGIN);

        // Assert: se normaliza (trim) y se registra la trazabilidad
        assertThat(result.parkingAddress()).isEqualTo("Av. de Europa 18");
        assertThat(result.updatedById()).isEqualTo(ADMIN_ID);
        assertThat(result.updatedAt()).isEqualTo(NOW);
        assertThat(settingsRepository.find().orElseThrow().getParkingAddress())
                .isEqualTo("Av. de Europa 18");
    }

    @Test
    void shouldClearParkingAddress_whenUpdatingWithBlank() {
        // Arrange: habia una direccion configurada
        settingsRepository.seed(SystemSettings.restore(
                SystemSettings.SINGLETON_ID, ApprovalMode.MANUAL, "Direccion previa", null, null,
                false, true, true, ADMIN_ID, NOW));
        givenAdmin();

        // Act: enviar blanco la borra
        SystemSettingsResponse result = newService().updateParkingAddress("   ", null, null, ADMIN_LOGIN);

        // Assert
        assertThat(result.parkingAddress()).isNull();
    }

    @Test
    void shouldReturnFalseWeekend_whenRowDoesNotExist() {
        // Act / Assert: default retrocompatible (sin fines de semana)
        assertThat(newService().weekendReservable()).isFalse();
        assertThat(newService().weekendReservableView().weekendReservable()).isFalse();
    }

    @Test
    void shouldReturnPersistedWeekend_whenRowExists() {
        // Arrange
        settingsRepository.seed(SystemSettings.restore(
                SystemSettings.SINGLETON_ID, ApprovalMode.MANUAL, null, null, null, true, true, true,
                ADMIN_ID, NOW));

        // Act / Assert
        assertThat(newService().weekendReservable()).isTrue();
    }

    @Test
    void shouldPersistWeekendAndTrace_whenUpdating() {
        // Arrange
        givenAdmin();

        // Act
        SystemSettingsResponse result = newService().updateWeekendReservable(true, ADMIN_LOGIN);

        // Assert
        assertThat(result.weekendReservable()).isTrue();
        assertThat(result.updatedById()).isEqualTo(ADMIN_ID);
        assertThat(result.updatedAt()).isEqualTo(NOW);
        assertThat(settingsRepository.find().orElseThrow().isWeekendReservable()).isTrue();
    }

    @Test
    void shouldThrowNotFound_whenActorNoLongerExists() {
        // Arrange
        given(employeeRepository.findByLogin(ADMIN_LOGIN)).willReturn(Optional.empty());

        // Act / Assert
        assertThatThrownBy(() -> newService().updateApprovalMode(ApprovalMode.AUTOMATIC, ADMIN_LOGIN))
                .isInstanceOf(EntityNotFoundException.class);
    }

    private void givenAdmin() {
        Employee admin = mock(Employee.class);
        given(admin.getId()).willReturn(ADMIN_ID);
        given(employeeRepository.findByLogin(ADMIN_LOGIN)).willReturn(Optional.of(admin));
    }

    /** Fake in-memory del puerto de persistencia del singleton de ajustes. */
    private static final class InMemorySettingsRepository implements SystemSettingsRepositoryPort {

        private SystemSettings current;

        void seed(SystemSettings settings) {
            this.current = settings;
        }

        @Override
        public Optional<SystemSettings> find() {
            return Optional.ofNullable(current);
        }

        @Override
        public SystemSettings save(SystemSettings settings) {
            this.current = settings;
            return settings;
        }
    }
}
