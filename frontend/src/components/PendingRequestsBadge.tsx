import { useTranslation } from 'react-i18next';
import { usePendingRequestsQuery } from '../hooks/useRequests';

// Badge rojo con el conteo de solicitudes PENDING (mockup .badge-red en "Solicitudes").
// Query ligera (size 1): solo interesa totalElements. Oculto si no hay pendientes.
export function PendingRequestsBadge() {
  const { t } = useTranslation();
  const query = usePendingRequestsQuery({ page: 0, size: 1 });
  const count = query.data?.totalElements ?? 0;
  if (count <= 0) {
    return null;
  }
  return (
    <span className="badge-red" aria-label={t('requests.inbox.pendingBadge', { count })}>
      {count}
    </span>
  );
}
