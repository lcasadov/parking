package com.aleatica.parking.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.aleatica.parking.employee.application.EmployeeConflictException;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

/**
 * Tests unitarios del manejador global de errores: verifica el estado HTTP y la
 * forma uniforme {@link ApiError} para cada tipo de excepcion.
 */
@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Mock
    private MethodArgumentNotValidException validationException;

    @Mock
    private BindingResult bindingResult;

    @Test
    void should_return_400_with_field_errors_when_validation_fails() {
        // Given
        FieldError fieldError = new FieldError("loginRequest", "login", "El login es obligatorio");
        given(validationException.getBindingResult()).willReturn(bindingResult);
        given(bindingResult.getFieldErrors()).willReturn(List.of(fieldError));

        // When
        ResponseEntity<ApiError> response = handler.handleValidation(validationException);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getBody().fields()).containsEntry("login", "El login es obligatorio");
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    @Test
    void should_return_403_when_access_denied() {
        // When
        ResponseEntity<ApiError> response =
                handler.handleAccessDenied(new AccessDeniedException("denied"));

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).isEqualTo("FORBIDDEN");
    }

    @Test
    void should_return_404_when_entity_not_found() {
        // When
        ResponseEntity<ApiError> response =
                handler.handleNotFound(new EntityNotFoundException("no existe"));

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).isEqualTo("NOT_FOUND");
        // El mensaje al cliente es generico: nunca filtra el detalle interno (OWASP API8).
        assertThat(response.getBody().message()).isEqualTo("Recurso no encontrado");
        assertThat(response.getBody().message()).doesNotContain("no existe");
    }

    @Test
    void should_return_501_when_not_implemented() {
        // When
        ResponseEntity<ApiError> response =
                handler.handleNotImplemented(new NotImplementedException("pendiente"));

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_IMPLEMENTED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).isEqualTo("NOT_IMPLEMENTED");
        assertThat(response.getBody().message()).isEqualTo("pendiente");
    }

    @Test
    void should_return_409_with_field_when_employee_conflict() {
        // When
        ResponseEntity<ApiError> response = handler.handleFieldConflict(
                new EmployeeConflictException("login", "El login ya esta en uso"));

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).isEqualTo("CONFLICT");
        assertThat(response.getBody().fields()).containsEntry("login", "El login ya esta en uso");
    }

    @Test
    void should_return_409_with_login_field_when_login_index_violated() {
        // When: la causa mas especifica menciona el indice UX_employees_login
        ResponseEntity<ApiError> response = handler.handleDataIntegrity(
                new DataIntegrityViolationException(
                        "duplicate", new IllegalStateException("Violation of UX_employees_login")));

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().fields()).containsKey("login");
    }

    @Test
    void should_return_409_with_email_field_when_email_index_violated() {
        // When
        ResponseEntity<ApiError> response = handler.handleDataIntegrity(
                new DataIntegrityViolationException(
                        "duplicate", new IllegalStateException("Violation of UX_employees_email")));

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().fields()).containsKey("email");
    }

    @Test
    void should_return_409_with_label_field_when_label_index_violated() {
        // When: la causa mas especifica menciona el indice UX_parking_spaces_label
        ResponseEntity<ApiError> response = handler.handleDataIntegrity(
                new DataIntegrityViolationException(
                        "duplicate", new IllegalStateException("Violation of UX_parking_spaces_label")));

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().fields()).containsKey("label");
    }

    @Test
    void should_return_generic_409_when_integrity_violation_unrecognized() {
        // When: sin nombre de indice conocido
        ResponseEntity<ApiError> response = handler.handleDataIntegrity(
                new DataIntegrityViolationException(
                        "fk", new IllegalStateException("FK violation")));

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().fields()).isNull();
    }

    @Test
    void should_return_409_not_500_when_deadlock_victim_lock_failure() {
        // Given: la victima de deadlock (SQL Server 1205) llega como CannotAcquireLockException,
        // subtipo de ConcurrencyFailureException (issue #57).
        CannotAcquireLockException deadlock = new CannotAcquireLockException(
                "deadlock", new IllegalStateException("Transaction (Process ID) was deadlocked"));

        // When
        ResponseEntity<ApiError> response = handler.handleConcurrencyFailure(deadlock);

        // Then: 409 controlado, nunca 500
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).isEqualTo("CONFLICT");
        // El mensaje al cliente no filtra el detalle interno del motor (OWASP API8).
        assertThat(response.getBody().message()).doesNotContain("deadlocked");
    }

    @Test
    void should_return_500_without_internal_details_when_unexpected_error() {
        // When
        ResponseEntity<ApiError> response =
                handler.handleUnexpected(new IllegalStateException("detalle interno sensible"));

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getBody().message()).doesNotContain("detalle interno sensible");
    }
}
