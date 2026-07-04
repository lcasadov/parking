/**
 * Adaptadores de infraestructura de {@code notifications}: el adaptador SMTP
 * ({@link com.aleatica.parking.notification.infrastructure.SmtpEmailSender}) que implementa
 * el puerto de envio mediante {@code JavaMailSender}, y la configuracion de planificacion
 * ({@link com.aleatica.parking.notification.infrastructure.NotificationSchedulingConfig})
 * que habilita el job de reintento.
 */
package com.aleatica.parking.notification.infrastructure;
