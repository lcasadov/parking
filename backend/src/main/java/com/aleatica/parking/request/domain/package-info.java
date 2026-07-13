/**
 * Dominio del agregado de solicitudes (arquitectura hexagonal, change
 * {@code hexagonal-persistence}): el modelo de dominio
 * {@link com.aleatica.parking.request.domain.Request} con su maquina de estados, los value
 * objects {@link com.aleatica.parking.request.domain.RequestStatus} y
 * {@link com.aleatica.parking.request.domain.RejectionReasonCode}, y el puerto de salida de
 * persistencia {@link com.aleatica.parking.request.domain.RequestRepositoryPort}.
 *
 * <p><strong>Libre de framework</strong>: ninguna clase de este paquete depende de
 * {@code jakarta.persistence}, {@code org.hibernate} ni de Spring Data Repository. La unica
 * concesion es {@code org.springframework.data.domain.Page}/{@code Pageable} en el puerto,
 * value types estables de paginacion (design §D4). La regla la verifica
 * {@code HexagonalArchitectureTest}.</p>
 */
package com.aleatica.parking.request.domain;
