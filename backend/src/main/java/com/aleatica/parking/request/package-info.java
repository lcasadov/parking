/**
 * Modulo de solicitudes puntuales, estructurado en arquitectura hexagonal (change
 * {@code hexagonal-persistence}):
 * <ul>
 *   <li>{@code request.domain}: el modelo de dominio
 *       {@link com.aleatica.parking.request.domain.Request} con su maquina de estados, los value
 *       objects de estado/motivo y el puerto
 *       {@link com.aleatica.parking.request.domain.RequestRepositoryPort}.</li>
 *   <li>{@code request.application}: los casos de uso
 *       ({@link com.aleatica.parking.request.application.RequestService}) y las excepciones de
 *       aplicacion, dependientes del puerto y del dominio.</li>
 *   <li>{@code request.infrastructure}: la entidad JPA, el repositorio Spring Data, el mapper y
 *       el adaptador de persistencia.</li>
 *   <li>este paquete raiz: el adaptador web
 *       {@link com.aleatica.parking.request.RequestController} y los {@code dto/}.</li>
 * </ul>
 *
 * <p>Una solicitud es la peticion de un empleado de un recurso para una fecha, con ciclo de vida
 * {@code PENDING} &rarr; {@code APPROVED} | {@code REJECTED} | {@code CANCELLED}. La validacion
 * temporal (hoy o fecha futura; se rechaza la fecha pasada) vive en el caso de uso con
 * {@code ClockPort}; la unicidad
 * {@code PENDING} por empleado/tipo/fecha y la de recurso {@code APPROVED} por fecha las
 * garantizan indices unicos filtrados de la BD (409, incluida la concurrencia entre
 * administradores). El listado y la cancelacion propios aplican verificacion de pertenencia
 * (BOLA), no solo RBAC. Las notificaciones se disparan {@code AFTER_COMMIT}.</p>
 */
package com.aleatica.parking.request;
