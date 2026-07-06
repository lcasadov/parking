import { useTranslation } from 'react-i18next';
import { useResourceAvailabilityQuery } from '../hooks/useCalendar';
import { isValidIsoDate } from '../utils/calendar';
import { isWithinWindow } from '../utils/requests';
import type { ResourceType } from '../types/request';

interface ResourceAvailabilityBannerProps {
  date: string;
  resourceType: ResourceType;
}

// Banner INFORMATIVO (no bloqueante) de disponibilidad de un recurso para la
// fecha elegida en la solicitud unificada. Consulta GET /availability?date&
// resourceType; la validación final la hace el backend al enviar.
export function ResourceAvailabilityBanner({ date, resourceType }: ResourceAvailabilityBannerProps) {
  const { t } = useTranslation();
  const { data, isLoading, isError } = useResourceAvailabilityQuery(date, resourceType);

  if (!isValidIsoDate(date) || !isWithinWindow(date)) {
    return null;
  }
  if (isLoading) {
    return (
      <p className="info-banner blue availability-banner" role="status">
        {t('requests.create.availability.loading')}
      </p>
    );
  }
  if (isError) {
    return null;
  }

  const count = data?.availableResources.length ?? 0;
  const available = count > 0;
  // Sin disponibilidad → tono rojo de alerta; con disponibilidad → verde de OK.
  const tone = available ? 'green' : 'red';
  const icon = available ? 'circle-check' : 'alert-triangle';
  return (
    <p className={`info-banner ${tone} availability-banner`} role="status" aria-live="polite">
      <i className={`ti ti-${icon}`} aria-hidden="true" />
      {t('requests.create.availability.count', { count })}
    </p>
  );
}
