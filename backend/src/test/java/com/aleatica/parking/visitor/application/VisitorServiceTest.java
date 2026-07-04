package com.aleatica.parking.visitor.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.aleatica.parking.employee.Employee;
import com.aleatica.parking.employee.EmployeeRepository;
import com.aleatica.parking.visitor.Visitor;
import com.aleatica.parking.visitor.VisitorRepository;
import com.aleatica.parking.visitor.dto.VisitorCreateRequest;
import com.aleatica.parking.visitor.dto.VisitorResponse;
import jakarta.persistence.EntityNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Tests unitarios de {@link VisitorService} con repositorios y publicador de eventos
 * mockeados: unicidad de {@code nationalId} en alta y edicion, atribucion al {@code ADMIN}
 * creador y publicacion del evento de auditoria. No toca la base de datos.
 */
@ExtendWith(MockitoExtension.class)
class VisitorServiceTest {

    private static final String ADMIN_LOGIN = "admin";
    private static final Long ADMIN_ID = 1L;
    private static final Long VISITOR_ID = 42L;
    private static final String NATIONAL_ID = "X1234567Z";

    @Mock
    private VisitorRepository visitorRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private VisitorService service() {
        return new VisitorService(visitorRepository, employeeRepository, eventPublisher);
    }

    @Test
    void shouldCreateVisitor_whenNationalIdIsUnique() {
        // Arrange
        Employee admin = admin();
        given(employeeRepository.findByLogin(ADMIN_LOGIN)).willReturn(Optional.of(admin));
        given(visitorRepository.existsByNationalId(NATIONAL_ID)).willReturn(false);
        given(visitorRepository.saveAndFlush(any(Visitor.class))).willAnswer(inv -> inv.getArgument(0));

        // Act
        VisitorResponse created = service().create(ADMIN_LOGIN, request(NATIONAL_ID));

        // Assert
        assertThat(created.nationalId()).isEqualTo(NATIONAL_ID);
        assertThat(created.createdById()).isEqualTo(ADMIN_ID);
        verify(eventPublisher).publishEvent(any(VisitorAuditEvent.class));
    }

    @Test
    void shouldThrowConflict_whenCreatingVisitorWithDuplicateNationalId() {
        // Arrange
        Employee admin = admin();
        given(employeeRepository.findByLogin(ADMIN_LOGIN)).willReturn(Optional.of(admin));
        given(visitorRepository.existsByNationalId(NATIONAL_ID)).willReturn(true);

        // Act / Assert
        assertThatThrownBy(() -> service().create(ADMIN_LOGIN, request(NATIONAL_ID)))
                .isInstanceOf(DuplicateNationalIdException.class);
        verify(visitorRepository, never()).saveAndFlush(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldThrowConflict_whenUpdatingToNationalIdOfAnotherVisitor() {
        // Arrange
        given(visitorRepository.findById(VISITOR_ID)).willReturn(Optional.of(existingVisitor()));
        given(visitorRepository.existsByNationalIdAndIdNot(NATIONAL_ID, VISITOR_ID)).willReturn(true);

        // Act / Assert
        assertThatThrownBy(() -> service().update(VISITOR_ID, request(NATIONAL_ID)))
                .isInstanceOf(DuplicateNationalIdException.class);
        verify(visitorRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldUpdateVisitor_whenNationalIdStaysUnique() {
        // Arrange
        given(visitorRepository.findById(VISITOR_ID)).willReturn(Optional.of(existingVisitor()));
        given(visitorRepository.existsByNationalIdAndIdNot(NATIONAL_ID, VISITOR_ID)).willReturn(false);
        given(visitorRepository.saveAndFlush(any(Visitor.class))).willAnswer(inv -> inv.getArgument(0));

        // Act
        VisitorResponse updated = service().update(VISITOR_ID, request(NATIONAL_ID));

        // Assert
        assertThat(updated.nationalId()).isEqualTo(NATIONAL_ID);
        verify(eventPublisher).publishEvent(any(VisitorAuditEvent.class));
    }

    @Test
    void shouldThrowNotFound_whenGettingUnknownVisitor() {
        // Arrange
        given(visitorRepository.findById(VISITOR_ID)).willReturn(Optional.empty());

        // Act / Assert
        assertThatThrownBy(() -> service().get(VISITOR_ID))
                .isInstanceOf(EntityNotFoundException.class);
    }

    private VisitorCreateRequest request(String nationalId) {
        return new VisitorCreateRequest("Ada", "Lovelace", nationalId, "1234ABC", "Contoso", "Reunion");
    }

    private Visitor existingVisitor() {
        return Visitor.create("Grace", "Hopper", "Y7654321X", null, null, null, ADMIN_ID);
    }

    private Employee admin() {
        Employee employee = mock(Employee.class);
        given(employee.getId()).willReturn(ADMIN_ID);
        return employee;
    }
}
