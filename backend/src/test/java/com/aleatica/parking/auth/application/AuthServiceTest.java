package com.aleatica.parking.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.aleatica.parking.auth.domain.ClockPort;
import com.aleatica.parking.auth.domain.LoginPhase;
import com.aleatica.parking.auth.domain.LoginResult;
import com.aleatica.parking.employee.Employee;
import com.aleatica.parking.employee.EmployeeRepository;
import com.aleatica.parking.employee.Role;
import com.aleatica.parking.support.EmployeeTestFactory;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Tests unitarios del caso de uso de autenticacion local (flujo critico 100 %):
 * verificacion BCrypt, contador de fallos, bloqueo 5/15 min, reinicio tras OK y
 * cambio de contrasena con politica. Reloj fijo via {@link ClockPort} (sin sleep).
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String LOGIN = "jperez";
    private static final String EMAIL = "jperez@aleatica.com";
    private static final String HASH = "$2a$12$hashplaceholder";
    private static final String RAW_PASSWORD = "Secret#Pass1word";
    private static final Long EMP_ID = 7L;
    private static final Instant NOW = Instant.parse("2026-06-30T10:00:00Z");

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private LoginLogRecorder loginLogRecorder;

    @Mock
    private ClockPort clock;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(employeeRepository, passwordEncoder, loginLogRecorder, clock);
    }

    @Test
    void shouldReturnIdentityAndLogOk_whenCredentialsValid() {
        // Arrange
        Employee employee = activeEmployee();
        given(employeeRepository.findByLogin(LOGIN)).willReturn(Optional.of(employee));
        given(clock.now()).willReturn(NOW);
        given(passwordEncoder.matches(RAW_PASSWORD, HASH)).willReturn(true);

        // Act
        AuthenticatedUser user = authService.authenticate(LOGIN, RAW_PASSWORD);

        // Assert
        assertThat(user.employeeId()).isEqualTo(EMP_ID);
        assertThat(user.login()).isEqualTo(LOGIN);
        assertThat(user.role()).isEqualTo(Role.ADMIN);
        verify(loginLogRecorder).record(LOGIN, EMP_ID, LoginResult.OK, LoginPhase.PHASE_1);
    }

    @Test
    void shouldThrowAndLogInvalidCredentials_whenLoginDoesNotExist() {
        // Arrange
        given(employeeRepository.findByLogin(LOGIN)).willReturn(Optional.empty());

        // Act / Assert
        assertThatThrownBy(() -> authService.authenticate(LOGIN, RAW_PASSWORD))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage(AuthenticationFailedException.GENERIC_MESSAGE);
        verify(loginLogRecorder)
                .record(LOGIN, null, LoginResult.INVALID_CREDENTIALS, LoginPhase.PHASE_1);
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void shouldIncrementFailures_whenPasswordIncorrect() {
        // Arrange
        Employee employee = activeEmployee();
        given(employeeRepository.findByLogin(LOGIN)).willReturn(Optional.of(employee));
        given(clock.now()).willReturn(NOW);
        given(passwordEncoder.matches(RAW_PASSWORD, HASH)).willReturn(false);

        // Act / Assert
        assertThatThrownBy(() -> authService.authenticate(LOGIN, RAW_PASSWORD))
                .isInstanceOf(AuthenticationFailedException.class);
        assertThat(employee.getFailedLoginAttempts()).isEqualTo(1);
        assertThat(employee.getLockedUntil()).isNull();
        verify(loginLogRecorder)
                .record(LOGIN, EMP_ID, LoginResult.INVALID_CREDENTIALS, LoginPhase.PHASE_1);
    }

    @Test
    void shouldLockAccount_whenFifthAttemptFails() {
        // Arrange: already 4 failures, this is the 5th
        Employee employee = activeEmployee();
        employee.setFailedLoginAttempts(4);
        given(employeeRepository.findByLogin(LOGIN)).willReturn(Optional.of(employee));
        given(clock.now()).willReturn(NOW);
        given(passwordEncoder.matches(RAW_PASSWORD, HASH)).willReturn(false);

        // Act / Assert
        assertThatThrownBy(() -> authService.authenticate(LOGIN, RAW_PASSWORD))
                .isInstanceOf(AuthenticationFailedException.class);
        assertThat(employee.getFailedLoginAttempts()).isEqualTo(5);
        assertThat(employee.getLockedUntil()).isEqualTo(NOW.plus(AuthService.LOCK_DURATION));
        verify(loginLogRecorder).record(LOGIN, EMP_ID, LoginResult.LOCKED, LoginPhase.PHASE_1);
    }

    @Test
    void shouldRejectAndLogLocked_whenAccountStillLocked() {
        // Arrange: locked 5 minutes into the future, correct password
        Employee employee = activeEmployee();
        employee.setLockedUntil(NOW.plusSeconds(300));
        given(employeeRepository.findByLogin(LOGIN)).willReturn(Optional.of(employee));
        given(clock.now()).willReturn(NOW);

        // Act / Assert
        assertThatThrownBy(() -> authService.authenticate(LOGIN, RAW_PASSWORD))
                .isInstanceOf(AuthenticationFailedException.class);
        verify(loginLogRecorder).record(LOGIN, EMP_ID, LoginResult.LOCKED, LoginPhase.PHASE_1);
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void shouldRejectAndLogInactive_whenAccountInactive() {
        // Arrange
        Employee employee = activeEmployee();
        EmployeeTestFactory.set(employee, "active", false);
        given(employeeRepository.findByLogin(LOGIN)).willReturn(Optional.of(employee));
        given(clock.now()).willReturn(NOW);

        // Act / Assert
        assertThatThrownBy(() -> authService.authenticate(LOGIN, RAW_PASSWORD))
                .isInstanceOf(AuthenticationFailedException.class);
        verify(loginLogRecorder).record(LOGIN, EMP_ID, LoginResult.INACTIVE, LoginPhase.PHASE_1);
    }

    @Test
    void shouldResetCounter_whenLoginSucceedsAfterPreviousFailures() {
        // Arrange: had 3 prior failures
        Employee employee = activeEmployee();
        employee.setFailedLoginAttempts(3);
        given(employeeRepository.findByLogin(LOGIN)).willReturn(Optional.of(employee));
        given(clock.now()).willReturn(NOW);
        given(passwordEncoder.matches(RAW_PASSWORD, HASH)).willReturn(true);

        // Act
        authService.authenticate(LOGIN, RAW_PASSWORD);

        // Assert
        assertThat(employee.getFailedLoginAttempts()).isZero();
        assertThat(employee.getLockedUntil()).isNull();
        verify(employeeRepository, times(1)).save(employee);
    }

    @Test
    void shouldExpireLockAndAuthenticate_whenLockWindowPassed() {
        // Arrange: lock expired in the past, correct password
        Employee employee = activeEmployee();
        employee.setFailedLoginAttempts(5);
        employee.setLockedUntil(NOW.minusSeconds(1));
        given(employeeRepository.findByLogin(LOGIN)).willReturn(Optional.of(employee));
        given(clock.now()).willReturn(NOW);
        given(passwordEncoder.matches(RAW_PASSWORD, HASH)).willReturn(true);

        // Act
        AuthenticatedUser user = authService.authenticate(LOGIN, RAW_PASSWORD);

        // Assert
        assertThat(user.login()).isEqualTo(LOGIN);
        assertThat(employee.getLockedUntil()).isNull();
        verify(loginLogRecorder).record(LOGIN, EMP_ID, LoginResult.OK, LoginPhase.PHASE_1);
    }

    @Test
    void shouldUpdateHashAndClearMustChange_whenChangePasswordValid() {
        // Arrange
        Employee employee = activeEmployee();
        employee.setPasswordMustChange(true);
        String newPassword = "Another#Pass2word";
        String newHash = "$2a$12$newhash";
        given(employeeRepository.findByLogin(LOGIN)).willReturn(Optional.of(employee));
        given(passwordEncoder.matches("oldCurrent1#X", HASH)).willReturn(true);
        given(passwordEncoder.encode(newPassword)).willReturn(newHash);
        given(clock.now()).willReturn(NOW);

        // Act
        authService.changePassword(LOGIN, "oldCurrent1#X", newPassword);

        // Assert
        ArgumentCaptor<Employee> saved = ArgumentCaptor.forClass(Employee.class);
        verify(employeeRepository).save(saved.capture());
        assertThat(saved.getValue().getPasswordHash()).isEqualTo(newHash);
        assertThat(saved.getValue().isPasswordMustChange()).isFalse();
        assertThat(saved.getValue().getLastPasswordChangeAt()).isEqualTo(NOW);
    }

    @Test
    void shouldThrowInvalidCurrentPassword_whenCurrentPasswordWrong() {
        // Arrange
        Employee employee = activeEmployee();
        given(employeeRepository.findByLogin(LOGIN)).willReturn(Optional.of(employee));
        given(passwordEncoder.matches("wrong", HASH)).willReturn(false);

        // Act / Assert
        assertThatThrownBy(() -> authService.changePassword(LOGIN, "wrong", "Another#Pass2word"))
                .isInstanceOf(InvalidCurrentPasswordException.class);
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void shouldThrowPasswordPolicy_whenNewPasswordViolatesPolicy() {
        // Arrange: new password has no symbol
        Employee employee = activeEmployee();
        given(employeeRepository.findByLogin(LOGIN)).willReturn(Optional.of(employee));
        given(passwordEncoder.matches("oldCurrent1#X", HASH)).willReturn(true);

        // Act / Assert
        assertThatThrownBy(() -> authService.changePassword(LOGIN, "oldCurrent1#X", "NoSymbol1Password"))
                .isInstanceOf(PasswordPolicyException.class);
        verify(employeeRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void shouldThrowInvalidCredentials_whenPasswordHashIsNull() {
        // Arrange: employee without local password (e.g. never provisioned locally)
        Employee employee = EmployeeTestFactory.active(EMP_ID, LOGIN, EMAIL, null, Role.ADMIN);
        given(employeeRepository.findByLogin(LOGIN)).willReturn(Optional.of(employee));
        given(clock.now()).willReturn(NOW);

        // Act / Assert
        assertThatThrownBy(() -> authService.authenticate(LOGIN, RAW_PASSWORD))
                .isInstanceOf(AuthenticationFailedException.class);
        assertThat(employee.getFailedLoginAttempts()).isEqualTo(1);
        verify(loginLogRecorder)
                .record(LOGIN, EMP_ID, LoginResult.INVALID_CREDENTIALS, LoginPhase.PHASE_1);
    }

    @Test
    void shouldThrowInvalidCurrentPassword_whenPasswordHashIsNullOnChange() {
        // Arrange: employee without local password cannot change it
        Employee employee = EmployeeTestFactory.active(EMP_ID, LOGIN, EMAIL, null, Role.ADMIN);
        given(employeeRepository.findByLogin(LOGIN)).willReturn(Optional.of(employee));

        // Act / Assert
        assertThatThrownBy(() -> authService.changePassword(LOGIN, "whatever", "Another#Pass2word"))
                .isInstanceOf(InvalidCurrentPasswordException.class);
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void shouldClearStaleLock_whenAttemptsZeroButLockPresentAndPasswordValid() {
        // Arrange: expired lock left behind while the failure counter is already 0
        Employee employee = activeEmployee();
        employee.setFailedLoginAttempts(0);
        employee.setLockedUntil(NOW.minusSeconds(1));
        given(employeeRepository.findByLogin(LOGIN)).willReturn(Optional.of(employee));
        given(clock.now()).willReturn(NOW);
        given(passwordEncoder.matches(RAW_PASSWORD, HASH)).willReturn(true);

        // Act
        authService.authenticate(LOGIN, RAW_PASSWORD);

        // Assert
        assertThat(employee.getLockedUntil()).isNull();
        verify(employeeRepository, times(1)).save(employee);
        verify(loginLogRecorder).record(LOGIN, EMP_ID, LoginResult.OK, LoginPhase.PHASE_1);
    }

    @Test
    void shouldReturnIdentity_whenLoadByLoginExists() {
        // Arrange
        Employee employee = activeEmployee();
        given(employeeRepository.findByLogin(LOGIN)).willReturn(Optional.of(employee));

        // Act
        AuthenticatedUser user = authService.loadByLogin(LOGIN);

        // Assert
        assertThat(user.employeeId()).isEqualTo(EMP_ID);
        assertThat(user.role()).isEqualTo(Role.ADMIN);
    }

    @Test
    void shouldThrow_whenLoadByLoginMissing() {
        // Arrange
        given(employeeRepository.findByLogin(LOGIN)).willReturn(Optional.empty());

        // Act / Assert
        assertThatThrownBy(() -> authService.loadByLogin(LOGIN))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    private Employee activeEmployee() {
        return EmployeeTestFactory.active(EMP_ID, LOGIN, EMAIL, HASH, Role.ADMIN);
    }
}
