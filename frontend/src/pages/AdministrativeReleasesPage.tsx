import { RESOURCE_ICON } from '../utils/resourceIcon';
import { useEffect, useMemo, useState, type ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import type { TFunction } from 'i18next';
import { InfoBanner } from '../components/InfoBanner';
import { EmbeddablePageHeader } from '../components/EmbeddablePageHeader';
import { StatusPill, type StatusTone } from '../components/StatusPill';
import { TableEmpty, TableError, TableSkeleton } from '../components/TableStates';
import {
  useBatchRelease,
  useEmployeeWeekOccupancyQuery,
  useSelectableReleaseEmployeesQuery,
  type BatchReleaseItem,
  type BatchReleaseResult,
} from '../hooks/useReleaseSelection';
import {
  addDaysIso,
  isoWeekNumber,
  longDate,
  mondayOfWeek,
  weekRangeLabel,
} from '../utils/calendar';
import { resourceLabel } from '../utils/resourceLabel';
import type { EmployeeWeekDay } from '../types/releaseSelection';
import type { OccupancyItem, OccupancyOrigin } from '../types/occupancy';
import type { ResourceType } from '../types/request';

const WEEK_LENGTH = 7;
// El backend exige motivo de 5..500 caracteres en admin-cancel (@Size); usamos
// el minimo mas estricto como valido para todo el lote.
const REASON_MIN = 5;

// Icono Tabler por tipo de recurso (mismo mapa que el resto de la app: plaza ->
// icono unico via RESOURCE_ICON. Solo presentacion.
function resourceIcon(type: ResourceType): string {
  return RESOURCE_ICON[type];
}

// Tono de la pill de estado segun el origen de la reserva (mapa unico
// estado->color del contrato §4): asignacion fija -> ocupado (verde),
// solicitud aprobada -> solicitud (naranja).
function originTone(origin: OccupancyOrigin): StatusTone {
  return origin === 'FIXED_ASSIGNMENT' ? 'occupied' : 'request';
}

// Clave estable de una reserva dentro de la semana (dia + recurso): sirve de
// `key` de React y de identificador de seleccion (nunca el indice del array).
function reservationKey(date: string, item: OccupancyItem): string {
  return `${date}__${item.resourceType}__${item.resourceId}`;
}

// Vista compartida ADMIN + AGENCIA: "Nueva liberación" por empleado y semana.
// Selecciona un empleado y delega en EmployeeWeekRelease el navegador de semana,
// la lista de reservas (plaza y puesto), la multi-seleccion y la liberacion en
// lote con un unico motivo. Cada reserva se libera por su mecanismo (asignacion
// fija -> liberacion administrativa; solicitud aprobada -> admin-cancel), resuelto
// en useBatchRelease segun el `origin`.
export function AdministrativeReleasesPage({ embedded = false }: { embedded?: boolean } = {}) {
  const { t } = useTranslation();
  const [employeeId, setEmployeeId] = useState<number | null>(null);
  const employeesQuery = useSelectableReleaseEmployeesQuery();
  const employees = employeesQuery.data ?? [];
  const selectedEmployee = employees.find((employee) => employee.id === employeeId) ?? null;

  return (
    <section className="administrative-releases-page" aria-label={t('releases.admin.title')}>
      <EmbeddablePageHeader
        embedded={embedded}
        eyebrow={t('releases.admin.eyebrow')}
        title={t('releases.admin.title')}
        description={t('releases.employeeWeek.description')}
      />

      <div className="release-body">
        <div className="release-field">
          <label className="release-field-label" htmlFor="release-employee">
            {t('releases.employeeWeek.employee')}
          </label>
          <div className="release-employee-select">
            <span className="release-avatar" aria-hidden="true">
              {employeeInitials(selectedEmployee?.fullName)}
            </span>
            <select
              id="release-employee"
              className="release-employee-input"
              value={employeeId ?? ''}
              onChange={(event) =>
                setEmployeeId(event.target.value === '' ? null : Number(event.target.value))
              }
            >
              <option value="">{t('releases.employeeWeek.selectEmployee')}</option>
              {employees.map((employee) => (
                <option key={employee.id} value={employee.id}>
                  {employee.fullName}
                </option>
              ))}
            </select>
            <i className="ti ti-selector release-employee-caret" aria-hidden="true" />
          </div>
        </div>

        {employeeId === null ? (
          <TableEmpty icon="user-question" message={t('releases.employeeWeek.selectEmployeePrompt')} />
        ) : (
          <EmployeeWeekRelease key={employeeId} employeeId={employeeId} />
        )}
      </div>
    </section>
  );
}

// Iniciales (max 2) del nombre para el avatar del selector de empleado.
function employeeInitials(fullName?: string): string {
  if (!fullName) {
    return '—';
  }
  const parts = fullName.trim().split(/\s+/).slice(0, 2);
  return parts.map((part) => part.charAt(0).toUpperCase()).join('');
}

// Flujo de liberacion de un empleado concreto: navegador de semana + lista de
// reservas con checkbox + motivo del lote + "Liberar". Se remonta al cambiar de
// empleado (key=employeeId), por lo que su estado (semana, seleccion, motivo)
// arranca limpio para cada empleado.
function EmployeeWeekRelease({ employeeId }: { employeeId: number }) {
  const { t, i18n } = useTranslation();
  const [weekStart, setWeekStart] = useState<string>(mondayOfWeek());
  const [selected, setSelected] = useState<Set<string>>(() => new Set());
  const [reason, setReason] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [feedback, setFeedback] = useState<BatchReleaseResult | null>(null);

  const occupancyQuery = useEmployeeWeekOccupancyQuery(employeeId, weekStart);
  const batch = useBatchRelease();

  const days = useMemo(() => occupancyQuery.data?.days ?? [], [occupancyQuery.data]);
  const daysWithReservations = useMemo(
    () => days.filter((day) => day.reservations.length > 0),
    [days],
  );
  const reservations = useMemo(() => flattenReservations(days), [days]);

  // Al cambiar de semana la seleccion y el feedback previos dejan de ser validos.
  useEffect(() => {
    setSelected(new Set());
    setFeedback(null);
    setError(null);
  }, [weekStart]);

  const rangeLabel =
    days.length > 0
      ? weekRangeLabel(days[0].date, days[days.length - 1].date, i18n.language)
      : weekStart;
  const weekNumber = isoWeekNumber(days.length > 0 ? days[0].date : weekStart);

  function toggle(key: string): void {
    setSelected((current) => {
      const next = new Set(current);
      if (next.has(key)) {
        next.delete(key);
      } else {
        next.add(key);
      }
      return next;
    });
  }

  function shiftWeek(deltaDays: number): void {
    setWeekStart((current) => addDaysIso(current, deltaDays));
  }

  function resetSelection(): void {
    setSelected(new Set());
    setReason('');
    setError(null);
    setFeedback(null);
  }

  function handleRelease(): void {
    const validationError = validateBatch(selected.size, reason, t);
    if (validationError) {
      setError(validationError);
      return;
    }
    setError(null);
    setFeedback(null);
    batch.mutate(
      { items: selectedItems(reservations, selected, employeeId), reason: reason.trim() },
      {
        onSuccess: (result) => {
          setFeedback(result);
          setSelected(new Set());
          setReason('');
        },
      },
    );
  }

  return (
    <>
      <WeekNavigator
        rangeLabel={rangeLabel}
        weekNumber={weekNumber}
        onPrevious={() => shiftWeek(-WEEK_LENGTH)}
        onNext={() => shiftWeek(WEEK_LENGTH)}
        onToday={() => setWeekStart(mondayOfWeek())}
      />

      {occupancyQuery.isLoading ? (
        <TableSkeleton label={t('releases.employeeWeek.loading')} columns={1} />
      ) : null}

      {occupancyQuery.isError ? (
        <TableError
          message={t('releases.employeeWeek.loadError')}
          retryLabel={t('common.retry')}
          onRetry={() => void occupancyQuery.refetch()}
        />
      ) : null}

      {occupancyQuery.isSuccess && daysWithReservations.length === 0 ? (
        <TableEmpty icon="calendar-off" message={t('releases.employeeWeek.empty')} />
      ) : null}

      {feedback ? (
        <InfoBanner
          variant={feedback.failed > 0 ? 'amber' : 'green'}
          icon={feedback.failed > 0 ? 'alert-triangle' : 'circle-check'}
        >
          {feedbackMessage(feedback, t)}
        </InfoBanner>
      ) : null}

      {occupancyQuery.isSuccess && daysWithReservations.length > 0 ? (
        <ReleaseBatchForm
          selectedCount={selected.size}
          reason={reason}
          error={error}
          pending={batch.isPending}
          onReasonChange={setReason}
          onSubmit={handleRelease}
          onCancel={resetSelection}
        >
          <ReservationsPicker
            days={daysWithReservations}
            selected={selected}
            onToggle={toggle}
            language={i18n.language}
          />
        </ReleaseBatchForm>
      ) : null}
    </>
  );
}

// Aplana las reservas de la semana (dia + item) conservando la clave de seleccion.
function flattenReservations(
  days: EmployeeWeekDay[],
): { key: string; date: string; item: OccupancyItem }[] {
  const list: { key: string; date: string; item: OccupancyItem }[] = [];
  for (const day of days) {
    for (const item of day.reservations) {
      list.push({ key: reservationKey(day.date, item), date: day.date, item });
    }
  }
  return list;
}

// Resuelve las reservas marcadas a items de lote (con el empleado seleccionado).
function selectedItems(
  reservations: { key: string; date: string; item: OccupancyItem }[],
  selected: Set<string>,
  employeeId: number,
): BatchReleaseItem[] {
  return reservations
    .filter((reservation) => selected.has(reservation.key))
    .map((reservation) => ({
      employeeId,
      resourceType: reservation.item.resourceType,
      resourceId: reservation.item.resourceId,
      releaseDate: reservation.date,
      origin: reservation.item.origin,
      requestId: reservation.item.requestId,
    }));
}

// Valida el lote antes de liberar: al menos una reserva marcada y motivo minimo.
function validateBatch(selectedCount: number, reason: string, t: TFunction): string | null {
  if (selectedCount === 0) {
    return t('releases.employeeWeek.requiredSelection');
  }
  if (reason.trim().length < REASON_MIN) {
    return t('releases.employeeWeek.requiredReason');
  }
  return null;
}

// Traduce el resultado del lote: todo bien, errores parciales, o todo fallido.
function feedbackMessage(result: BatchReleaseResult, t: TFunction): string {
  if (result.released === 0) {
    return t('releases.employeeWeek.feedback.failedAll');
  }
  if (result.failed > 0) {
    return t('releases.employeeWeek.feedback.partial', {
      released: result.released,
      failed: result.failed,
    });
  }
  return t('releases.employeeWeek.feedback.released', { count: result.released });
}

interface WeekNavigatorProps {
  rangeLabel: string;
  weekNumber: number;
  onPrevious: () => void;
  onNext: () => void;
  onToday: () => void;
}

// Navegador de semana (anterior / siguiente / hoy) con el rango visible y el
// numero de semana ISO.
function WeekNavigator({ rangeLabel, weekNumber, onPrevious, onNext, onToday }: WeekNavigatorProps) {
  const { t } = useTranslation();
  return (
    <nav className="release-week-nav" aria-label={t('releases.admin.title')}>
      <div className="release-week-nav-controls">
        <button
          type="button"
          className="release-week-nav-btn"
          aria-label={t('calendar.toolbar.previous')}
          onClick={onPrevious}
        >
          <i className="ti ti-chevron-left" aria-hidden="true" />
        </button>
        <button
          type="button"
          className="release-week-nav-btn"
          aria-label={t('calendar.toolbar.next')}
          onClick={onNext}
        >
          <i className="ti ti-chevron-right" aria-hidden="true" />
        </button>
      </div>
      <button type="button" className="release-week-today" onClick={onToday}>
        {t('calendar.toolbar.today')}
      </button>
      <span className="release-week-range" aria-live="polite">
        {rangeLabel}
      </span>
      <span className="release-week-number">
        {t('releases.employeeWeek.weekNumber', { week: weekNumber })}
      </span>
    </nav>
  );
}

interface ReservationsPickerProps {
  days: EmployeeWeekDay[];
  selected: Set<string>;
  onToggle: (key: string) => void;
  language: string;
}

// Lista de reservas de la semana agrupadas por dia, cada una con su checkbox
// (plaza y puesto, con etiqueta de recurso y origen).
function ReservationsPicker({ days, selected, onToggle, language }: ReservationsPickerProps) {
  return (
    <div className="release-days">
      {days.map((day) => (
        <div
          key={day.date}
          className="release-day"
          role="group"
          aria-label={longDate(day.date, language)}
        >
          <div className="release-day-head">
            <span className="release-day-name">{weekdayName(day.date, language)}</span>
            <span className="release-day-date">{longDate(day.date, language)}</span>
          </div>
          <div className="release-day-grid">
            {day.reservations.map((item) => (
              <ReservationCell
                key={reservationKey(day.date, item)}
                item={item}
                checked={selected.has(reservationKey(day.date, item))}
                onToggle={() => onToggle(reservationKey(day.date, item))}
              />
            ))}
          </div>
        </div>
      ))}
    </div>
  );
}

// Nombre del dia de la semana (serif, capitalize) sin el resto de la fecha.
function weekdayName(dateIso: string, locale: string): string {
  const parsed = new Date(`${dateIso}T00:00:00`);
  if (Number.isNaN(parsed.getTime())) {
    return dateIso;
  }
  return new Intl.DateTimeFormat(locale, { weekday: 'long' }).format(parsed);
}

interface ReservationCellProps {
  item: OccupancyItem;
  checked: boolean;
  onToggle: () => void;
}

// Celda de una reserva: checkbox + icono del recurso + etiqueta/subtitulo + pill
// de origen. Toda la celda es un label clicable; resalta cuando esta marcada.
function ReservationCell({ item, checked, onToggle }: ReservationCellProps) {
  const { t } = useTranslation();
  return (
    <label className={`release-cell${checked ? ' is-checked' : ''}`}>
      <input
        type="checkbox"
        className="release-cell-input"
        checked={checked}
        onChange={onToggle}
      />
      <span className="release-cell-box" aria-hidden="true">
        <i className="ti ti-check" />
      </span>
      <i className={`ti ti-${resourceIcon(item.resourceType)} release-cell-icon`} aria-hidden="true" />
      <span className="release-cell-text">
        <span className="release-cell-label">{resourceLabel(item, t)}</span>
        <span className="release-cell-sub">
          {t(`releases.employeeWeek.resourceKind.${item.resourceType}`)}
        </span>
      </span>
      <StatusPill tone={originTone(item.origin)}>
        {t(`releases.byDate.origin.${item.origin}`)}
      </StatusPill>
    </label>
  );
}

interface ReleaseBatchFormProps {
  selectedCount: number;
  reason: string;
  error: string | null;
  pending: boolean;
  onReasonChange: (value: string) => void;
  onSubmit: () => void;
  onCancel: () => void;
  children: ReactNode;
}

// Motivo del lote + footer de accion (recuento + Cancelar + Liberar). Envuelve la
// lista de reservas para que el formulario cubra seleccion y motivo.
function ReleaseBatchForm({
  selectedCount,
  reason,
  error,
  pending,
  onReasonChange,
  onSubmit,
  onCancel,
  children,
}: ReleaseBatchFormProps) {
  const { t } = useTranslation();
  return (
    <form
      className="release-form"
      onSubmit={(event) => {
        event.preventDefault();
        onSubmit();
      }}
      noValidate
    >
      {children}

      <div className="release-reason">
        <label className="release-field-label" htmlFor="release-batch-reason">
          {t('releases.employeeWeek.reason')}
        </label>
        <textarea
          id="release-batch-reason"
          className="release-reason-input"
          value={reason}
          onChange={(event) => onReasonChange(event.target.value)}
        />
        <p className="release-reason-hint">
          <i className="ti ti-alert-circle" aria-hidden="true" />
          {t('releases.employeeWeek.reasonHint')}
        </p>
        {error ? (
          <p className="form-error" role="alert">
            {error}
          </p>
        ) : null}
      </div>

      <div className="release-footer">
        <span
          className="release-selected"
          aria-live="polite"
          aria-label={t('releases.employeeWeek.selectedCount', { count: selectedCount })}
        >
          <span className="release-selected-badge">{selectedCount}</span>
          {t('releases.employeeWeek.selectedCountLabel')}
        </span>
        <div className="release-footer-actions">
          <button type="button" className="btn btn-white" onClick={onCancel} disabled={pending}>
            {t('releases.employeeWeek.cancel')}
          </button>
          <button type="submit" className="btn btn-green" disabled={pending}>
            <i className="ti ti-arrow-back-up" aria-hidden="true" />
            {t('releases.employeeWeek.releaseBatch')}
          </button>
        </div>
      </div>
    </form>
  );
}
