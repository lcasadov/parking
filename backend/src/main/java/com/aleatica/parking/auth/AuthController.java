package com.aleatica.parking.auth;

import com.aleatica.parking.auth.application.AuthService;
import com.aleatica.parking.auth.application.AuthenticatedUser;
import com.aleatica.parking.auth.dto.ChangePasswordRequest;
import com.aleatica.parking.auth.dto.CurrentUser;
import com.aleatica.parking.auth.dto.LoginRequest;
import com.aleatica.parking.exception.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de autenticacion local de Fase 1.
 *
 * <p>Tras un login correcto se crea la sesion server-side (Spring Session JDBC) y
 * se persiste el {@link SecurityContext} en ella, de modo que la cookie
 * {@code parking_SESSION} autentica las siguientes peticiones. El logout invalida
 * la sesion (invalidacion inmediata server-side, no solo borrado en cliente).</p>
 */
@Tag(name = "Auth", description = "Autenticacion local de Fase 1")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String ROLE_PREFIX = "ROLE_";

    private final AuthService authService;
    private final SecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();
    private final SecurityContextHolderStrategy securityContextHolderStrategy =
            SecurityContextHolder.getContextHolderStrategy();

    /**
     * @param authService caso de uso de autenticacion local
     */
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Inicia sesion con login y contrasena (Fase 1).
     *
     * @param loginRequest credenciales validadas sintacticamente
     * @param request      peticion HTTP (para persistir el contexto en la sesion)
     * @param response     respuesta HTTP (para emitir la cookie de sesion)
     * @return {@code 200} con la identidad ({@link CurrentUser})
     */
    @Operation(summary = "Iniciar sesion",
            description = "Autentica con login y contrasena (BCrypt). Bloqueo tras 5 intentos "
                    + "fallidos durante 15 min. Emite la cookie parking_SESSION.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autenticado; cookie de sesion emitida",
                    content = @Content(schema = @Schema(implementation = CurrentUser.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Credenciales invalidas o cuenta no disponible",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<CurrentUser> login(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        AuthenticatedUser user = authService.authenticate(loginRequest.login(), loginRequest.password());
        establishSession(user, request, response);
        return ResponseEntity.ok(CurrentUser.from(user));
    }

    /**
     * Cierra la sesion actual invalidandola en el servidor.
     *
     * @param request peticion HTTP autenticada
     * @return {@code 204} sin contenido
     */
    @Operation(summary = "Cerrar sesion", security = @SecurityRequirement(name = "sessionCookie"))
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Sesion invalidada"),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        securityContextHolderStrategy.clearContext();
        return ResponseEntity.noContent().build();
    }

    /**
     * Devuelve la identidad del usuario autenticado.
     *
     * @param authentication autenticacion resuelta de la sesion
     * @return {@code 200} con la identidad ({@link CurrentUser})
     */
    @Operation(summary = "Usuario actual", security = @SecurityRequirement(name = "sessionCookie"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario autenticado",
                    content = @Content(schema = @Schema(implementation = CurrentUser.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/me")
    public ResponseEntity<CurrentUser> me(Authentication authentication) {
        AuthenticatedUser user = authService.loadByLogin(authentication.getName());
        return ResponseEntity.ok(CurrentUser.from(user));
    }

    /**
     * Cambia la propia contrasena cumpliendo la politica.
     *
     * @param changeRequest  contrasena actual y nueva, validadas sintacticamente
     * @param authentication autenticacion resuelta de la sesion
     * @return {@code 204} si la contrasena se actualizo
     */
    @Operation(summary = "Cambiar la propia contrasena",
            description = "Politica: >=10 caracteres, mayuscula + minuscula + digito + simbolo, "
                    + "distinta de login y email.",
            security = @SecurityRequirement(name = "sessionCookie"))
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Contrasena actualizada"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos o politica incumplida",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest changeRequest,
            Authentication authentication) {
        authService.changePassword(
                authentication.getName(),
                changeRequest.currentPassword(),
                changeRequest.newPassword());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    private void establishSession(
            AuthenticatedUser user, HttpServletRequest request, HttpServletResponse response) {
        var authorities = List.of(new SimpleGrantedAuthority(ROLE_PREFIX + user.role().name()));
        Authentication authentication =
                UsernamePasswordAuthenticationToken.authenticated(user.login(), null, authorities);
        SecurityContext context = securityContextHolderStrategy.createEmptyContext();
        context.setAuthentication(authentication);
        securityContextHolderStrategy.setContext(context);
        request.getSession(true);
        securityContextRepository.saveContext(context, request, response);
    }
}
