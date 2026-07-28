/* Service Worker de parking ALEATICA — Web Push (change push-notifications).
   Maneja la recepción de notificaciones (push) y el clic (notificationclick).
   Se registra desde main.tsx; sin dependencias de build (SW plano). */

// Recibe el push: muestra la notificación del sistema con título/cuerpo/url del payload.
self.addEventListener('push', (event) => {
  let payload = {};
  try {
    payload = event.data ? event.data.json() : {};
  } catch {
    payload = { title: 'Parking ALEATICA', body: event.data ? event.data.text() : '' };
  }
  const title = payload.title || 'Parking ALEATICA';
  const options = {
    body: payload.body || '',
    icon: '/logo192.png',
    badge: '/favicon-32x32.png',
    data: { url: payload.url || '/' },
    tag: payload.tag || undefined,
  };
  event.waitUntil(self.registration.showNotification(title, options));
});

// Al pulsar la notificación: enfoca una pestaña existente de la app o abre una nueva,
// navegando a la ruta del deep-link (data.url).
self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  const targetUrl = (event.notification.data && event.notification.data.url) || '/';
  event.waitUntil(
    self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then((clientList) => {
      for (const client of clientList) {
        if ('focus' in client) {
          client.postMessage({ type: 'push-navigate', url: targetUrl });
          return client.focus();
        }
      }
      if (self.clients.openWindow) {
        return self.clients.openWindow(targetUrl);
      }
      return undefined;
    }),
  );
});

// Activarse cuanto antes (no esperar a que se cierren pestañas viejas).
self.addEventListener('install', () => self.skipWaiting());
self.addEventListener('activate', (event) => event.waitUntil(self.clients.claim()));
