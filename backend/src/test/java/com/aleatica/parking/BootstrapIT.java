package com.aleatica.parking;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aleatica.parking.support.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * Tests de integracion del arranque: smoke del contexto, endpoint de health,
 * seguridad minima (401), endpoints de auth reales (login 401 sin credenciales
 * validas; me/logout/change-password 401 sin sesion) y forma uniforme del error
 * (ApiError) ante validacion.
 */
class BootstrapIT extends BaseIntegrationTest {

    private static final String HEALTH_PATH = "/api/v1/health";
    private static final String LOGIN_PATH = "/api/v1/auth/login";
    private static final String LOGOUT_PATH = "/api/v1/auth/logout";
    private static final String ME_PATH = "/api/v1/auth/me";
    private static final String CHANGE_PASSWORD_PATH = "/api/v1/auth/change-password";
    private static final String PROTECTED_PATH = "/api/v1/secured-probe";

    private static final String VALID_LOGIN_BODY = "{\"login\":\"jperez\",\"password\":\"secret\"}";
    private static final String INVALID_LOGIN_BODY = "{\"login\":\"\",\"password\":\"\"}";

    // 1.1
    @Test
    void should_load_spring_context_when_app_starts() throws Exception {
        // El contexto arranca (BaseIntegrationTest) y MockMvc esta inyectado:
        // un GET al health confirma que la aplicacion responde.
        mockMvc.perform(get(HEALTH_PATH))
                .andExpect(status().isOk());
    }

    // 1.3
    @Test
    void should_return_200_up_when_get_health_unauthenticated() throws Exception {
        mockMvc.perform(get(HEALTH_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    // 1.4
    @Test
    void should_return_401_when_access_protected_endpoint_unauthenticated() throws Exception {
        mockMvc.perform(get(PROTECTED_PATH))
                .andExpect(status().isUnauthorized());
    }

    // 1.5 — los endpoints de auth ya son reales (auth-local sustituye el placeholder 501).
    @Test
    void should_return_401_when_login_with_unknown_credentials() throws Exception {
        // El login jperez no existe -> 401 generico (no revela la inexistencia).
        mockMvc.perform(post(LOGIN_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_LOGIN_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void should_return_401_when_logout_without_session() throws Exception {
        mockMvc.perform(post(LOGOUT_PATH))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void should_return_401_when_me_without_session() throws Exception {
        mockMvc.perform(get(ME_PATH))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void should_return_401_when_change_password_without_session() throws Exception {
        mockMvc.perform(post(CHANGE_PASSWORD_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"old-secret\",\"newPassword\":\"new-secret\"}"))
                .andExpect(status().isUnauthorized());
    }

    // 1.6 — forma uniforme del error { error, message, fields, timestamp }.
    @Test
    void should_return_apierror_shape_when_validation_fails() throws Exception {
        mockMvc.perform(post(LOGIN_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(INVALID_LOGIN_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.fields").exists())
                .andExpect(jsonPath("$.fields.login").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
