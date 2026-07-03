import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { SESSION_EXPIRED } from '../api/events';
import { useAuth } from '../auth/useAuth';
import { ROUTES } from '../routes/paths';
import { Button } from './Button';
import { Modal } from './Modal';

// Suscrita al evento del interceptor 401: muestra el modal y, al cerrar,
// limpia la sesion y vuelve a /login.
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
      variant="red"
      narrow
      onClose={handleClose}
      footer={
        <Button variant="red" onClick={handleClose}>
          {t('auth.backToLogin')}
        </Button>
      }
    >
      <p>{t('auth.sessionExpiredBody')}</p>
    </Modal>
  );
}
