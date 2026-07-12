package com.aleatica.parking.export.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.aleatica.parking.audit.AuditContext;
import com.aleatica.parking.audit.AuditContextResolver;
import com.aleatica.parking.audit.AuditDetailsSerializer;
import com.aleatica.parking.audit.AuditEntry;
import com.aleatica.parking.audit.AuditLogRepository;
import com.aleatica.parking.employee.AuthOrigin;
import com.aleatica.parking.employee.Employee;
import com.aleatica.parking.employee.EmployeeRepository;
import com.aleatica.parking.employee.Role;
import com.aleatica.parking.export.ExportTable;
import com.aleatica.parking.request.infrastructure.RequestEntity;
import com.aleatica.parking.request.infrastructure.RequestJpaRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

/**
 * Tests unitarios de {@link ExportService}: proyecciones de cada exportacion (empleados excluye
 * credenciales; "mis datos" omite ademas los campos "Solo admins" y se limita al sujeto; "mis
 * solicitudes" filtra por el propietario; histórico/auditoria completos), y registro del evento
 * de exportacion en {@code audit_log}. La logica se aisla con dobles de Mockito.
 */
@ExtendWith(MockitoExtension.class)
class ExportServiceTest {

    private static final String LOGIN = "jperez";
    private static final Instant NOW = Instant.parse("2026-07-04T10:00:00Z");

    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private RequestJpaRepository requestRepository;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private com.aleatica.parking.audit.AuditRecorder auditRecorder;
    @Mock
    private AuditContextResolver auditContextResolver;
    @Mock
    private AuditDetailsSerializer auditDetailsSerializer;

    private ExportService service;

    @BeforeEach
    void setUp() {
        service = new ExportService(employeeRepository, requestRepository, auditLogRepository,
                auditRecorder, auditContextResolver, auditDetailsSerializer);
        // Stub compartido por los casos que si auditan; lenient para no marcarlo innecesario en
        // el caso que lanza antes de auditar (Mockito strictness).
        org.mockito.Mockito.lenient().when(auditContextResolver.resolve())
                .thenReturn(new AuditContext(7L, LOGIN, "ip", "ua"));
    }

    @Test
    void shouldExcludeCredentialColumns_whenExportingEmployees() {
        // Arrange
        given(employeeRepository.findAll(any(Sort.class))).willReturn(List.of(employee()));

        // Act
        ExportTable table = service.exportEmployees();

        // Assert: cabeceras nunca incluyen campos de credenciales (spec Req 1 / OWASP API3)
        assertThat(table.headers())
                .doesNotContain("passwordHash", "failedLoginAttempts", "lockedUntil")
                .contains("login", "email", "role");
        assertThat(table.rows()).hasSize(1);
        verify(auditRecorder).record(any(AuditEntry.class));
    }

    @Test
    void shouldReturnOnlyOwnRowWithOwnPersonalData_whenExportingMyData() {
        // Arrange
        given(employeeRepository.findByLogin(LOGIN)).willReturn(Optional.of(employee()));

        // Act
        ExportTable table = service.exportMyData(LOGIN);

        // Assert (derecho de acceso RGPD): una sola fila, con los datos PROPIOS del sujeto
        // (mobilePhone/licensePlate incluidos), nunca campos de credenciales.
        assertThat(table.headers())
                .contains("mobilePhone", "licensePlate")
                .doesNotContain("passwordHash", "failedLoginAttempts", "lockedUntil");
        assertThat(table.rows()).hasSize(1);
        assertThat(table.rows().get(0)).contains(LOGIN, "600100200", "1234ABC");
    }

    @Test
    void shouldThrow_whenSessionLoginHasNoEmployee() {
        // Arrange
        given(employeeRepository.findByLogin(LOGIN)).willReturn(Optional.empty());

        // Act / Assert
        assertThatThrownBy(() -> service.exportMyData(LOGIN))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void shouldFilterByOwner_whenExportingMyRequests() {
        // Arrange
        given(employeeRepository.findByLogin(LOGIN)).willReturn(Optional.of(employeeWithId(42L)));
        given(requestRepository.findByEmployeeIdOrderByIdAsc(42L)).willReturn(List.of(request(42L)));

        // Act
        ExportTable table = service.exportMyRequests(LOGIN);

        // Assert (BOLA): consulta restringida al propietario 42
        verify(requestRepository).findByEmployeeIdOrderByIdAsc(42L);
        assertThat(table.rows()).hasSize(1);
        assertThat(table.rows().get(0)).contains("42");
    }

    @Test
    void shouldExportAllRequests_whenAdminExportsHistory() {
        // Arrange
        given(requestRepository.findAll(any(Sort.class)))
                .willReturn(List.of(request(1L), request(2L)));

        // Act
        ExportTable table = service.exportRequests();

        // Assert
        assertThat(table.rows()).hasSize(2);
        assertThat(table.headers()).contains("status", "requestedDate");
    }

    @Test
    void shouldExportAllAuditEntries_whenAdminExportsAuditLog() {
        // Arrange
        given(auditLogRepository.findAll(any(Sort.class))).willReturn(List.of());

        // Act
        ExportTable table = service.exportAuditLog();

        // Assert: dataset vacio -> tabla de solo cabecera (edge case sin filas)
        assertThat(table.rows()).isEmpty();
        assertThat(table.headers()).contains("action", "occurredAt");
    }

    @Test
    void shouldRecordAuditWithResolvedActor_whenExporting() {
        // Arrange
        given(employeeRepository.findAll(any(Sort.class))).willReturn(List.of());

        // Act
        service.exportEmployees();

        // Assert: el evento se audita con el actor resuelto del contexto (id 7)
        ArgumentCaptor<AuditEntry> captor = ArgumentCaptor.forClass(AuditEntry.class);
        verify(auditRecorder).record(captor.capture());
        assertThat(captor.getValue().actorEmployeeId()).isEqualTo(7L);
        assertThat(captor.getValue().action()).isEqualTo("EXPORT_EMPLOYEES");
    }

    private static Employee employee() {
        Employee employee = Employee.register(
                "Juan", "Perez", LOGIN, "jperez@aleatica.com", Role.EMPLOYEE, AuthOrigin.LOCAL);
        employee.setDepartment("IT");
        employee.setMobilePhone("600100200");
        employee.setLicensePlate("1234ABC");
        return employee;
    }

    private static Employee employeeWithId(long id) {
        Employee employee = employee();
        org.springframework.test.util.ReflectionTestUtils.setField(employee, "id", id);
        return employee;
    }

    private static RequestEntity request(long employeeId) {
        return RequestEntity.create(employeeId, LocalDate.parse("2026-07-10"), NOW);
    }
}
