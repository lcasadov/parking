import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { LocationDesk } from './LocationDesk';
import { LocationParking } from './LocationParking';
import { longDate } from '../../utils/calendar';
import { PARKING_AUTO, RESOURCE_DESK } from './wizardTypes';
import type { DayChoice, ParkingChoice, WizardState } from './wizardTypes';

interface StepLocationPerDayProps {
  state: WizardState;
  dates: string[];
  patch: (partial: Partial<WizardState>) => void;
}

// Modo "Distinta por día": una fila por fecha con su elección, y debajo el editor
// (plano o rejilla de plazas) de la fecha ACTIVA, filtrado por la disponibilidad de
// ESE día. Atajos: aplicar la elección activa a todos los días y (plaza) auto en
// los días aún sin asignar.
export function StepLocationPerDay({ state, dates, patch }: StepLocationPerDayProps) {
  const { t, i18n } = useTranslation();
  const isDesk = state.resourceType === RESOURCE_DESK;
  const [rawActive, setActiveDate] = useState<string>(
    () => dates.find((date) => !state.perDay[date]) ?? dates[0],
  );
  // Si el usuario cambió las fechas atrás, la activa podría ya no existir: cae a la
  // primera fecha vigente para no editar un día fuera de la selección.
  const activeDate = dates.includes(rawActive) ? rawActive : dates[0];

  function setDayChoice(date: string, choice: DayChoice): void {
    patch({ perDay: { ...state.perDay, [date]: choice } });
  }

  function onDeskChange(deskId: number, label: string): void {
    setDayChoice(activeDate, { resourceId: deskId, label, auto: false });
  }

  function onParkingChange(choice: ParkingChoice, label: string): void {
    setDayChoice(
      activeDate,
      choice === PARKING_AUTO
        ? { resourceId: null, label: null, auto: true }
        : { resourceId: choice, label, auto: false },
    );
  }

  // Copia la elección de la fecha activa a todas las fechas.
  function applyToAll(): void {
    const choice = state.perDay[activeDate];
    if (!choice) {
      return;
    }
    const next: Record<string, DayChoice> = {};
    dates.forEach((date) => {
      next[date] = choice;
    });
    patch({ perDay: next });
  }

  // Rellena con auto-asignación los días aún sin elección (solo plaza).
  function autoEmpties(): void {
    const next: Record<string, DayChoice> = { ...state.perDay };
    dates.forEach((date) => {
      if (!next[date]) {
        next[date] = { resourceId: null, label: null, auto: true };
      }
    });
    patch({ perDay: next });
  }

  function choiceText(date: string): { label: string; tone: string } {
    const choice = state.perDay[date];
    if (!choice) {
      return { label: t('wizard.perday.unset'), tone: ' is-unset' };
    }
    if (choice.auto) {
      return { label: t('wizard.perday.auto'), tone: ' is-auto' };
    }
    return { label: choice.label ?? String(choice.resourceId), tone: '' };
  }

  const activeChoiceSet = Boolean(state.perDay[activeDate]);
  const deskValue = state.perDay[activeDate]?.resourceId ?? null;
  const parkingValue: ParkingChoice | null = state.perDay[activeDate]?.auto
    ? PARKING_AUTO
    : (state.perDay[activeDate]?.resourceId ?? null);

  return (
    <div className="rzw-perday">
      <ul className="rzw-perday-list" role="tablist" aria-label={t('wizard.perday.listLabel')}>
        {dates.map((date) => {
          const active = date === activeDate;
          const { label, tone } = choiceText(date);
          return (
            <li key={date}>
              <button
                type="button"
                role="tab"
                aria-selected={active}
                className={`rzw-perday-row${active ? ' is-active' : ''}`}
                onClick={() => setActiveDate(date)}
              >
                <span className="rzw-perday-date">{longDate(date, i18n.language)}</span>
                <span className={`rzw-perday-choice${tone}`}>
                  {isDesk ? (
                    <i className="ti ti-armchair" aria-hidden="true" />
                  ) : (
                    <i className="ti ti-car" aria-hidden="true" />
                  )}
                  {label}
                </span>
                <i className="ti ti-pencil rzw-perday-edit" aria-hidden="true" />
              </button>
            </li>
          );
        })}
      </ul>

      <div className="rzw-perday-tools">
        <button
          type="button"
          className="rzw-linkbtn"
          disabled={!activeChoiceSet}
          onClick={applyToAll}
        >
          <i className="ti ti-copy" aria-hidden="true" />
          {t('wizard.perday.applyToAll')}
        </button>
        {!isDesk ? (
          <button type="button" className="rzw-linkbtn" onClick={autoEmpties}>
            <i className="ti ti-wand" aria-hidden="true" />
            {t('wizard.perday.autoEmpties')}
          </button>
        ) : null}
      </div>

      <div className="rzw-perday-editor">
        <p className="rzw-eyebrow rzw-eyebrow-accent">
          <i className="ti ti-calendar-event" aria-hidden="true" />
          {t('wizard.perday.editing', { date: longDate(activeDate, i18n.language) })}
        </p>
        {isDesk ? (
          <LocationDesk dates={[activeDate]} deskId={deskValue} onChange={onDeskChange} />
        ) : (
          <LocationParking
            dates={[activeDate]}
            employeeId={state.employeeId}
            allowAuto={state.beneficiaryType === 'EMPLOYEE'}
            choice={parkingValue}
            onChange={onParkingChange}
          />
        )}
      </div>
    </div>
  );
}
