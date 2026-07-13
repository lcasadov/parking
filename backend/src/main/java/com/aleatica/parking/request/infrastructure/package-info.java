/**
 * Infraestructura de persistencia del agregado de solicitudes (arquitectura hexagonal, change
 * {@code hexagonal-persistence}): la entidad JPA
 * {@link com.aleatica.parking.request.infrastructure.RequestEntity}, el repositorio Spring Data
 * {@link com.aleatica.parking.request.infrastructure.RequestJpaRepository}, el mapper a mano
 * {@link com.aleatica.parking.request.infrastructure.RequestMapper} y el adaptador
 * {@link com.aleatica.parking.request.infrastructure.RequestPersistenceAdapter} que implementa
 * el puerto de dominio {@code RequestRepositoryPort}.
 *
 * <p>Es el unico paquete del agregado que conoce JPA/Hibernate. La frontera transaccional y de
 * concurrencia (indices unicos filtrados &rarr; 409) no cambia respecto al diseno previo
 * (design §D5).</p>
 */
package com.aleatica.parking.request.infrastructure;
