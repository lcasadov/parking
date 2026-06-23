package com.aleatica.parking.auth;

import com.aleatica.parking.auth.dto.ChangePasswordRequest;
import com.aleatica.parking.auth.dto.LoginRequest;
import com.aleatica.parking.exception.ApiError;
import com.aleatica.parking.exception.NotImplementedException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints placeholder de autenticacion de Fase 1.
 *
 * <p>Las rutas existen en el contrato y son publicas, pero todavia no tienen
 * logica: devuelven {@code 501 Not Implemented} con el cuerpo de error uniforme
 * {@link ApiError}. La autenticacion real (verificacion BCrypt, bloqueo de
 * cuenta, sesion) la implementa el change funcional de {@code auth-local}.</p>
 */
@Tag(name = "Auth", description = "Autenticacion de Fase 1 (placeholder)")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String NOT_IMPLEMENTED_MESSAGE =
            "Endpoint de autenticacion aun no implementado (Fase 1 pendiente)";

    private static final String RESPONSE_501_DESC = "Funcionalidad aun no implementada";

    /**
     * Placeholder de inicio de sesion.
     *
     * @param request credenciales (validadas sintacticamente)
     * @throws NotImplementedException siempre, hasta que {@code auth-local} lo implemente
     */
    @Operation(summary = "Iniciar sesion (placeholder)",
            description = "Validara las credenciales y creara la sesion. Aun no implementado.")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Datos invalidos",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "501", description = RESPONSE_501_DESC,
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/login")
    public void login(@Valid @RequestBody LoginRequest request) {
        throw new NotImplementedException(NOT_IMPLEMENTED_MESSAGE);
    }

    /**
     * Placeholder de cierre de sesion.
     *
     * @throws NotImplementedException siempre, hasta que {@code auth-local} lo implemente
     */
    @Operation(summary = "Cerrar sesion (placeholder)")
    @ApiResponses(@ApiResponse(responseCode = "501", description = RESPONSE_501_DESC,
            content = @Content(schema = @Schema(implementation = ApiError.class))))
    @PostMapping("/logout")
    public void logout() {
        throw new NotImplementedException(NOT_IMPLEMENTED_MESSAGE);
    }

    /**
     * Placeholder de consulta del usuario autenticado.
     *
     * @throws NotImplementedException siempre, hasta que {@code auth-local} lo implemente
     */
    @Operation(summary = "Usuario actual (placeholder)")
    @ApiResponses(@ApiResponse(responseCode = "501", description = RESPONSE_501_DESC,
            content = @Content(schema = @Schema(implementation = ApiError.class))))
    @GetMapping("/me")
    public void me() {
        throw new NotImplementedException(NOT_IMPLEMENTED_MESSAGE);
    }

    /**
     * Placeholder de cambio de contrasena.
     *
     * @param request datos del cambio (validados sintacticamente)
     * @throws NotImplementedException siempre, hasta que {@code auth-local} lo implemente
     */
    @Operation(summary = "Cambiar contrasena (placeholder)")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Datos invalidos",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "501", description = RESPONSE_501_DESC,
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/change-password")
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        throw new NotImplementedException(NOT_IMPLEMENTED_MESSAGE);
    }
}
