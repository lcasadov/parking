// Registro del Service Worker de Web Push (change push-notifications). Se llama una vez al
// arrancar la app. El SW se sirve en /sw.js. En navegadores sin soporte no hace nada.

export function registerServiceWorker(): void {
  if (!('serviceWorker' in navigator)) {
    return;
  }
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('/sw.js').catch(() => {
      // Registro best-effort: si falla, la app sigue funcionando sin push.
    });
  });

  // El SW pide navegar al pulsar una notificación (deep-link). Al llegar el mensaje,
  // navegamos a la ruta indicada si no estamos ya en ella.
  navigator.serviceWorker.addEventListener('message', (event) => {
    const data = event.data as { type?: string; url?: string } | undefined;
    if (data?.type === 'push-navigate' && data.url && window.location.pathname !== data.url) {
      window.location.assign(data.url);
    }
  });
}
