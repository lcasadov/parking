import type { TFunction } from 'i18next';
import { useTranslation } from 'react-i18next';
import { Button } from './Button';
import { ResourceIcon } from './ResourceIcon';
import { useResourceAvailabilityQuery } from '../hooks/useCalendar';
import { useSuggestedResourceQuery } from '../hooks/useRequests';
import { isValidIsoDate } from '../utils/calendar';
import { isTodayOrFuture, todayIso } from '../utils/requests';
import type { ResourceType } from '../types/request';

interface ResourceChoiceCardProps {
  resourceType: ResourceType;
  title: string;
  date: string;
  // Selección del formulario (se muestra como seleccionada solo si NO está tomada).
  selected: boolean;
  // El empleado YA tiene ese recurso reservado ese día: no se puede volver a pedir.
  taken: boolean;
  // Hay VARIOS días seleccionados: la disponibilidad y la sugerencia de recurso
  // son por-día, así que no tienen sentido y se ocultan (solo se muestran para un
  // único día concreto).
  multiDay: boolean;
  waitlistJoined: boolean;
  onToggle: () => void;
  onJoinWaitlist: (resourceType: ResourceType) => void;
}

interface StatusView {
  icon: string;
  text: string;
  tone: 'reserved' | 'ok' | 'none' | 'muted';
}

// Descriptor de la línea de estado de la tarjeta (null = no mostrar línea).
// Con varios días seleccionados no se muestra disponibilidad (es por-día).
// Extraído a módulo para mantener baja la complejidad cognitiva (S3776).
function statusView(
  t: TFunction,
  resourceType: ResourceType,
  taken: boolean,
  validDate: boolean,
  loading: boolean,
  count: number,
  multiDay: boolean,
  when: string,
): StatusView | null {
  if (taken) {
    const key = resourceType === 'PARKING' ? 'reservedParking' : 'reservedDesk';
    return { icon: 'circle-check', text: t(`requests.create.card.${key}`), tone: 'reserved' };
  }
  if (!validDate) {
    return { icon: 'calendar', text: t('requests.create.card.needDate'), tone: 'muted' };
  }
  if (multiDay) {
    return null; // varios días: la disponibilidad por-día no aplica
  }
  if (loading) {
    return { icon: 'loader-2', text: t('requests.create.card.checking'), tone: 'muted' };
  }
  if (count > 0) {
    return { icon: 'circle-check', text: t('requests.create.card.available', { count, when }), tone: 'ok' };
  }
  return { icon: 'alert-triangle', text: t('requests.create.card.none'), tone: 'none' };
}

// Icono del indicador de selección (esquina): candado si está tomada, check si
// seleccionada, círculo vacío si disponible sin seleccionar.
function markIcon(taken: boolean, active: boolean): string {
  if (taken) return 'lock';
  return active ? 'circle-check' : 'circle';
}

// Referencia del día para "N libres {cuándo}": "hoy" si es hoy, si no "el jueves".
// Extraído a módulo para mantener baja la complejidad del componente (S3776).
function dayReference(date: string, validDate: boolean, locale: string, t: TFunction): string {
  if (!validDate) {
    return ''; // fecha vacía/inválida: statusView no usa este valor (corta antes)
  }
  if (date === todayIso()) {
    return t('requests.create.card.today');
  }
  const day = new Intl.DateTimeFormat(locale, { weekday: 'long' }).format(
    new Date(`${date}T00:00:00`),
  );
  return t('requests.create.card.onDay', { day });
}

// Clases de la tarjeta según tono/selección/bloqueo. Extraído para no cargar la
// complejidad cognitiva del componente (S3776).
function cardClassName(tone: StatusView['tone'] | undefined, active: boolean, taken: boolean): string {
  const parts = ['rc-card', tone ? `is-${tone}` : 'is-plain'];
  if (active) parts.push('is-selected');
  if (taken) parts.push('is-locked');
  return parts.join(' ');
}

// Tarjeta seleccionable de recurso (plaza / puesto) para la reserva rápida.
// Consulta su propia disponibilidad; si el recurso ya está reservado ese día se
// muestra bloqueada (no seleccionable). Cuando no hay disponibilidad ofrece
// apuntarse a la lista de espera (capability request-waitlist).
export function ResourceChoiceCard({
  resourceType,
  title,
  date,
  selected,
  taken,
  multiDay,
  waitlistJoined,
  onToggle,
  onJoinWaitlist,
}: ResourceChoiceCardProps) {
  const { t, i18n } = useTranslation();
  const availability = useResourceAvailabilityQuery(date, resourceType);
  const validDate = isValidIsoDate(date) && isTodayOrFuture(date);
  const count = availability.data?.availableResources.length ?? 0;
  const active = selected && !taken;

  // Preview de auto-asignación: qué recurso concreto se te asignará (incluye tu
  // fijo si sigue libre). Solo con UN día concreto seleccionado y tarjeta activa.
  const showSingleDayInfo = active && !multiDay;
  const suggestion = useSuggestedResourceQuery(date, resourceType, showSingleDayInfo);
  const suggestedLabel =
    showSingleDayInfo && suggestion.data?.available ? suggestion.data.resourceLabel : null;

  const when = dayReference(date, validDate, i18n.language, t);

  const status = statusView(
    t,
    resourceType,
    taken,
    validDate,
    availability.isLoading,
    count,
    multiDay,
    when,
  );
  const showWaitlist =
    showSingleDayInfo && validDate && !availability.isLoading && count === 0;

  const cardClass = cardClassName(status?.tone, active, taken);

  return (
    <div className={cardClass}>
      <button
        type="button"
        className="rc-card-main"
        onClick={onToggle}
        disabled={taken}
        aria-pressed={active}
      >
        <span className="rc-card-icon" aria-hidden="true">
          <ResourceIcon type={resourceType} />
        </span>
        <span className="rc-card-text">
          <span className="rc-card-title">{title}</span>
          {status ? (
            <span className={`rc-card-status tone-${status.tone}`}>
              <i className={`ti ti-${status.icon}`} aria-hidden="true" />
              {status.text}
            </span>
          ) : null}
          {suggestedLabel ? (
            <span className="rc-card-suggestion">
              <i className="ti ti-arrow-right" aria-hidden="true" />
              {t('requests.create.willAssign', { resource: suggestedLabel })}
            </span>
          ) : null}
        </span>
        <span className="rc-card-mark" aria-hidden="true">
          <i className={`ti ti-${markIcon(taken, active)}`} />
        </span>
      </button>
      {showWaitlist ? (
        <div className="rc-card-waitlist">
          {waitlistJoined ? (
            <span className="waitlist-offer-joined">
              <i className="ti ti-check" aria-hidden="true" />
              {t('requests.create.waitlist.joined')}
            </span>
          ) : (
            <Button variant="white" icon="clock" onClick={() => onJoinWaitlist(resourceType)}>
              {t('requests.create.waitlist.join')}
            </Button>
          )}
        </div>
      ) : null}
    </div>
  );
}
