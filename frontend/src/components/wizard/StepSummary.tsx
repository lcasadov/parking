import { useTranslation } from 'react-i18next';
import { useSelectableReleaseEmployeesQuery } from '../../hooks/useReleaseSelection';
import { useSuggestedSpaces } from '../../hooks/useSuggestedSpaces';
import { longDate } from '../../utils/calendar';
import { PARKING_AUTO, RESOURCE_DESK } from './wizardTypes';
import type { WizardState } from './wizardTypes';

interface StepSummaryProps {
  state: WizardState;
  dates: string[];
}

// Paso 5 — Resumen y confirmación. Recap (tipo · empleado · categoría) + aviso de
// email. La ubicación se muestra según el modo: ALL con una sola plaza/puesto (y si
// es auto, la plaza exacta por fecha); PER_DAY con una fila por fecha y su recurso
// (auto resuelto a la plaza concreta). El botón Confirmar vive en el pie.
export function StepSummary({ state, dates }: StepSummaryProps) {
  const { t, i18n } = useTranslation();
  const employeesQuery = useSelectableReleaseEmployeesQuery();
  const employee = employeesQuery.data?.find((candidate) => candidate.id === state.employeeId);
  const employeeName = employee?.fullName ?? t('wizard.summary.unknownEmployee');

  const isDesk = state.resourceType === RESOURCE_DESK;
  const resourceTypeLabel = t(isDesk ? 'wizard.resource.desk' : 'wizard.resource.parking');
  const perDayMode = dates.length > 1 && state.locationMode === 'PER_DAY';
  const allAuto = !perDayMode && !isDesk && state.parkingChoice === PARKING_AUTO;

  // Fechas con auto-asignación (según el modo) cuya plaza hay que previsualizar.
  const autoDates = perDayMode
    ? dates.filter((date) => state.perDay[date]?.auto)
    : allAuto
      ? dates
      : [];
  const suggested = useSuggestedSpaces(state.employeeId, autoDates, autoDates.length > 0);
  const suggestedByDate = new Map(suggested.byDate.map((entry) => [entry.date, entry]));

  const rows = [
    { icon: isDesk ? 'armchair' : 'car', label: t('wizard.summary.resourceType'), value: resourceTypeLabel },
    { icon: 'user', label: t('wizard.summary.employee'), value: employeeName },
  ];
  if (employee?.category) {
    rows.push({
      icon: 'stairs-up',
      label: t('wizard.summary.category'),
      value: t(`employees.category.${employee.category}`),
    });
  }
  if (!perDayMode && !allAuto) {
    rows.push({ icon: 'map-pin', label: t('wizard.summary.location'), value: state.chosenLabel ?? '—' });
  }

  // Texto de la plaza auto para una fecha (cargando / plaza N · planta P / sin plaza).
  function autoText(date: string): { value: string; tone: string } {
    const entry = suggestedByDate.get(date);
    if (!entry || entry.isLoading) {
      return { value: t('wizard.summary.autoResolving'), tone: '' };
    }
    if (entry.isError) {
      return { value: t('wizard.summary.autoError'), tone: ' is-error' };
    }
    if (entry.space?.available) {
      return {
        value: t('wizard.summary.autoSpace', { number: entry.space.number, floor: entry.space.floor }),
        tone: '',
      };
    }
    return { value: t('wizard.summary.autoNoSpace'), tone: ' is-warn' };
  }

  return (
    <div className="rzw-step-body">
      <dl className="rzw-recap">
        {rows.map((row) => (
          <div key={row.label} className="rzw-recap-row">
            <dt>
              <i className={`ti ti-${row.icon}`} aria-hidden="true" />
              {row.label}
            </dt>
            <dd>{row.value}</dd>
          </div>
        ))}
        {!perDayMode ? (
          <div className="rzw-recap-row rzw-recap-dates">
            <dt>
              <i className="ti ti-calendar" aria-hidden="true" />
              {t('wizard.summary.dates', { count: dates.length })}
            </dt>
            <dd>
              <ul className="rzw-recap-chips">
                {dates.map((iso) => (
                  <li key={iso} className="rzw-chip mono">
                    {longDate(iso, i18n.language)}
                  </li>
                ))}
              </ul>
            </dd>
          </div>
        ) : null}
      </dl>

      {allAuto ? (
        <div className="rzw-auto-preview">
          <p className="rzw-auto-preview-head">
            <i className="ti ti-wand" aria-hidden="true" />
            <span>
              <strong>{t('wizard.summary.autoAssignTitle')}</strong>
              <br />
              {t('wizard.summary.autoAssignHint')}
            </span>
          </p>
          <ul className="rzw-auto-preview-list">
            {dates.map((date) => {
              const { value, tone } = autoText(date);
              return (
                <li key={date} className={`rzw-auto-preview-row${tone}`}>
                  <span className="rzw-auto-preview-date mono">{longDate(date, i18n.language)}</span>
                  <span className="rzw-auto-preview-space">{value}</span>
                </li>
              );
            })}
          </ul>
        </div>
      ) : null}

      {perDayMode ? (
        <div className="rzw-auto-preview">
          <p className="rzw-auto-preview-head">
            <i className="ti ti-calendar-cog" aria-hidden="true" />
            <span>
              <strong>{t('wizard.summary.perDayTitle')}</strong>
            </span>
          </p>
          <ul className="rzw-auto-preview-list">
            {dates.map((date) => {
              const choice = state.perDay[date];
              let value = '—';
              let tone = '';
              if (choice?.auto) {
                const auto = autoText(date);
                value = `${t('wizard.perday.auto')} · ${auto.value}`;
                tone = auto.tone;
              } else if (choice?.resourceId != null) {
                value = choice.label ?? String(choice.resourceId);
              }
              return (
                <li key={date} className={`rzw-auto-preview-row${tone}`}>
                  <span className="rzw-auto-preview-date mono">{longDate(date, i18n.language)}</span>
                  <span className="rzw-auto-preview-space">{value}</span>
                </li>
              );
            })}
          </ul>
        </div>
      ) : null}

      <p className="rzw-notice" role="note">
        <i className="ti ti-mail" aria-hidden="true" />
        <span>{t('wizard.summary.emailNotice', { name: employeeName })}</span>
      </p>
    </div>
  );
}
