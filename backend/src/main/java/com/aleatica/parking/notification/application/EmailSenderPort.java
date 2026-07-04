package com.aleatica.parking.notification.application;

/**
 * Puerto de salida para el envio efectivo de un email por SMTP.
 *
 * <p>Arquitectura hexagonal: el nucleo de {@code notifications} depende de este puerto,
 * no del {@code JavaMailSender} concreto, lo que permite sustituir el canal (SMTP real,
 * doble de test) sin tocar la logica de resolucion/resiliencia. En produccion lo
 * implementa el adaptador SMTP; en los tests, un doble controlable.</p>
 */
public interface EmailSenderPort {

    /**
     * Envia el mensaje por SMTP.
     *
     * @param message mensaje ya renderizado (destinatario, asunto, cuerpo HTML)
     * @throws EmailDeliveryException si el servidor SMTP rechaza o no responde
     */
    void send(EmailMessage message);
}
