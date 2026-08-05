package com.aleatica.parking.push;

import com.aleatica.parking.push.dto.PushSubscriptionRequest;
import com.aleatica.parking.push.dto.PushUnsubscribeRequest;
import com.aleatica.parking.push.dto.VapidKeyResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de suscripcion Web Push (change {@code push-notifications}). Cualquier usuario
 * autenticado gestiona <strong>su propia</strong> suscripcion; la clave publica VAPID se expone
 * para que el navegador pueda suscribirse.
 */
@Tag(name = "Push", description = "Suscripciones Web Push")
@RestController
@RequestMapping("/api/v1/push")
public class PushController {

    private final PushSubscriptionService subscriptionService;
    private final WebPushSender webPushSender;

    public PushController(PushSubscriptionService subscriptionService, WebPushSender webPushSender) {
        this.subscriptionService = subscriptionService;
        this.webPushSender = webPushSender;
    }

    /**
     * Alta idempotente de la suscripcion del usuario autenticado (upsert por endpoint).
     *
     * @param request        suscripcion del navegador (endpoint + claves)
     * @param userAgent      cabecera User-Agent (identifica el dispositivo; opcional)
     * @param authentication usuario autenticado
     * @return {@code 204}
     */
    @Operation(summary = "Registra la suscripcion Web Push del usuario")
    @PostMapping("/subscriptions")
    public ResponseEntity<Void> subscribe(
            @Valid @RequestBody PushSubscriptionRequest request,
            @RequestHeader(name = "User-Agent", required = false) String userAgent,
            Authentication authentication) {
        subscriptionService.subscribe(
                authentication.getName(), request.endpoint(),
                request.keys().p256dh(), request.keys().auth(), userAgent);
        return ResponseEntity.noContent().build();
    }

    /**
     * Baja de una suscripcion del propio usuario (por endpoint).
     *
     * @param request        endpoint a dar de baja
     * @param authentication usuario autenticado
     * @return {@code 204}
     */
    @Operation(summary = "Da de baja una suscripcion Web Push del usuario")
    @DeleteMapping("/subscriptions")
    public ResponseEntity<Void> unsubscribe(
            @Valid @RequestBody PushUnsubscribeRequest request, Authentication authentication) {
        subscriptionService.unsubscribe(authentication.getName(), request.endpoint());
        return ResponseEntity.noContent().build();
    }

    /**
     * Devuelve la clave publica VAPID para suscribir el navegador.
     *
     * @return {@code 200} con la clave publica (vacia si push no esta configurado)
     */
    @Operation(summary = "Clave publica VAPID")
    @GetMapping("/vapid-public-key")
    public ResponseEntity<VapidKeyResponse> vapidPublicKey() {
        return ResponseEntity.status(HttpStatus.OK)
                .body(new VapidKeyResponse(webPushSender.publicKey()));
    }
}
