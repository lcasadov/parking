// Detección de plataforma/estado para el onboarding y la suscripción Web Push
// (change push-notifications, design D14). Sin efectos secundarios.

/** ¿La app corre instalada (PWA en modo standalone)? */
export function isStandalone(): boolean {
  const standaloneNav = (window.navigator as Navigator & { standalone?: boolean }).standalone;
  return window.matchMedia('(display-mode: standalone)').matches || standaloneNav === true;
}

/** ¿El dispositivo es iOS/iPadOS? (Web Push exige instalar la PWA en iOS ≥16.4). */
export function isIOS(): boolean {
  const ua = window.navigator.userAgent;
  return /iphone|ipad|ipod/i.test(ua) || (/Macintosh/.test(ua) && 'ontouchend' in document);
}

/** ¿El navegador soporta Web Push? */
export function pushSupported(): boolean {
  return 'serviceWorker' in navigator && 'PushManager' in window && 'Notification' in window;
}

/** Estado del permiso de notificaciones ('default' | 'granted' | 'denied' | 'unsupported'). */
export function notificationPermission(): NotificationPermission | 'unsupported' {
  return 'Notification' in window ? Notification.permission : 'unsupported';
}

/** Convierte la clave pública VAPID (base64url) al formato Uint8Array que exige subscribe(). */
export function urlBase64ToUint8Array(base64String: string): Uint8Array<ArrayBuffer> {
  const padding = '='.repeat((4 - (base64String.length % 4)) % 4);
  const base64 = (base64String + padding).replace(/-/g, '+').replace(/_/g, '/');
  const raw = window.atob(base64);
  const output = new Uint8Array(new ArrayBuffer(raw.length));
  for (let i = 0; i < raw.length; i += 1) {
    output[i] = raw.charCodeAt(i);
  }
  return output;
}
