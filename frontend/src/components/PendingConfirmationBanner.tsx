import { useTranslation } from 'react-i18next';
import { Button } from './Button';
import { getApiError, getStatus } from '../api/apiError';
import { emitApiErrorToast } from '../api/events';
import { useResendRequest } from '../hooks/useRequests';
import { useToast } from '../hooks/useToast';
import { canResendRequest, resendCooldownRemainingMs } from '../utils/requests';

interface PendingConfirmationBannerProps {
  requestId: number;
  createdAt: string;
  lastRemindedAt?: string | null;
  onResent?: () => void;
}

const HTTP_FORBIDDEN = 403;
const HTTP_CONFLICT = 409;
const RESEND_TOO_SOON = 'RESEND_TOO_SOON';
const MS_PER_HOUR = 60 * 60 * 1000;

// Traduce el error de POST /requests/{id}/resend a la clave i18n del toast:
// 403 (no es tuya), 409 REQUEST_NOT_PENDING (ya no esta pendiente) o
// 409 RESEND_TOO_SOON (aun dentro de la ventana de 24h, condicion de carrera con
// otra pestaña/reenvio previo).
function toastKeyForResendError(error: unknown): string {
  const status = getStatus(error);
  if (status === HTTP_FORBIDDEN) {
    return 'requests.errors.resendForbidden';
  }
  if (status === HTTP_CONFLICT) {
    return getApiError(error)?.error === RESEND_TOO_SOON
      ? 'requests.errors.resendTooSoon'
      : 'requests.errors.alreadyResolved';
  }
  return 'requests.errors.generic';
}

// Banner AMBAR reutilizable (design-system info-banner) para una solicitud
// PENDING: recuerda que la ubicacion final podria cambiar y, cuando ya han
// pasado 24h desde la creacion (o el ultimo reenvio), ofrece reenviar el aviso
// a los admins (POST /requests/{id}/resend). Se usa en MyRequestsPage y en
// MyWeekPage (Ola de rediseno de "Mi Semana").
export function PendingConfirmationBanner({
  requestId,
  createdAt,
  lastRemindedAt,
  onResent,
}: PendingConfirmationBannerProps) {
  const { t } = useTranslation();
  const toast = useToast();
  const resendMutation = useResendRequest();

  const eligible = canResendRequest(createdAt, lastRemindedAt);
  const remainingHours = eligible
    ? 0
    : Math.ceil(resendCooldownRemainingMs(createdAt, lastRemindedAt) / MS_PER_HOUR);

  function handleResend(): void {
    resendMutation.mutate(requestId, {
      onSuccess: () => {
        toast.success('requests.pendingBanner.resent');
        onResent?.();
      },
      onError: (mutationError) => emitApiErrorToast(toastKeyForResendError(mutationError)),
    });
  }

  return (
    <div
      className="info-banner amber pending-confirmation-banner"
      role="status"
      aria-live="polite"
    >
      <span className="pending-confirmation-banner-text">
        <i className="ti ti-clock" aria-hidden="true" />
        {t('requests.pendingBanner.message')}
      </span>
      {eligible ? (
        <Button
          variant="white"
          icon="send"
          disabled={resendMutation.isPending}
          onClick={handleResend}
        >
          {t('requests.pendingBanner.resend')}
        </Button>
      ) : (
        <span className="pending-confirmation-banner-hint">
          {t('requests.pendingBanner.remaining', { hours: remainingHours })}
        </span>
      )}
    </div>
  );
}
