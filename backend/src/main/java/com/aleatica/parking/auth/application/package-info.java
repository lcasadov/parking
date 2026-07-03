/**
 * Capa de aplicacion de autenticacion: el caso de uso
 * {@link com.aleatica.parking.auth.application.AuthService} y sus puertos de
 * salida ({@link com.aleatica.parking.auth.application.LoginLogRecorder}).
 *
 * <p>Orquesta el dominio ({@code PasswordPolicy}, {@code ClockPort}) y el
 * repositorio de empleados; no conoce HTTP ni la sesion.</p>
 */
package com.aleatica.parking.auth.application;
