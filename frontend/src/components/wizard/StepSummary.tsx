import { type ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { useSelectableReleaseEmployeesQuery } from '../../hooks/useReleaseSelection';
import { useVisitorsQuery } from '../../hooks/useVisitors';
import { useSuggestedSpaces } from '../../hooks/useSuggestedSpaces';
import { longDate } from '../../utils/calendar';
import { PARKING_AUTO, RESOURCE_DESK } from './wizardTypes';
import type { WizardState } from './wizardTypes';

interface StepSummaryProps {
  state: WizardState;
  dates: string[];
}

interface SummaryRow {
  icon: string;
  label: string;
  value: string;
}

interface PreviewItem {
  date: string;
  value: string;
  tone: string;
}

// Fechas cuya plaza auto hay que previsualizar: en PER_DAY las marcadas como auto;
// en ALL-auto todas; en el resto ninguna.
function autoPreviewDates(
  state: WizardState,
  dates: string[],
  perDayMode: boolean,
  allAuto: boolean,
): string[] {
  if (perDayMode) {
    return dates.filter((date) => state.perDay[date]?.auto);
  }
  return allAuto ? dates : [];
}

// Paso 5 — Resumen y confirmación. Recap (tipo · beneficiario · categoría) + aviso de
// email (empleado) o "sin email" (visitante). La ubicación se muestra según el modo.
export function StepSummary({ state, dates }: StepSummaryProps) {
  const { t, i18n } = useTranslation();
  const isVisitor = state.beneficiaryType === 'VISITOR';
  const employeesQuery = useSelectableReleaseEmployeesQuery();
  const visitorsQuery = useVisitorsQuery({ page: 0, size: 100 });
  const employee = employeesQuery.data?.find((candidate) => candidate.id === state.employeeId);
  const visitor = visitorsQuery.data?.content.find((candidate) => candidate.id === state.visitorId);
  const beneficiaryName = isVisitor
    ? (visitor ? `${visitor.firstName} ${visitor.lastName}`.trim() : t('wizard.summary.unknownEmployee'))
    : (employee?.fullName ?? t('wizard.summary.unknownEmployee'));

  const isDesk = state.resourceType === RESOURCE_DESK;
  const perDayMode = dates.length > 1 && state.locationMode === 'PER_DAY';
  const allAuto = !perDayMode && !isDesk && state.parkingChoice === PARKING_AUTO;

  const autoDates = autoPreviewDates(state, dates, perDayMode, allAuto);
  const suggested = useSuggestedSpaces(state.employeeId, autoDates, autoDates.length > 0);
  const suggestedByDate = new Map(suggested.byDate.map((entry) => [entry.date, entry]));

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

  // Texto de la elección de un día (modo PER_DAY): auto → resuelto a la plaza;
  // recurso concreto → su etiqueta; sin elegir → guion.
  function perDayText(date: string): { value: string; tone: string } {
    const choice = state.perDay[date];
    if (choice?.auto) {
      const auto = autoText(date);
      return { value: `${t('wizard.perday.auto')} · ${auto.value}`, tone: auto.tone };
    }
    if (choice && choice.resourceId !== null) {
      return { value: choice.label ?? String(choice.resourceId), tone: '' };
    }
    return { value: '—', tone: '' };
  }

  function summaryRows(): SummaryRow[] {
    const rows: SummaryRow[] = [
      {
        icon: isDesk ? 'armchair' : 'car',
        label: t('wizard.summary.resourceType'),
        value: t(isDesk ? 'wizard.resource.desk' : 'wizard.resource.parking'),
      },
      {
        icon: isVisitor ? 'user-plus' : 'user',
        label: t(isVisitor ? 'wizard.summary.visitor' : 'wizard.summary.employee'),
        value: beneficiaryName,
      },
    ];
    if (!isVisitor && employee?.category) {
      rows.push({
        icon: 'stairs-up',
        label: t('wizard.summary.category'),
        value: t(`employees.category.${employee.category}`),
      });
    }
    if (!perDayMode && !allAuto) {
      rows.push({ icon: 'map-pin', label: t('wizard.summary.location'), value: state.chosenLabel ?? '—' });
    }
    return rows;
  }

  function renderPreview(icon: string, heading: ReactNode, items: PreviewItem[]): ReactNode {
    return (
      <div className="rzw-auto-preview">
        <p className="rzw-auto-preview-head">
          <i className={`ti ti-${icon}`} aria-hidden="true" />
          <span>{heading}</span>
        </p>
        <ul className="rzw-auto-preview-list">
          {items.map((item) => (
            <li key={item.date} className={`rzw-auto-preview-row${item.tone}`}>
              <span className="rzw-auto-preview-date mono">{longDate(item.date, i18n.language)}</span>
              <span className="rzw-auto-preview-space">{item.value}</span>
            </li>
          ))}
        </ul>
      </div>
    );
  }

  const autoHead = (
    <>
      <strong>{t('wizard.summary.autoAssignTitle')}</strong>
      <br />
      {t('wizard.summary.autoAssignHint')}
    </>
  );

  return (
    <div className="rzw-step-body">
      <dl className="rzw-recap">
        {summaryRows().map((row) => (
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

      {allAuto
        ? renderPreview('wand', autoHead, dates.map((date) => ({ date, ...autoText(date) })))
        : null}

      {perDayMode
        ? renderPreview(
            'calendar-cog',
            <strong>{t('wizard.summary.perDayTitle')}</strong>,
            dates.map((date) => ({ date, ...perDayText(date) })),
          )
        : null}

      <p className={`rzw-notice${isVisitor ? ' is-muted' : ''}`} role="note">
        <i className={`ti ti-${isVisitor ? 'mail-off' : 'mail'}`} aria-hidden="true" />
        <span>
          {t(isVisitor ? 'wizard.summary.noEmailNotice' : 'wizard.summary.emailNotice', {
            name: beneficiaryName,
          })}
        </span>
      </p>
    </div>
  );
}
