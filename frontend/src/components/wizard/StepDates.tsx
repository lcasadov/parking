import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { MiniCalendar } from './MiniCalendar';
import { firstOfMonth, toggleScatterDate } from '../../utils/wizardDates';
import { longDate } from '../../utils/calendar';
import { todayIso } from '../../utils/requests';
import type { DateMode, WizardState } from './wizardTypes';

interface StepDatesProps {
  state: WizardState;
  dates: string[];
  patch: (partial: Partial<WizardState>) => void;
}

const MODES: DateMode[] = ['SINGLE', 'RANGE', 'SCATTER'];
const MODE_ICON: Record<DateMode, string> = {
  SINGLE: 'calendar-event',
  RANGE: 'calendar-week',
  SCATTER: 'calendar-plus',
};

// Aplica el clic de un día según el modo activo, devolviendo el patch de estado.
function pickForMode(state: WizardState, iso: string): Partial<WizardState> {
  if (state.dateMode === 'SINGLE') {
    return { singleDate: iso };
  }
  if (state.dateMode === 'SCATTER') {
    return { scatterDates: toggleScatterDate(state.scatterDates, iso) };
  }
  // RANGE: primer clic (o reinicio) fija el inicio; el segundo, el fin (>= inicio).
  const startingOver = state.rangeStart === '' || state.rangeEnd !== '';
  if (startingOver) {
    return { rangeStart: iso, rangeEnd: '' };
  }
  return iso < state.rangeStart ? { rangeStart: iso, rangeEnd: '' } : { rangeEnd: iso };
}

// Paso 2 — Fechas: tres modos conmutables (día concreto, rango, días sueltos) sobre
// el mismo calendario mensual; las fechas elegidas se muestran como chips retirables.
export function StepDates({ state, dates, patch }: StepDatesProps) {
  const { t, i18n } = useTranslation();
  const [anchor, setAnchor] = useState<string>(() => firstOfMonth(todayIso()));

  function switchMode(mode: DateMode): void {
    patch({ dateMode: mode });
  }

  function pick(iso: string): void {
    patch(pickForMode(state, iso));
  }

  function removeChip(iso: string): void {
    if (state.dateMode === 'SCATTER') {
      patch({ scatterDates: state.scatterDates.filter((date) => date !== iso) });
    } else if (state.dateMode === 'SINGLE') {
      patch({ singleDate: '' });
    } else {
      patch({ rangeStart: '', rangeEnd: '' });
    }
  }

  return (
    <div className="rzw-step-body">
      <div className="rzw-segmented" role="group" aria-label={t('wizard.dates.modeLabel')}>
        {MODES.map((mode) => (
          <button
            key={mode}
            type="button"
            className={state.dateMode === mode ? 'is-active' : ''}
            aria-pressed={state.dateMode === mode}
            onClick={() => switchMode(mode)}
          >
            <i className={`ti ti-${MODE_ICON[mode]}`} aria-hidden="true" />
            {t(`wizard.dates.mode.${mode}`)}
          </button>
        ))}
      </div>
      <p className="rzw-lead">{t(`wizard.dates.hint.${state.dateMode}`)}</p>

      <div className="rzw-dates-layout">
        <MiniCalendar anchor={anchor} onAnchorChange={setAnchor} state={state} onPick={pick} />

        <div className="rzw-chips-panel" aria-live="polite">
          <span className="rzw-eyebrow">
            {t('wizard.dates.selectedCount', { count: dates.length })}
          </span>
          {dates.length === 0 ? (
            <p className="rzw-empty">{t('wizard.dates.none')}</p>
          ) : (
            <ul className="rzw-chips">
              {dates.map((iso) => (
                <li key={iso}>
                  <span className="rzw-chip">
                    <i className="ti ti-calendar" aria-hidden="true" />
                    <span className="mono">{longDate(iso, i18n.language)}</span>
                    <button
                      type="button"
                      className="rzw-chip-x"
                      aria-label={t('wizard.dates.remove', { date: longDate(iso, i18n.language) })}
                      onClick={() => removeChip(iso)}
                    >
                      <i className="ti ti-x" aria-hidden="true" />
                    </button>
                  </span>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </div>
  );
}
