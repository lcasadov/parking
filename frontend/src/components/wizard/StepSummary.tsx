import { useTranslation } from 'react-i18next';
import { useSelectableReleaseEmployeesQuery } from '../../hooks/useReleaseSelection';
import { longDate } from '../../utils/calendar';
import { PARKING_AUTO, RESOURCE_DESK } from './wizardTypes';
import type { WizardState } from './wizardTypes';

interface StepSummaryProps {
  state: WizardState;
  dates: string[];
}

// Paso 5 — Resumen y confirmación: recap (tipo · empleado · recurso · fechas) y aviso
// destacado de que al confirmar se notifica por email al empleado. El botón Confirmar
// vive en el pie del asistente.
export function StepSummary({ state, dates }: StepSummaryProps) {
  const { t, i18n } = useTranslation();
  const employeesQuery = useSelectableReleaseEmployeesQuery();
  const employee = employeesQuery.data?.find((candidate) => candidate.id === state.employeeId);
  const employeeName = employee?.fullName ?? t('wizard.summary.unknownEmployee');

  const isDesk = state.resourceType === RESOURCE_DESK;
  const resourceTypeLabel = t(isDesk ? 'wizard.resource.desk' : 'wizard.resource.parking');
  const isAuto = !isDesk && state.parkingChoice === PARKING_AUTO;
  const locationLabel = isAuto ? t('wizard.location.anyFree') : (state.chosenLabel ?? '—');

  const rows = [
    { icon: isDesk ? 'armchair' : 'car', label: t('wizard.summary.resourceType'), value: resourceTypeLabel },
    { icon: 'user', label: t('wizard.summary.employee'), value: employeeName },
    { icon: 'map-pin', label: t('wizard.summary.location'), value: locationLabel },
  ];

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

      <p className="rzw-notice" role="note">
        <i className="ti ti-mail" aria-hidden="true" />
        <span>{t('wizard.summary.emailNotice', { name: employeeName })}</span>
      </p>
    </div>
  );
}
