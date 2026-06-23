package com.aleatica.parking.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.CookieSerializer.CookieValue;

/**
 * Verifica que la cookie de sesion se emite con el nombre {@code parking_SESSION}
 * y los flags de seguridad exigidos: {@code HttpOnly}, {@code SameSite=Lax} y
 * {@code Path=/parking-api}.
 */
class SessionCookieTest {

    private static final String EXPECTED_NAME = "parking_SESSION";
    private static final String EXPECTED_PATH = "/parking-api";
    private static final String SESSION_ID = "test-session-id";

    @Test
    void should_create_session_cookie_named_parking_SESSION_with_flags() {
        // Given
        CookieSerializer serializer = new SessionConfig().cookieSerializer(false);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // When
        serializer.writeCookieValue(new CookieValue(request, response, SESSION_ID));

        // Then
        Cookie cookie = response.getCookie(EXPECTED_NAME);
        assertThat(cookie).isNotNull();
        assertThat(cookie.getName()).isEqualTo(EXPECTED_NAME);
        assertThat(cookie.getPath()).isEqualTo(EXPECTED_PATH);
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getMaxAge()).isEqualTo(SessionConfig.SESSION_TTL_SECONDS);

        String setCookieHeader = response.getHeader("Set-Cookie");
        assertThat(setCookieHeader).contains("SameSite=Lax");
    }

    @Test
    void should_not_mark_cookie_secure_when_secure_flag_disabled() {
        // Given
        CookieSerializer serializer = new SessionConfig().cookieSerializer(false);
        MockHttpServletResponse response = new MockHttpServletResponse();

        // When
        serializer.writeCookieValue(
                new CookieValue(new MockHttpServletRequest(), response, SESSION_ID));

        // Then
        assertThat(response.getCookie(EXPECTED_NAME)).isNotNull();
        assertThat(response.getCookie(EXPECTED_NAME).getSecure()).isFalse();
    }

    @Test
    void should_mark_cookie_secure_when_secure_flag_enabled() {
        // Given
        CookieSerializer serializer = new SessionConfig().cookieSerializer(true);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSecure(true);

        // When
        serializer.writeCookieValue(new CookieValue(request, response, SESSION_ID));

        // Then
        assertThat(response.getCookie(EXPECTED_NAME)).isNotNull();
        assertThat(response.getCookie(EXPECTED_NAME).getSecure()).isTrue();
    }
}
