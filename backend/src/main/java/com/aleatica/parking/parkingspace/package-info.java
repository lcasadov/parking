/**
 * Modulo de plazas de parking: la entidad de persistencia
 * {@link com.aleatica.parking.parkingspace.ParkingSpace}, su repositorio JPA, los
 * casos de uso de gestion ({@link com.aleatica.parking.parkingspace.application.ParkingSpaceService})
 * y el adaptador web ({@link com.aleatica.parking.parkingspace.ParkingSpaceController}).
 *
 * <p>{@code ParkingSpace} es el recurso reservable vivo del nucleo de parking:
 * todas las FKs reservables ({@code fixed_assignments}, {@code releases},
 * {@code requests}, {@code visitor_reservations}) apuntan a el. La gestion del
 * catalogo (alta, edicion, configuracion del total) es exclusiva de {@code ADMIN}.</p>
 */
package com.aleatica.parking.parkingspace;
