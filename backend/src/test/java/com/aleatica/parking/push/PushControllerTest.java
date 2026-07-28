package com.aleatica.parking.push;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aleatica.parking.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PushController.class)
@Import(SecurityConfig.class)
class PushControllerTest {

    private static final String SUBSCRIPTIONS = "/api/v1/push/subscriptions";
    private static final String VAPID = "/api/v1/push/vapid-public-key";
    private static final String SUB_BODY =
            "{\"endpoint\":\"https://push/e\",\"keys\":{\"p256dh\":\"p\",\"auth\":\"a\"}}";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PushSubscriptionService subscriptionService;

    @MockBean
    private WebPushSender webPushSender;

    @Test
    void shouldReturn401_whenGettingVapidKeyWithoutSession() throws Exception {
        mockMvc.perform(get(VAPID)).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnVapidKey_whenAuthenticated() throws Exception {
        given(webPushSender.publicKey()).willReturn("pk.test");
        mockMvc.perform(get(VAPID).with(user("emp").roles("EMPLOYEE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicKey").value("pk.test"));
    }

    @Test
    void shouldSubscribe_whenAuthenticated() throws Exception {
        mockMvc.perform(post(SUBSCRIPTIONS).with(user("emp").roles("EMPLOYEE"))
                        .contentType(MediaType.APPLICATION_JSON).content(SUB_BODY))
                .andExpect(status().isNoContent());
        verify(subscriptionService).subscribe(eq("emp"), eq("https://push/e"), eq("p"), eq("a"), any());
    }

    @Test
    void shouldReturn400_whenSubscribingWithoutKeys() throws Exception {
        mockMvc.perform(post(SUBSCRIPTIONS).with(user("emp").roles("EMPLOYEE"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"endpoint\":\"e\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldUnsubscribe_whenAuthenticated() throws Exception {
        mockMvc.perform(delete(SUBSCRIPTIONS).with(user("emp").roles("EMPLOYEE"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"endpoint\":\"https://push/e\"}"))
                .andExpect(status().isNoContent());
        verify(subscriptionService).unsubscribe(eq("emp"), anyString());
    }

    @Test
    void shouldReturn401_whenSubscribingWithoutSession() throws Exception {
        mockMvc.perform(post(SUBSCRIPTIONS)
                        .contentType(MediaType.APPLICATION_JSON).content(SUB_BODY))
                .andExpect(status().isUnauthorized());
    }
}
