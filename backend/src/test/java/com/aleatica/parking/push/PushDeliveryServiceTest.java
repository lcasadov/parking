package com.aleatica.parking.push;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.aleatica.parking.employee.Employee;
import com.aleatica.parking.employee.EmployeeRepository;
import com.aleatica.parking.employee.Role;
import com.aleatica.parking.notification.NotificationEventType;
import com.aleatica.parking.notification.application.NotificationCommand;
import com.aleatica.parking.support.EmployeeTestFactory;
import com.aleatica.parking.systemsettings.application.SystemSettingsService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PushDeliveryServiceTest {

    @Mock private SystemSettingsService systemSettingsService;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private PushSubscriptionRepository subscriptionRepository;
    @Mock private WebPushSender webPushSender;
    @Mock private PushContentRenderer contentRenderer;

    private PushDeliveryService service;

    @BeforeEach
    void setUp() {
        service = new PushDeliveryService(
                systemSettingsService, employeeRepository, subscriptionRepository, webPushSender, contentRenderer);
        lenient().when(webPushSender.isEnabled()).thenReturn(true);
        lenient().when(systemSettingsService.pushNotificationsEnabled()).thenReturn(true);
        lenient().when(contentRenderer.render(any())).thenReturn("{}");
    }

    private static NotificationCommand command() {
        return new NotificationCommand(NotificationEventType.REQUEST_APPROVED, 10L, null);
    }

    private Employee employeeWithPush(boolean push) {
        Employee e = EmployeeTestFactory.active(10L, "u10", "u10@aleatica.com", null, Role.EMPLOYEE);
        e.setPushNotificationsEnabled(push);
        return e;
    }

    @Test
    void shouldNotSend_whenPushDisabledGlobally() {
        given(systemSettingsService.pushNotificationsEnabled()).willReturn(false);
        service.dispatch(command());
        verify(webPushSender, never()).send(any(), any(), any(), any());
    }

    @Test
    void shouldNotSend_whenSenderNotConfigured() {
        given(webPushSender.isEnabled()).willReturn(false);
        service.dispatch(command());
        verify(webPushSender, never()).send(any(), any(), any(), any());
    }

    @Test
    void shouldNotSend_whenEmployeePushDisabled() {
        given(employeeRepository.findById(10L)).willReturn(Optional.of(employeeWithPush(false)));
        service.dispatch(command());
        verify(webPushSender, never()).send(any(), any(), any(), any());
    }

    @Test
    void shouldNotSend_whenNoSubscriptions() {
        given(employeeRepository.findById(10L)).willReturn(Optional.of(employeeWithPush(true)));
        given(subscriptionRepository.findByEmployeeId(10L)).willReturn(List.of());
        service.dispatch(command());
        verify(webPushSender, never()).send(any(), any(), any(), any());
    }

    @Test
    void shouldSendToAllSubscriptions_whenEligible() {
        given(employeeRepository.findById(10L)).willReturn(Optional.of(employeeWithPush(true)));
        given(subscriptionRepository.findByEmployeeId(10L)).willReturn(List.of(
                PushSubscription.of(10L, "e1", "p", "a", null),
                PushSubscription.of(10L, "e2", "p", "a", null)));
        given(webPushSender.send(anyString(), anyString(), anyString(), anyString()))
                .willReturn(WebPushSender.Outcome.SENT);

        service.dispatch(command());

        verify(webPushSender).send(eq("e1"), any(), any(), any());
        verify(webPushSender).send(eq("e2"), any(), any(), any());
        verify(subscriptionRepository, never()).deleteByEndpoint(any());
    }

    @Test
    void shouldDeleteExpiredSubscription_whenGone() {
        given(employeeRepository.findById(10L)).willReturn(Optional.of(employeeWithPush(true)));
        given(subscriptionRepository.findByEmployeeId(10L))
                .willReturn(List.of(PushSubscription.of(10L, "e1", "p", "a", null)));
        given(webPushSender.send(anyString(), anyString(), anyString(), anyString()))
                .willReturn(WebPushSender.Outcome.EXPIRED);

        service.dispatch(command());

        verify(subscriptionRepository).deleteByEndpoint("e1");
    }
}
