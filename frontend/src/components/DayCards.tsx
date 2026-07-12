import { useTranslation } from 'react-i18next';

interface DayCardsProps {
  // Dias seleccionados (ISO 1-7).
  value: number[];
  onChange: (next: number[]) => void;
  // Dias disponibles a mostrar; por defecto lunes-viernes.
  days?: number[];
}

const WEEK_LV = [1, 2, 3, 4, 5];

// Selector de dias en tarjetas (mockup .day-cards). Cada tarjeta es un boton
// con aria-pressed que alterna el dia dentro de value.
export function DayCards({ value, onChange, days = WEEK_LV }: DayCardsProps) {
  const { t } = useTranslation();

  function toggle(day: number): void {
    const next = value.includes(day)
      ? value.filter((d) => d !== day)
      : [...value, day].sort((a, b) => a - b);
    onChange(next);
  }

  return (
    <div className="day-cards">
      {days.map((day) => {
        const selected = value.includes(day);
        return (
          <button
            key={day}
            type="button"
            aria-pressed={selected}
            className={`day-card${selected ? ' selected' : ''}`}
            onClick={() => toggle(day)}
          >
            <div className="day-abbr">{t(`common.weekdayAbbr.${day}`)}</div>
            <div className="day-name">{t(`common.weekdayName.${day}`)}</div>
            <div className="day-check">
              {selected ? <i className="ti ti-check" aria-hidden="true" /> : null}
            </div>
          </button>
        );
      })}
    </div>
  );
}
