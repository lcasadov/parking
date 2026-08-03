package com.aleatica.parking.employee.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.aleatica.parking.employee.Employee;
import com.aleatica.parking.employee.EmployeeRepository;
import com.aleatica.parking.employee.Role;
import com.aleatica.parking.notification.application.EmailMessage;
import com.aleatica.parking.notification.application.EmailSenderPort;
import com.aleatica.parking.push.PushSubscription;
import com.aleatica.parking.push.PushSubscriptionRepository;
import com.aleatica.parking.push.WebPushSender;
import com.aleatica.parking.systemsettings.application.SystemSettingsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests unitarios de {@link EmployeeVehicleAdminNotifier}: fan-out a administradores y aviso al
 * empleado por email + push, con su gateado (interruptor global + preferencia + canal habilitado) y
 * el borrado de una suscripción caducada. Puertos mockeados; {@link ObjectMapper} real.
 */
@ExtendWith(MockitoExtension.class)
class EmployeeVehicleAdminNotifierTest {

    private static final Long ADMIN_ID = 1L;
    private static final Long EMPLOYEE_ID = 15L;

    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private SystemSettingsService systemSettingsService;
    @Mock
    private EmailSenderPort emailSenderPort;
    @Mock
    private PushSubscriptionRepository subscriptionRepository;
    @Mock
    private WebPushSender webPushSender;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private EmployeeVehicleAdminNotifier notifier() {
        return new EmployeeVehicleAdminNotifier(
                employeeRepository, systemSettingsService, emailSenderPort,
                subscriptionRepository, webPushSender, objectMapper);
    }

    private Employee recipient(Long id) {
        Employee employee = org.mockito.Mockito.mock(Employee.class);
        org.mockito.Mockito.lenient().when(employee.getId()).thenReturn(id);
        return employee;
    }

    private PushSubscription subscription() {
        PushSubscription sub = org.mockito.Mockito.mock(PushSubscription.class);
        org.mockito.Mockito.lenient().when(sub.getEndpoint()).thenReturn("https://push/e");
        org.mockito.Mockito.lenient().when(sub.getP256dh()).thenReturn("p");
        org.mockito.Mockito.lenient().when(sub.getAuth()).thenReturn("a");
        return sub;
    }

    @Test
    void shouldEmailAndPushEachActiveAdmin_onVehicleSubmitted() {
        Employee admin = recipient(ADMIN_ID);
        given(admin.isEmailNotificationsEnabled()).willReturn(true);
        given(admin.getEmail()).willReturn("admin@aleatica.com");
        given(admin.isPushNotificationsEnabled()).willReturn(true);
        given(employeeRepository.findByRoleAndActiveTrue(Role.ADMIN)).willReturn(List.of(admin));
        given(systemSettingsService.emailNotificationsEnabled()).willReturn(true);
        given(webPushSender.isEnabled()).willReturn(true);
        given(systemSettingsService.pushNotificationsEnabled()).willReturn(true);
        PushSubscription sub = subscription();
        given(subscriptionRepository.findByEmployeeId(ADMIN_ID)).willReturn(List.of(sub));
        given(webPushSender.send(anyString(), anyString(), anyString(), anyString()))
                .willReturn(WebPushSender.Outcome.SENT);

        notifier().vehicleSubmitted("Juan Perez", "1234ABC");

        verify(emailSenderPort).send(any(EmailMessage.class));
        verify(webPushSender).send(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void shouldSkipBothChannels_whenGloballyDisabled() {
        Employee admin = recipient(ADMIN_ID);
        given(employeeRepository.findByRoleAndActiveTrue(Role.ADMIN)).willReturn(List.of(admin));
        given(systemSettingsService.emailNotificationsEnabled()).willReturn(false);
        given(webPushSender.isEnabled()).willReturn(false);

        notifier().deletionRequested("Juan Perez", "1234ABC");

        verify(emailSenderPort, never()).send(any());
        verify(webPushSender, never()).send(any(), any(), any(), any());
    }

    @Test
    void shouldEmailEmployee_onVehicleApproved() {
        Employee employee = recipient(EMPLOYEE_ID);
        given(employee.isEmailNotificationsEnabled()).willReturn(true);
        given(employee.getEmail()).willReturn("emp@aleatica.com");
        given(employeeRepository.findById(EMPLOYEE_ID)).willReturn(Optional.of(employee));
        given(systemSettingsService.emailNotificationsEnabled()).willReturn(true);
        given(webPushSender.isEnabled()).willReturn(false);

        notifier().vehicleApproved(EMPLOYEE_ID, "1234ABC");

        verify(emailSenderPort).send(any(EmailMessage.class));
    }

    @Test
    void shouldDeleteExpiredSubscription_onPush() {
        Employee employee = recipient(EMPLOYEE_ID);
        given(employee.isPushNotificationsEnabled()).willReturn(true);
        given(employeeRepository.findById(EMPLOYEE_ID)).willReturn(Optional.of(employee));
        given(systemSettingsService.emailNotificationsEnabled()).willReturn(false);
        given(webPushSender.isEnabled()).willReturn(true);
        given(systemSettingsService.pushNotificationsEnabled()).willReturn(true);
        PushSubscription sub = subscription();
        given(subscriptionRepository.findByEmployeeId(EMPLOYEE_ID)).willReturn(List.of(sub));
        given(webPushSender.send(anyString(), anyString(), anyString(), anyString()))
                .willReturn(WebPushSender.Outcome.EXPIRED);

        notifier().vehicleRejected(EMPLOYEE_ID, "1234ABC", "Motivo");

        verify(subscriptionRepository).deleteByEndpoint(eq("https://push/e"));
    }
}
