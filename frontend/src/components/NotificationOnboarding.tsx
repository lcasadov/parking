import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from './Button';
import { usePush, useInstallPrompt } from '../hooks/usePush';

const DISMISS_KEY = 'push-onboarding-dismissed';

function isDismissed(): boolean {
  try {
    return window.localStorage.getItem(DISMISS_KEY) === '1';
  } catch {
    return false;
  }
}

// Tarjetas de onboarding de notificaciones en la página principal (change push-notifications,
// design D14). SIN auto-prompt: el permiso se pide solo al pulsar el botón. Muestra la tarjeta
// que corresponda según plataforma/estado; descartable y sin bloquear el uso de la app.
export function NotificationOnboarding() {
  const { t } = useTranslation();
  const { state, busy, enable } = usePush();
  const { canInstall, install } = useInstallPrompt();
  const [dismissed, setDismissed] = useState(isDismissed());

  function dismiss(): void {
    setDismissed(true);
    try {
      window.localStorage.setItem(DISMISS_KEY, '1');
    } catch {
      // localStorage puede no estar disponible; el descarte queda solo en memoria.
    }
  }

  // Ya activo o no soportado / descartado: no se muestra nada.
  if (dismissed || state === 'subscribed' || state === 'unsupported') {
    return null;
  }

  // Android/Chromium instalable: tarjeta de instalar con botón que dispara el prompt nativo.
  if (canInstall) {
    return (
      <OnboardingCard
        icon="download"
        title={t('notifications.onboarding.installTitle')}
        body={t('notifications.onboarding.installBody')}
        onDismiss={dismiss}
        action={
          <Button variant="green" icon="download" onClick={() => void install()}>
            {t('notifications.onboarding.installAction')}
          </Button>
        }
      />
    );
  }

  // iOS sin instalar: instrucciones para añadir a pantalla de inicio (no hay prompt en iOS).
  if (state === 'ios-not-installed') {
    return (
      <OnboardingCard
        icon="device-mobile"
        title={t('notifications.onboarding.iosTitle')}
        body={t('notifications.onboarding.iosBody')}
        onDismiss={dismiss}
      />
    );
  }

  // Permiso denegado: guía para reactivarlo en los ajustes del navegador (sin reintentar prompt).
  if (state === 'denied') {
    return (
      <OnboardingCard
        icon="bell-off"
        title={t('notifications.onboarding.deniedTitle')}
        body={t('notifications.onboarding.deniedBody')}
        onDismiss={dismiss}
      />
    );
  }

  // Soportado y sin suscribir: tarjeta de activar con botón que dispara el permiso nativo.
  return (
    <OnboardingCard
      icon="bell"
      title={t('notifications.onboarding.activateTitle')}
      body={t('notifications.onboarding.activateBody')}
      onDismiss={dismiss}
      action={
        <Button variant="green" icon="bell" loading={busy} onClick={() => void enable()}>
          {t('notifications.onboarding.activateAction')}
        </Button>
      }
    />
  );
}

function OnboardingCard({
  icon,
  title,
  body,
  action,
  onDismiss,
}: {
  icon: string;
  title: string;
  body: string;
  action?: React.ReactNode;
  onDismiss: () => void;
}) {
  const { t } = useTranslation();
  return (
    <section className="push-onboarding" aria-label={title}>
      <span className="push-onboarding-icon" aria-hidden="true">
        <i className={`ti ti-${icon}`} />
      </span>
      <div className="push-onboarding-text">
        <h3 className="push-onboarding-title">{title}</h3>
        <p className="push-onboarding-body">{body}</p>
      </div>
      <div className="push-onboarding-actions">
        {action}
        <button
          type="button"
          className="push-onboarding-dismiss"
          aria-label={t('common.close')}
          onClick={onDismiss}
        >
          <i className="ti ti-x" aria-hidden="true" />
        </button>
      </div>
    </section>
  );
}
