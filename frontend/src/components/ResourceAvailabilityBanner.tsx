import { useTranslation } from 'react-i18next';
import { Button } from './Button';
import { useResourceAvailabilityQuery } from '../hooks/useCalendar';
import { isValidIsoDate } from '../utils/calendar';
import { isTodayOrFuture } from '../utils/requests';
import type { ResourceType } from '../types/request';

interface WaitlistJoinActionProps {
  joined: boolean;
  onJoin: () => void;
}

// Boton "Apuntarme a la lista de espera" o, una vez pulsado, la confirmacion
// correspondiente (sin posicion numerica, fuera de alcance de la capability).
function WaitlistJoinAction({ joined, onJoin }: WaitlistJoinActionProps) {
  const { t } = useTranslation();
  if (joined) {
    return (
      <span className="waitlist-offer-joined">
        <i className="ti ti-check" aria-hidden="true" />
        {t('requests.create.waitlist.joined')}
      </span>
    );
  }
  return (
    <Button variant="white" icon="clock" onClick={onJoin}>
      {t('requests.create.waitlist.join')}
    </Button>
  );
}

interface ResourceAvailabilityBannerProps {
  date: string;
  resourceType: ResourceType;
  // El recurso esta marcado en el formulario: sin seleccionar solo se muestra el
  // aviso informativo (sin CTA), para no ofrecer apuntarse a un recurso que no
  // forma parte de la solicitud.
  selected?: boolean;
  // Ya apuntado a la lista de espera para este recurso en la sesion del modal
  // (capability request-waitlist). Cuando es true sustituye el botón por una
  // confirmación.
  waitlistJoined?: boolean;
  onJoinWaitlist?: (resourceType: ResourceType) => void;
}

// Banner INFORMATIVO (no bloqueante) de disponibilidad de un recurso para la
// fecha elegida en la solicitud unificada. Consulta GET /availability?date&
// resourceType; la validación final la hace el backend al enviar. Cuando la
// disponibilidad es 0, comunica con honestidad que los recursos se liberan a
// menudo y ofrece apuntarse a la lista de espera (employee-portal spec §1):
// NUNCA bloquea el envío de la solicitud.
export function ResourceAvailabilityBanner({
  date,
  resourceType,
  selected,
  waitlistJoined,
  onJoinWaitlist,
}: ResourceAvailabilityBannerProps) {
  const { t } = useTranslation();
  const { data, isLoading, isError } = useResourceAvailabilityQuery(date, resourceType);

  if (!isValidIsoDate(date) || !isTodayOrFuture(date)) {
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
  if (count > 0) {
    return (
      <p className="info-banner green availability-banner" role="status" aria-live="polite">
        <i className="ti ti-circle-check" aria-hidden="true" />
        {t('requests.create.availability.count', { count })}
      </p>
    );
  }

  const resourceNoun = t(`requests.create.waitlist.resourceNoun.${resourceType}`);
  return (
    <div
      className="info-banner amber availability-banner pending-confirmation-banner waitlist-offer"
      role="status"
      aria-live="polite"
    >
      <span className="pending-confirmation-banner-text">
        <i className="ti ti-alert-triangle" aria-hidden="true" />
        {t('requests.create.waitlist.noneHint', { resource: resourceNoun })}
      </span>
      {selected && onJoinWaitlist ? (
        <WaitlistJoinAction
          joined={Boolean(waitlistJoined)}
          onJoin={() => onJoinWaitlist(resourceType)}
        />
      ) : null}
    </div>
  );
}
