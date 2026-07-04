package com.aleatica.parking.notification.infrastructure;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Habilita la planificacion de tareas para el job de reintento de emails
 * ({@code EmailRetryJob}). El periodo concreto se configura via
 * {@code parking.notifications.retry-interval-ms}.
 */
@Configuration
@EnableScheduling
public class NotificationSchedulingConfig {
}
