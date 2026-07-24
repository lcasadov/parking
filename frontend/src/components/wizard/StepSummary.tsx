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

// Paso 5 — Resumen y confirmación: recap (tipo · empleado · categoría · recurso ·
// fechas) y aviso destacado de que al confirmar se notifica por email al empleado.
// En auto-asignación de plaza, resuelve y muestra la plaza EXACTA que se asignaría
// por cada fecha (según la categoría del empleado) ANTES de confirmar. El botón
// Confirmar vive en el pie del asistente.
export function StepSummary({ state, dates }: StepSummaryProps) {
  const { t, i18n } = useTranslation();
  const employeesQuery = useSelectableReleaseEmployeesQuery();
  const employee = employeesQuery.data?.find((candidate) => candidate.id === state.employeeId);
  const employeeName = employee?.fullName ?? t('wizard.summary.unknownEmployee');

  const isDesk = state.resourceType === RESOURCE_DESK;
  const resourceTypeLabel = t(isDesk ? 'wizard.resource.desk' : 'wizard.resource.parking');
  const isAuto = !isDesk && state.parkingChoice === PARKING_AUTO;

  // Preview de auto-asignación por categoría (solo cuando aplica): plaza por fecha.
  const suggested = useSuggestedSpaces(state.employeeId, dates, isAuto);

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
  if (!isAuto) {
    rows.push({
      icon: 'map-pin',
      label: t('wizard.summary.location'),
      value: state.chosenLabel ?? '—',
    });
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
      </dl>

      {isAuto ? (
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
            {suggested.byDate.map((entry) => {
              const space = entry.space;
              let value: string;
              let tone = '';
              if (entry.isLoading) {
                value = t('wizard.summary.autoResolving');
              } else if (entry.isError) {
                value = t('wizard.summary.autoError');
                tone = ' is-error';
              } else if (space?.available) {
                value = t('wizard.summary.autoSpace', {
                  number: space.number,
                  floor: space.floor,
                });
              } else {
                value = t('wizard.summary.autoNoSpace');
                tone = ' is-warn';
              }
              return (
                <li key={entry.date} className={`rzw-auto-preview-row${tone}`}>
                  <span className="rzw-auto-preview-date mono">
                    {longDate(entry.date, i18n.language)}
                  </span>
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
