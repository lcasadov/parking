import { useTranslation } from 'react-i18next';
import { Button } from './Button';
import { addDaysIso, longDate } from '../utils/calendar';
import { REQUEST_WINDOW_DAYS, maxRequestDateIso, todayIso } from '../utils/requests';

interface FloorPlanDatebarProps {
  date: string;
  onChange: (date: string) => void;
}

// Barra de fecha del plano: día anterior / "Hoy" / día siguiente, fecha larga
// localizada y recordatorio de la ventana de reserva (14 días). La navegación
// se restringe a la ventana hoy..hoy+14 (init-floor-plan).
export function FloorPlanDatebar({ date, onChange }: FloorPlanDatebarProps) {
  const { t, i18n } = useTranslation();
  const min = todayIso();
  const max = maxRequestDateIso();
  const prev = addDaysIso(date, -1);
  const next = addDaysIso(date, 1);
  const canPrev = prev >= min;
  const canNext = next <= max;
  const isToday = date === min;

  return (
    <div className="plano-datebar" role="group" aria-label={t('floorPlan.datebar.label')}>
      <div className="plano-datebar-nav">
        <Button
          variant="white"
          icon="chevron-left"
          aria-label={t('floorPlan.datebar.previous')}
          disabled={!canPrev}
          onClick={() => onChange(prev)}
        />
        <Button variant="white" disabled={isToday} onClick={() => onChange(min)}>
          {t('floorPlan.datebar.today')}
        </Button>
        <Button
          variant="white"
          icon="chevron-right"
          aria-label={t('floorPlan.datebar.next')}
          disabled={!canNext}
          onClick={() => onChange(next)}
        />
      </div>
      <p className="plano-datebar-date" aria-live="polite">
        {longDate(date, i18n.language)}
      </p>
      <p className="plano-datebar-window">
        {t('floorPlan.datebar.window', { days: REQUEST_WINDOW_DAYS })}
      </p>
    </div>
  );
}
