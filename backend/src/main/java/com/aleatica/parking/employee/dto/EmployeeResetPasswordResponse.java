package com.aleatica.parking.employee.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Respuesta del reset administrativo de contrasena
 * (schema {@code EmployeeResetPasswordResponse} de la API).
 *
 * <p>En Fase 1 🟢 {@code temporaryPassword} lleva la contrasena temporal para
 * mostrarla una sola vez en pantalla; en Fase 2 🔵 se envia por email y el campo
 * viaja {@code null} (no se expone el secreto en la API).</p>
 *
 * @param temporaryPassword contrasena temporal en claro (solo Fase 1; {@code null} en Fase 2)
 * @param mustChange        siempre {@code true}: obliga al cambio en el primer acceso
 */
@Schema(description = "Resultado del reset de contrasena")
public record EmployeeResetPasswordResponse(
        @Schema(description = "Contrasena temporal (solo Fase 1; null en Fase 2)")
        String temporaryPassword,

        @Schema(description = "Obliga a cambiar la contrasena en el proximo acceso")
        boolean mustChange) {

    /**
     * Respuesta de Fase 1: devuelve la contrasena temporal en claro.
     *
     * @param temporaryPassword contrasena temporal generada
     * @return la respuesta con {@code mustChange = true}
     */
    public static EmployeeResetPasswordResponse phase1(String temporaryPassword) {
        return new EmployeeResetPasswordResponse(temporaryPassword, true);
    }

    /**
     * Respuesta de Fase 2: no expone la contrasena (se envio por email).
     *
     * @return la respuesta con {@code temporaryPassword = null} y {@code mustChange = true}
     */
    public static EmployeeResetPasswordResponse phase2() {
        return new EmployeeResetPasswordResponse(null, true);
    }
}
