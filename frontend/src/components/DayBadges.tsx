import { useTranslation } from 'react-i18next';

interface DayBadgesProps {
  days: number[];
}

const WEEK_LV = [1, 2, 3, 4, 5];

// Muestra los dias de la semana (L-V) como chips (mockup .day-chip): verde
// solido si el dia esta en `days` (.on), gris si no (.off). El chip visible es
// una sola letra; el nombre completo y el estado van en texto sr-only.
export function DayBadges({ days }: DayBadgesProps) {
  const { t } = useTranslation();
  return (
    <span className="day-badges">
      {WEEK_LV.map((day) => {
        const on = days.includes(day);
        const state = on ? t('common.dayIncluded') : t('common.dayExcluded');
        return (
          <span
            key={day}
            className={`day-chip ${on ? 'on' : 'off'}`}
            title={t(`common.weekdayName.${day}`)}
          >
            <span className="sr-only">{`${t(`common.weekdayName.${day}`)}: ${state}`}</span>
            <span aria-hidden="true">{t(`common.weekdayChip.${day}`)}</span>
          </span>
        );
      })}
    </span>
  );
}
