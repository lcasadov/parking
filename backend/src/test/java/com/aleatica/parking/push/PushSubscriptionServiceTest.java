package com.aleatica.parking.push;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.aleatica.parking.employee.Employee;
import com.aleatica.parking.employee.EmployeeRepository;
import com.aleatica.parking.employee.Role;
import com.aleatica.parking.support.EmployeeTestFactory;
import jakarta.persistence.EntityNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PushSubscriptionServiceTest {

    @Mock
    private PushSubscriptionRepository subscriptionRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    private PushSubscriptionService service() {
        return new PushSubscriptionService(subscriptionRepository, employeeRepository);
    }

    private Employee emp(long id) {
        return EmployeeTestFactory.active(id, "u" + id, "u" + id + "@aleatica.com", null, Role.EMPLOYEE);
    }

    @Test
    void shouldInsertWhenNew_whenSubscribing() {
        given(employeeRepository.findByLogin("u10")).willReturn(Optional.of(emp(10L)));
        given(subscriptionRepository.findByEndpoint("e")).willReturn(Optional.empty());

        service().subscribe("u10", "e", "p", "a", "ua");

        verify(subscriptionRepository).save(any(PushSubscription.class));
        verify(subscriptionRepository, never()).deleteByEndpoint(any());
    }

    @Test
    void shouldUpsertWhenEndpointExists_whenSubscribing() {
        given(employeeRepository.findByLogin("u10")).willReturn(Optional.of(emp(10L)));
        given(subscriptionRepository.findByEndpoint("e"))
                .willReturn(Optional.of(PushSubscription.of(99L, "e", "x", "y", null)));

        service().subscribe("u10", "e", "p", "a", "ua");

        verify(subscriptionRepository).deleteByEndpoint("e");
        verify(subscriptionRepository).save(any(PushSubscription.class));
    }

    @Test
    void shouldThrow_whenLoginHasNoEmployee() {
        given(employeeRepository.findByLogin("ghost")).willReturn(Optional.empty());
        PushSubscriptionService service = service();

        assertThatThrownBy(() -> service.subscribe("ghost", "e", "p", "a", null))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void shouldDeleteOwnSubscription_whenUnsubscribing() {
        given(employeeRepository.findByLogin("u10")).willReturn(Optional.of(emp(10L)));
        given(subscriptionRepository.findByEndpoint("e"))
                .willReturn(Optional.of(PushSubscription.of(10L, "e", "p", "a", null)));

        service().unsubscribe("u10", "e");

        verify(subscriptionRepository).deleteByEndpoint("e");
    }

    @Test
    void shouldNotDeleteOthersSubscription_whenUnsubscribing() {
        given(employeeRepository.findByLogin("u10")).willReturn(Optional.of(emp(10L)));
        given(subscriptionRepository.findByEndpoint("e"))
                .willReturn(Optional.of(PushSubscription.of(20L, "e", "p", "a", null)));

        service().unsubscribe("u10", "e");

        verify(subscriptionRepository, never()).deleteByEndpoint(any());
    }

    @Test
    void shouldDeleteAll_whenEmployeeDeactivated() {
        service().deleteAllForEmployee(10L);
        verify(subscriptionRepository).deleteByEmployeeId(10L);
    }
}
