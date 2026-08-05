import { useCallback, useEffect, useState } from 'react';
import { getVapidPublicKey, subscribeApi, unsubscribeApi } from '../api/pushApi';
import {
  isIOS,
  isStandalone,
  notificationPermission,
  pushSupported,
  urlBase64ToUint8Array,
} from '../push/pushSupport';

// Estado del canal push desde el punto de vista del dispositivo/usuario.
export type PushState =
  | 'unsupported' // el navegador no soporta Web Push
  | 'ios-not-installed' // iOS sin instalar la PWA (requisito para push)
  | 'denied' // el usuario bloqueó el permiso
  | 'default' // soportado, aún sin suscribir
  | 'subscribed'; // suscrito y activo

// Evento no estándar de instalación (Android/Chromium).
interface BeforeInstallPromptEvent extends Event {
  prompt: () => Promise<void>;
  userChoice: Promise<{ outcome: 'accepted' | 'dismissed' }>;
}

/** Gestiona el permiso y la suscripción push del dispositivo actual. */
export function usePush(): {
  state: PushState;
  busy: boolean;
  enable: () => Promise<void>;
  disable: () => Promise<void>;
} {
  const [state, setState] = useState<PushState>('default');
  const [busy, setBusy] = useState(false);

  const refresh = useCallback(async () => {
    if (!pushSupported()) {
      setState(isIOS() && !isStandalone() ? 'ios-not-installed' : 'unsupported');
      return;
    }
    if (isIOS() && !isStandalone()) {
      setState('ios-not-installed');
      return;
    }
    if (notificationPermission() === 'denied') {
      setState('denied');
      return;
    }
    const registration = await navigator.serviceWorker.ready;
    const subscription = await registration.pushManager.getSubscription();
    setState(subscription ? 'subscribed' : 'default');
  }, []);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  const enable = useCallback(async () => {
    setBusy(true);
    try {
      const permission = await Notification.requestPermission();
      if (permission !== 'granted') {
        await refresh();
        return;
      }
      const key = await getVapidPublicKey();
      if (!key) {
        await refresh();
        return;
      }
      const registration = await navigator.serviceWorker.ready;
      const subscription = await registration.pushManager.subscribe({
        userVisibleOnly: true,
        applicationServerKey: urlBase64ToUint8Array(key),
      });
      await subscribeApi(subscription.toJSON());
      setState('subscribed');
    } finally {
      setBusy(false);
    }
  }, [refresh]);

  const disable = useCallback(async () => {
    setBusy(true);
    try {
      const registration = await navigator.serviceWorker.ready;
      const subscription = await registration.pushManager.getSubscription();
      if (subscription) {
        await unsubscribeApi(subscription.endpoint);
        await subscription.unsubscribe();
      }
      setState('default');
    } finally {
      setBusy(false);
    }
  }, []);

  return { state, busy, enable, disable };
}

/** Captura el evento beforeinstallprompt (Android/Chromium) para instalar la PWA a demanda. */
export function useInstallPrompt(): { canInstall: boolean; install: () => Promise<void> } {
  const [deferred, setDeferred] = useState<BeforeInstallPromptEvent | null>(null);

  useEffect(() => {
    const handler = (event: Event) => {
      event.preventDefault();
      setDeferred(event as BeforeInstallPromptEvent);
    };
    window.addEventListener('beforeinstallprompt', handler);
    const installed = () => setDeferred(null);
    window.addEventListener('appinstalled', installed);
    return () => {
      window.removeEventListener('beforeinstallprompt', handler);
      window.removeEventListener('appinstalled', installed);
    };
  }, []);

  const install = useCallback(async () => {
    if (!deferred) {
      return;
    }
    await deferred.prompt();
    setDeferred(null);
  }, [deferred]);

  return { canInstall: deferred !== null, install };
}
