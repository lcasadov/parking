/**
 * Casos de uso del modulo de visitantes: creacion/edicion/consulta/listado de fichas
 * ({@link com.aleatica.parking.visitor.application.VisitorService}) y creacion/listado/
 * anulacion de reservas ({@link com.aleatica.parking.visitor.application.VisitorReservationService}),
 * junto con las excepciones de negocio, el puerto de auditoria
 * ({@link com.aleatica.parking.visitor.application.VisitorAuditPort}), su adaptador de log
 * y el listener {@code AFTER_COMMIT}.
 *
 * <p>La logica no depende de la web y nunca devuelve entidades JPA (OWASP API3). La
 * unicidad de {@code nationalId} y de plaza+fecha se comprueba en el servicio (mensaje
 * claro) y se blinda con los indices unicos de la BD (409 bajo concurrencia). La
 * disponibilidad de plaza y la ventana de anulacion (solo futuras) consolidaran
 * {@code availability-calendar} (B7); la auditoria consolidara {@code audit-retention}
 * (B10).</p>
 */
package com.aleatica.parking.visitor.application;
