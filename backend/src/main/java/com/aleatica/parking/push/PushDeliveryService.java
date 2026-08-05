package com.aleatica.parking.push;

import com.aleatica.parking.employee.Employee;
import com.aleatica.parking.employee.EmployeeRepository;
import com.aleatica.parking.notification.application.NotificationCommand;
import com.aleatica.parking.systemsettings.application.SystemSettingsService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Entrega de notificaciones por el canal <strong>push</strong> (change {@code push-notifications}),
 * en paralelo al email. Se auto-gatea con la regla de entrega efectiva:
 * {@code global.push AND empleado.push AND tiene suscripcion}. Best-effort: un fallo no rompe el
 * flujo; una suscripcion caducada (404/410) se borra.
 */
@Service
public class PushDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(PushDeliveryService.class);

    private final SystemSettingsService systemSettingsService;
    private final EmployeeRepository employeeRepository;
    private final PushSubscriptionRepository subscriptionRepository;
    private final WebPushSender webPushSender;
    private final PushContentRenderer contentRenderer;

    public PushDeliveryService(
            SystemSettingsService systemSettingsService,
            EmployeeRepository employeeRepository,
            PushSubscriptionRepository subscriptionRepository,
            WebPushSender webPushSender,
            PushContentRenderer contentRenderer) {
        this.systemSettingsService = systemSettingsService;
        this.employeeRepository = employeeRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.webPushSender = webPushSender;
        this.contentRenderer = contentRenderer;
    }

    /**
     * Envia el push de una orden de notificacion al destinatario, si procede.
     *
     * @param command orden (tipo de evento + destinatario + solicitud)
     */
    public void dispatch(NotificationCommand command) {
        if (!webPushSender.isEnabled() || !systemSettingsService.pushNotificationsEnabled()) {
            return;
        }
        Employee recipient = employeeRepository.findById(command.recipientEmployeeId()).orElse(null);
        if (recipient == null || !recipient.isPushNotificationsEnabled()) {
            return;
        }
        List<PushSubscription> subscriptions =
                subscriptionRepository.findByEmployeeId(command.recipientEmployeeId());
        if (subscriptions.isEmpty()) {
            return;
        }
        String payload = contentRenderer.render(command);
        for (PushSubscription subscription : subscriptions) {
            WebPushSender.Outcome outcome = webPushSender.send(
                    subscription.getEndpoint(), subscription.getP256dh(), subscription.getAuth(), payload);
            if (outcome == WebPushSender.Outcome.EXPIRED) {
                subscriptionRepository.deleteByEndpoint(subscription.getEndpoint());
                log.info("[push] Suscripcion caducada eliminada (empleado {})", command.recipientEmployeeId());
            }
        }
    }
}
