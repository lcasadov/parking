import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { SESSION_EXPIRED } from '../api/events';
import { useAuth } from '../auth/useAuth';
import { ROUTES } from '../routes/paths';
import { Button } from './Button';
import { InfoBanner } from './InfoBanner';
import { Modal } from './Modal';

// Suscrita al evento del interceptor 401: muestra el modal (mockup 19) y, al
// cerrar, limpia la sesion y vuelve a /login. Cabecera ambar (no roja).
export function SessionExpiredModal() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { clearUser } = useAuth();
  const [open, setOpen] = useState(false);

  useEffect(() => {
    function handle() {
      setOpen(true);
    }
    window.addEventListener(SESSION_EXPIRED, handle);
    return () => window.removeEventListener(SESSION_EXPIRED, handle);
  }, []);

  if (!open) {
    return null;
  }

  function handleClose() {
    setOpen(false);
    clearUser();
    navigate(ROUTES.login, { replace: true });
  }

  return (
    <Modal
      title={t('auth.sessionExpiredTitle')}
      variant="amber"
      icon="clock-exclamation"
      narrow
      onClose={handleClose}
      footer={
        <div className="footer-center">
          <Button variant="green" icon="login-2" onClick={handleClose}>
            {t('auth.backToLogin')}
          </Button>
        </div>
      }
    >
      <div className="session-expired-body">
        <i className="ti ti-lock-access session-expired-icon" aria-hidden="true" />
        <p className="session-expired-heading">{t('auth.sessionExpiredHeading')}</p>
        <p className="muted session-expired-detail">{t('auth.sessionExpiredDetail')}</p>
        <InfoBanner variant="blue" icon="info-circle">
          {t('auth.sessionExpiredPhaseNote')}
        </InfoBanner>
      </div>
    </Modal>
  );
}
