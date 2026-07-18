/**
 * Bootstrap del primer administrador en despliegues limpios.
 *
 * <p>Contiene el mecanismo opt-in que, al arrancar, crea el administrador inicial a
 * partir de configuracion inyectada por entorno cuando aun no existe ningun
 * {@code ADMIN} (perfiles {@code docker}/{@code pro}, donde el seed de desarrollo
 * esta excluido a proposito). Es idempotente y no tumba el arranque ante credenciales
 * ausentes o colisiones.</p>
 */
package com.aleatica.parking.employee.bootstrap;
