import { useTranslation } from 'react-i18next';
import { Button } from './Button';
import { addDaysIso, longDate } from '../utils/calendar';
import { todayIso } from '../utils/requests';

interface FloorPlanDatebarProps {
  date: string;
  onChange: (date: string) => void;
}

// Barra de fecha del plano: día anterior / "Hoy" / día siguiente, fecha larga
// localizada y recordatorio de reserva. La navegación permite cualquier fecha
// futura (hoy en adelante); solo se acota el mínimo a hoy (no fechas pasadas).
export function FloorPlanDatebar({ date, onChange }: FloorPlanDatebarProps) {
  const { t, i18n } = useTranslation();
  const min = todayIso();
  const prev = addDaysIso(date, -1);
  const next = addDaysIso(date, 1);
  const canPrev = prev >= min;
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
          onClick={() => onChange(next)}
        />
      </div>
      <p className="plano-datebar-date" aria-live="polite">
        {longDate(date, i18n.language)}
      </p>
    </div>
  );
}
