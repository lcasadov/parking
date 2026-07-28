import { useTranslation } from 'react-i18next';
import type { DayResourceMap } from '../utils/fixedAssignments';
import { WEEK_LV } from '../utils/fixedAssignments';

interface DayResourceBadgesProps {
  map: DayResourceMap;
  labels: Map<number, string>;
}

// Chips de día que muestran el RECURSO concreto asignado cada día (mockup: un
// empleado puede tener recursos distintos por día, p.ej. D-03 el lunes y D-07 el
// miércoles). El día con recurso se pinta en verde con la etiqueta del recurso; el
// día libre queda en gris con la inicial del día. El nombre del día y el recurso
// (o "sin recurso") van en texto sr-only para lectores de pantalla.
export function DayResourceBadges({ map, labels }: DayResourceBadgesProps) {
  const { t } = useTranslation();
  return (
    <span className="day-badges">
      {WEEK_LV.map((day) => {
        const resourceId = map[day];
        const on = resourceId !== undefined;
        const dayName = t(`common.weekdayName.${day}`);
        const label = on ? (labels.get(resourceId) ?? `#${resourceId}`) : t(`common.weekdayChip.${day}`);
        const srText = on ? `${dayName}: ${label}` : `${dayName}: ${t('common.dayExcluded')}`;
        return (
          <span key={day} className={`day-chip ${on ? 'on has-label' : 'off'}`} title={dayName}>
            <span className="sr-only">{srText}</span>
            <span aria-hidden="true">{label}</span>
          </span>
        );
      })}
    </span>
  );
}
