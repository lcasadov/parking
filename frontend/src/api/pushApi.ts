import { apiClient } from './apiClient';
import type { SystemSettings } from '../types/settings';

// Endpoints Web Push (change push-notifications).
const VAPID_KEY = '/push/vapid-public-key';
const SUBSCRIPTIONS = '/push/subscriptions';
const ADMIN_NOTIFICATION_CHANNELS = '/admin/settings/notification-channels';

// GET /push/vapid-public-key: clave pública VAPID para suscribir el navegador.
export async function getVapidPublicKey(): Promise<string> {
  const { data } = await apiClient.get<{ publicKey: string }>(VAPID_KEY);
  return data.publicKey;
}

// POST /push/subscriptions: alta idempotente de la suscripción del navegador (endpoint + claves).
export async function subscribeApi(subscription: PushSubscriptionJSON): Promise<void> {
  await apiClient.post(SUBSCRIPTIONS, {
    endpoint: subscription.endpoint,
    keys: {
      p256dh: subscription.keys?.p256dh,
      auth: subscription.keys?.auth,
    },
  });
}

// DELETE /push/subscriptions: baja de la suscripción del propio usuario por endpoint.
export async function unsubscribeApi(endpoint: string): Promise<void> {
  await apiClient.delete(SUBSCRIPTIONS, { data: { endpoint } });
}

// PUT /admin/settings/notification-channels (ADMIN): interruptores globales email/push.
export async function updateNotificationChannels(
  emailNotificationsEnabled: boolean,
  pushNotificationsEnabled: boolean,
): Promise<SystemSettings> {
  const { data } = await apiClient.put<SystemSettings>(ADMIN_NOTIFICATION_CHANNELS, {
    emailNotificationsEnabled,
    pushNotificationsEnabled,
  });
  return data;
}
