/**
 * Modulo de autenticacion (Fase 1, login local).
 *
 * <p>Expone los endpoints {@code /api/v1/auth/login|logout|me|change-password}
 * ({@link com.aleatica.parking.auth.AuthController}). El caso de uso
 * {@link com.aleatica.parking.auth.application.AuthService} verifica las
 * credenciales (BCrypt coste 12), gestiona el bloqueo por intentos fallidos y el
 * cambio de contrasena; la sesion server-side la respalda Spring Session JDBC y
 * cada intento se registra en {@code login_log}
 * ({@link com.aleatica.parking.auth.JdbcLoginLogRecorder}). El nucleo de dominio
 * ({@link com.aleatica.parking.auth.domain.PasswordPolicy},
 * {@link com.aleatica.parking.auth.domain.ClockPort}) no depende de Spring.</p>
 */
package com.aleatica.parking.auth;
