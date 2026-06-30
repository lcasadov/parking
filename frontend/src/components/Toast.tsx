import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { API_ERROR_TOAST, type ApiErrorToastDetail } from '../api/events';

const AUTO_DISMISS_MS = 5000;

// Toast suscrito a los eventos 403/5xx del interceptor Axios.
export function Toast() {
  const { t } = useTranslation();
  const [messageKey, setMessageKey] = useState<string | null>(null);

  useEffect(() => {
    function handle(event: Event) {
      const detail = (event as CustomEvent<ApiErrorToastDetail>).detail;
      setMessageKey(detail.message);
    }
    window.addEventListener(API_ERROR_TOAST, handle);
    return () => window.removeEventListener(API_ERROR_TOAST, handle);
  }, []);

  useEffect(() => {
    if (messageKey === null) {
      return;
    }
    const timer = window.setTimeout(() => setMessageKey(null), AUTO_DISMISS_MS);
    return () => window.clearTimeout(timer);
  }, [messageKey]);

  if (messageKey === null) {
    return null;
  }

  return (
    <div className="toast" role="alert">
      {t(messageKey)}
    </div>
  );
}
