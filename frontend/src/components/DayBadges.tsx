import { useTranslation } from 'react-i18next';

interface DayBadgesProps {
  days: number[];
}

// Muestra los dias de la semana (1-7) como pills accesibles.
export function DayBadges({ days }: DayBadgesProps) {
  const { t } = useTranslation();
  return (
    <span className="day-badges">
      {days.map((day) => (
        <span key={day} className="pill pill-green">
          {t(`fixedAssignments.weekdays.${day}`)}
        </span>
      ))}
    </span>
  );
}
