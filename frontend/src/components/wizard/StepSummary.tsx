import { RESOURCE_ICON } from '../../utils/resourceIcon';
import { type ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import type { TFunction } from 'i18next';
import { useSelectableReleaseEmployeesQuery } from '../../hooks/useReleaseSelection';
import { useVisitorsQuery } from '../../hooks/useVisitors';
import { useSuggestedSpaces } from '../../hooks/useSuggestedSpaces';
import { longDate } from '../../utils/calendar';
import { PARKING_AUTO, RESOURCE_DESK, RESOURCE_PARKING } from './wizardTypes';
import type { TypeLocation, WizardState } from './wizardTypes';
import type { ResourceType } from '../../types/request';

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

// ¿Está la ubicación de un tipo en modo auto-completo (plaza, todos los días auto)?
function isAllAuto(location: TypeLocation, type: ResourceType, perDayMode: boolean): boolean {
  return !perDayMode && type === RESOURCE_PARKING && location.parkingChoice === PARKING_AUTO;
}

// Fechas cuya plaza auto hay que previsualizar (solo plaza): en PER_DAY las marcadas
// como auto; en ALL-auto todas; en el resto ninguna.
function autoPreviewDates(location: TypeLocation, type: ResourceType, dates: string[]): string[] {
  if (type !== RESOURCE_PARKING) {
    return [];
  }
  const perDayMode = dates.length > 1 && location.locationMode === 'PER_DAY';
  if (perDayMode) {
    return dates.filter((date) => location.perDay[date]?.auto);
  }
  return isAllAuto(location, type, perDayMode) ? dates : [];
}

// Sección de resumen de la ubicación de UN tipo (Plaza / Puesto): etiqueta simple
// (modo ALL concreto), preview de plazas auto (ALL-auto) o lista por día (PER_DAY).
function LocationSection({
  type,
  location,
  dates,
  autoText,
  t,
  renderPreview,
}: {
  type: ResourceType;
  location: TypeLocation;
  dates: string[];
  autoText: (date: string) => { value: string; tone: string };
  t: TFunction;
  renderPreview: (icon: string, heading: ReactNode, items: PreviewItem[]) => ReactNode;
}) {
  const isDesk = type === RESOURCE_DESK;
  const perDayMode = dates.length > 1 && location.locationMode === 'PER_DAY';
  const allAuto = isAllAuto(location, type, perDayMode);
  const typeName = t(isDesk ? 'wizard.resource.desk' : 'wizard.resource.parking');

  function perDayText(date: string): { value: string; tone: string } {
    const choice = location.perDay[date];
    if (choice?.auto) {
      const auto = autoText(date);
      return { value: `${t('wizard.perday.auto')} · ${auto.value}`, tone: auto.tone };
    }
    if (choice && choice.resourceId !== null) {
      return { value: choice.label ?? String(choice.resourceId), tone: '' };
    }
    return { value: '—', tone: '' };
  }

  return (
    <div className="rzw-summary-section">
      <p className="rzw-eyebrow rzw-eyebrow-accent">
        <i className={`ti ti-${isDesk ? RESOURCE_ICON.DESK : 'car'}`} aria-hidden="true" />
        {t('wizard.summary.locationOf', { resource: typeName })}
      </p>
      {!perDayMode && !allAuto ? (
        <p className="rzw-summary-line">
          <i className="ti ti-map-pin" aria-hidden="true" />
          <span>{location.chosenLabel ?? '—'}</span>
        </p>
      ) : null}
      {allAuto
        ? renderPreview(
            'wand',
            <strong>{t('wizard.summary.autoAssignTitle')}</strong>,
            dates.map((date) => ({ date, ...autoText(date) })),
          )
        : null}
      {perDayMode
        ? renderPreview(
            'calendar-cog',
            <strong>{t('wizard.summary.perDayTitle')}</strong>,
            dates.map((date) => ({ date, ...perDayText(date) })),
          )
        : null}
    </div>
  );
}

// Paso final — Resumen y confirmación. Recap (tipos · beneficiario · categoría · fechas)
// + una sección de ubicación por cada tipo elegido + aviso de email (empleado) o
// "sin email" (visitante).
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

  // La auto-asignación solo aplica a la plaza: sus fechas auto se resuelven en lote.
  const parkingLocation = state.locations[RESOURCE_PARKING];
  const autoDates = state.resourceTypes.includes(RESOURCE_PARKING)
    ? autoPreviewDates(parkingLocation, RESOURCE_PARKING, dates)
    : [];
  const suggested = useSuggestedSpaces(state.employeeId, autoDates, autoDates.length > 0);
  const suggestedByDate = new Map(suggested.byDate.map((entry) => [entry.date, entry]));

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

  function summaryRows(): SummaryRow[] {
    const typeNames = state.resourceTypes
      .map((type) => t(type === RESOURCE_DESK ? 'wizard.resource.desk' : 'wizard.resource.parking'))
      .join(' · ');
    const rows: SummaryRow[] = [
      {
        icon: state.resourceTypes.includes(RESOURCE_DESK) && state.resourceTypes.length === 1
          ? RESOURCE_ICON.DESK
          : 'layout-grid',
        label: t('wizard.summary.resourceType'),
        value: typeNames || '—',
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

      {state.resourceTypes.map((type) => (
        <LocationSection
          key={type}
          type={type}
          location={state.locations[type]}
          dates={dates}
          autoText={autoText}
          t={t}
          renderPreview={renderPreview}
        />
      ))}

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
