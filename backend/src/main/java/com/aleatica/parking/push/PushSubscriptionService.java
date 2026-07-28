package com.aleatica.parking.push;

import com.aleatica.parking.employee.Employee;
import com.aleatica.parking.employee.EmployeeRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso de gestion de suscripciones Web Push (change {@code push-notifications}):
 * alta idempotente, baja del propio usuario y borrado en bloque al desactivar un empleado.
 */
@Service
public class PushSubscriptionService {

    private final PushSubscriptionRepository subscriptionRepository;
    private final EmployeeRepository employeeRepository;

    public PushSubscriptionService(
            PushSubscriptionRepository subscriptionRepository,
            EmployeeRepository employeeRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.employeeRepository = employeeRepository;
    }

    /**
     * Alta idempotente de una suscripcion para el usuario autenticado (upsert por endpoint): si
     * el endpoint ya existia (mismo o distinto empleado) se reemplaza por el del usuario actual
     * con las claves nuevas.
     *
     * @param login     login del usuario autenticado
     * @param endpoint  endpoint del push service
     * @param p256dh    clave publica del cliente
     * @param auth      secreto de autenticacion del cliente
     * @param userAgent descripcion del dispositivo (opcional)
     */
    @Transactional
    public void subscribe(String login, String endpoint, String p256dh, String auth, String userAgent) {
        Long employeeId = employeeIdFor(login);
        Optional<PushSubscription> existing = subscriptionRepository.findByEndpoint(endpoint);
        if (existing.isPresent()) {
            PushSubscription current = existing.get();
            if (current.getEmployeeId().equals(employeeId)) {
                // Mismo dueño: actualiza claves in-place (dirty-check -> UPDATE). Sin delete/insert,
                // asi no se viola el indice unico de endpoint al re-suscribir el mismo dispositivo.
                current.updateKeys(p256dh, auth, userAgent);
                return;
            }
            // El endpoint cambia de dueño: borra y FUERZA el flush del DELETE antes del INSERT
            // (si no, Hibernate ordena el INSERT antes del DELETE encolado -> UNIQUE violation).
            subscriptionRepository.delete(current);
            subscriptionRepository.flush();
        }
        subscriptionRepository.save(PushSubscription.of(employeeId, endpoint, p256dh, auth, userAgent));
    }

    /**
     * Baja de una suscripcion del propio usuario (por endpoint). No hace nada si el endpoint no
     * existe o pertenece a otro empleado.
     *
     * @param login    login del usuario autenticado
     * @param endpoint endpoint a dar de baja
     */
    @Transactional
    public void unsubscribe(String login, String endpoint) {
        Long employeeId = employeeIdFor(login);
        subscriptionRepository.findByEndpoint(endpoint)
                .filter(subscription -> subscription.getEmployeeId().equals(employeeId))
                .ifPresent(subscription -> subscriptionRepository.deleteByEndpoint(endpoint));
    }

    /**
     * Borra todas las suscripciones de un empleado (al desactivarlo/eliminarlo).
     *
     * @param employeeId empleado afectado
     */
    @Transactional
    public void deleteAllForEmployee(Long employeeId) {
        subscriptionRepository.deleteByEmployeeId(employeeId);
    }

    private Long employeeIdFor(String login) {
        return employeeRepository.findByLogin(login)
                .map(Employee::getId)
                .orElseThrow(() -> new EntityNotFoundException("Empleado no encontrado: " + login));
    }
}
