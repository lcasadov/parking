/**
 * Capa de aplicacion de {@code notifications} (arquitectura hexagonal): el puerto de salida
 * {@link com.aleatica.parking.notification.application.EmailSenderPort}, el listener
 * {@code AFTER_COMMIT} ({@link com.aleatica.parking.notification.application.EmailNotificationListener}),
 * la resolucion de destinatarios y contenido
 * ({@link com.aleatica.parking.notification.application.NotificationDispatcher},
 * {@link com.aleatica.parking.notification.application.EmailContentRenderer}), la entrega
 * resiliente con encolado y reintento
 * ({@link com.aleatica.parking.notification.application.NotificationDeliveryService}) y el
 * job programado ({@link com.aleatica.parking.notification.application.EmailRetryJob}).
 */
package com.aleatica.parking.notification.application;
