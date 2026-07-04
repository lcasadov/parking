package com.aleatica.parking.notification.application;

/**
 * Fallo de entrega SMTP de un email. La captura el servicio de entrega para registrar el
 * fallo y encolar el email para reintento, sin propagar el error al flujo de negocio ya
 * confirmado.
 */
public class EmailDeliveryException extends RuntimeException {

    /**
     * @param message descripcion del fallo
     * @param cause   causa raiz (excepcion del proveedor de correo)
     */
    public EmailDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
