package com.aleatica.parking.notification;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Adaptador de salida de persistencia del almacen de reintento de emails
 * (Spring Data JPA).
 *
 * <p>El job de reintento consulta unicamente las entradas {@link EmailOutboxStatus#PENDING},
 * garantizando que las {@code SENT}/{@code FAILED} nunca se reprocesan (idempotencia). La
 * consulta usa un parametro vinculado (sin concatenacion), eliminando la inyeccion SQL por
 * construccion.</p>
 */
public interface EmailOutboxRepository extends JpaRepository<EmailOutbox, Long> {

    /**
     * Devuelve las entradas del almacen en un estado dado.
     *
     * @param status estado por el que filtrar
     * @return lista de entradas en ese estado (posiblemente vacia)
     */
    List<EmailOutbox> findByStatus(EmailOutboxStatus status);
}
