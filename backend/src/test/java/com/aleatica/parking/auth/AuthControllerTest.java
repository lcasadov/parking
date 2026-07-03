package com.aleatica.parking.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aleatica.parking.auth.application.AuthService;
import com.aleatica.parking.auth.application.AuthenticatedUser;
import com.aleatica.parking.auth.application.AuthenticationFailedException;
import com.aleatica.parking.auth.application.InvalidCurrentPasswordException;
import com.aleatica.parking.config.SecurityConfig;
import com.aleatica.parking.employee.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Tests del adaptador web de autenticacion (@WebMvcTest): estados HTTP, emision
 * de cookie de sesion, validacion de DTO y proteccion RBAC (401 sin sesion).
 * Flujo critico de autorizacion: cobertura de los caminos de seguridad.
 */
@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String ME_URL = "/api/v1/auth/me";
    private static final String LOGOUT_URL = "/api/v1/auth/logout";
    private static final String CHANGE_PWD_URL = "/api/v1/auth/change-password";
    private static final String LOGIN = "jperez";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @Test
    void shouldReturn200AndIssueSessionCookie_whenLoginValid() throws Exception {
        // Arrange
        given(authService.authenticate(eq(LOGIN), any()))
                .willReturn(new AuthenticatedUser(7L, LOGIN, "Juan", "Perez", Role.ADMIN, false));

        // Act / Assert
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"jperez\",\"password\":\"Secret#Pass1word\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeId").value(7))
                .andExpect(jsonPath("$.login").value(LOGIN))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.passwordMustChange").value(false))
                .andExpect(request().sessionAttribute(
                        "SPRING_SECURITY_CONTEXT", org.hamcrest.Matchers.notNullValue()));
    }

    @Test
    void shouldReturn401WithGenericMessage_whenCredentialsInvalid() throws Exception {
        // Arrange
        willThrow(new AuthenticationFailedException())
                .given(authService).authenticate(eq(LOGIN), any());

        // Act / Assert
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"jperez\",\"password\":\"bad\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message")
                        .value(AuthenticationFailedException.GENERIC_MESSAGE));
    }

    @Test
    void shouldReturn400_whenLoginBodyMissingFields() throws Exception {
        // Act / Assert: empty password fails @NotBlank
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"jperez\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldReturn401_whenMeCalledWithoutSession() throws Exception {
        // Act / Assert
        mockMvc.perform(get(ME_URL))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnCurrentUser_whenMeCalledAuthenticated() throws Exception {
        // Arrange
        given(authService.loadByLogin(LOGIN))
                .willReturn(new AuthenticatedUser(7L, LOGIN, "Juan", "Perez", Role.EMPLOYEE, false));

        // Act / Assert
        mockMvc.perform(get(ME_URL).with(user(LOGIN).roles("EMPLOYEE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value(LOGIN))
                .andExpect(jsonPath("$.role").value("EMPLOYEE"));
    }

    @Test
    void shouldReturn401_whenLogoutCalledWithoutSession() throws Exception {
        // Act / Assert
        mockMvc.perform(post(LOGOUT_URL))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn204_whenLogoutAuthenticated() throws Exception {
        // Act / Assert
        mockMvc.perform(post(LOGOUT_URL).with(user(LOGIN).roles("ADMIN")))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturn401_whenChangePasswordWithoutSession() throws Exception {
        // Act / Assert
        mockMvc.perform(post(CHANGE_PWD_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"old\",\"newPassword\":\"New#Pass1word\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn204_whenChangePasswordValid() throws Exception {
        // Act / Assert
        mockMvc.perform(post(CHANGE_PWD_URL).with(user(LOGIN).roles("EMPLOYEE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"old\",\"newPassword\":\"New#Pass1word\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturn400WithCurrentPasswordField_whenCurrentPasswordWrong() throws Exception {
        // Arrange
        willThrow(new InvalidCurrentPasswordException())
                .given(authService).changePassword(eq(LOGIN), any(), any());

        // Act / Assert
        mockMvc.perform(post(CHANGE_PWD_URL).with(user(LOGIN).roles("EMPLOYEE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"wrong\",\"newPassword\":\"New#Pass1word\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.currentPassword").exists());
    }

    @Test
    void shouldReturn400_whenChangePasswordBodyMissingFields() throws Exception {
        // Act / Assert: blank newPassword fails @NotBlank
        mockMvc.perform(post(CHANGE_PWD_URL).with(user(LOGIN).roles("EMPLOYEE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"old\",\"newPassword\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }
}
