package com.aleatica.parking.notification.application;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Test unitario de {@link EmailRetryJob}: el disparo del job delega en el reintento del
 * servicio de entrega. La programacion temporal real no se testea aqui (S2925: se invoca el
 * metodo directamente, sin {@code Thread.sleep} ni temporizadores).
 */
@ExtendWith(MockitoExtension.class)
class EmailRetryJobTest {

    @Mock
    private NotificationDeliveryService deliveryService;

    @Test
    void shouldDelegateToRetryPending_whenJobFires() {
        // Act
        new EmailRetryJob(deliveryService).retryPendingEmails();

        // Assert
        verify(deliveryService).retryPending();
    }
}
