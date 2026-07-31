import { useTranslation } from 'react-i18next';
import { usePendingVehicleCountQuery } from '../hooks/useEmployeeVehicleReview';

// Badge rojo con el número de vehículos pendientes de acción (PENDING + PENDING_DELETION), en la
// navegación admin (change employee-vehicle-self-service, Fase 2). Oculto si no hay ninguno.
export function VehicleReviewBadge() {
  const { t } = useTranslation();
  const query = usePendingVehicleCountQuery();
  const count = query.data ?? 0;
  if (count <= 0) {
    return null;
  }
  return (
    <span className="badge-red" aria-label={t('vehicles.review.pendingBadge', { count })}>
      {count}
    </span>
  );
}
