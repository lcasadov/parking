package com.aleatica.parking.push;

import java.security.Security;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.apache.http.HttpResponse;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Adaptador de envio Web Push (VAPID) sobre la libreria {@code nl.martijndwars:web-push}
 * (change {@code push-notifications}).
 *
 * <p>Arranque TOLERANTE: si faltan las claves VAPID (publica/privada), queda
 * {@link #isEnabled() deshabilitado} y {@link #send} devuelve {@link Outcome#FAILED} sin
 * lanzar — el sistema sigue funcionando solo con email. El resultado distingue el caso
 * {@link Outcome#EXPIRED} (404/410 del push service) para que el llamante borre la suscripcion.</p>
 */
@Component
public class WebPushSender {

    /** Resultado del intento de envio a una suscripcion. */
    public enum Outcome { SENT, EXPIRED, FAILED }

    private static final Logger log = LoggerFactory.getLogger(WebPushSender.class);

    static {
        // web-push usa "BC" como provider JCE para las claves EC/VAPID pero NO lo registra por si
        // mismo (a diferencia de versiones antiguas). Sin este registro, construir PushService lanza
        // "no such provider: BC" y el canal push queda deshabilitado. Se registra una sola vez.
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private final String publicKey;
    private final PushService pushService;

    public WebPushSender(
            @Value("${parking.push.vapid.public-key:}") String publicKey,
            @Value("${parking.push.vapid.private-key:}") String privateKey,
            @Value("${parking.push.vapid.subject:mailto:no-reply@parking.aleatica.com}") String subject) {
        this.publicKey = publicKey;
        PushService svc = null;
        if (isBlank(publicKey) || isBlank(privateKey)) {
            log.warn("[push] VAPID sin configurar: el canal push queda deshabilitado (solo email).");
        } else {
            try {
                svc = new PushService(publicKey, privateKey, subject);
            } catch (Throwable t) {  // NOSONAR S1181: arranque tolerante — incluso un Error de lib nativa (BC) deja push off, nunca tumba la app
                log.warn("[push] No se pudo inicializar PushService (VAPID invalido o BouncyCastle ausente?): {}", t.getMessage());
            }
        }
        this.pushService = svc;
    }

    /** @return {@code true} si el canal push esta operativo (VAPID configurado correctamente). */
    public boolean isEnabled() {
        return pushService != null;
    }

    /** @return la clave publica VAPID para que el cliente se suscriba (vacia si sin configurar). */
    public String publicKey() {
        return publicKey;
    }

    /**
     * Envia un payload cifrado a una suscripcion. Nunca lanza: mapea el resultado a {@link Outcome}.
     *
     * @param endpoint    endpoint del push service
     * @param p256dh      clave publica del cliente
     * @param auth        secreto de autenticacion del cliente
     * @param payloadJson cuerpo JSON (titulo/cuerpo/url) que interpretara el Service Worker
     * @return el resultado del intento
     */
    public Outcome send(String endpoint, String p256dh, String auth, String payloadJson) {
        if (pushService == null) {
            return Outcome.FAILED;
        }
        try {
            Notification notification =
                    new Notification(new Subscription(endpoint, new Subscription.Keys(p256dh, auth)), payloadJson);
            HttpResponse response = pushService.send(notification);
            int status = response.getStatusLine().getStatusCode();
            if (status >= 200 && status < 300) {
                return Outcome.SENT;
            }
            if (status == 404 || status == 410) {
                return Outcome.EXPIRED;
            }
            log.warn("[push] Envio fallido (status {}) a endpoint {}", status, shorten(endpoint));
            return Outcome.FAILED;
        } catch (Exception e) {  // NOSONAR: best-effort — cualquier fallo no debe romper el flujo
            log.warn("[push] Error enviando a {}: {}", shorten(endpoint), e.getMessage());
            return Outcome.FAILED;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String shorten(String endpoint) {
        return endpoint == null || endpoint.length() <= 40 ? endpoint : endpoint.substring(0, 40) + "...";
    }
}
