import { useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import type { TFunction } from 'i18next';
import { Button } from '../components/Button';
import { InfoBanner } from '../components/InfoBanner';
import { PageHeader } from '../components/PageHeader';
import { StatusPill } from '../components/StatusPill';
import { TableEmpty, TableError, TableSkeleton } from '../components/TableStates';
import {
  useBatchRelease,
  useEmployeeWeekOccupancyQuery,
  useSelectableReleaseEmployeesQuery,
  type BatchReleaseItem,
  type BatchReleaseResult,
} from '../hooks/useReleaseSelection';
import { addDaysIso, longDate, mondayOfWeek, weekRangeLabel } from '../utils/calendar';
import { resourceLabel } from '../utils/resourceLabel';
import type { EmployeeWeekDay } from '../types/releaseSelection';
import type { OccupancyItem } from '../types/occupancy';

const WEEK_LENGTH = 7;
// El backend exige motivo de 5..500 caracteres en admin-cancel (@Size); usamos
// el minimo mas estricto como valido para todo el lote.
const REASON_MIN = 5;

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
export function AdministrativeReleasesPage() {
  const { t } = useTranslation();
  const [employeeId, setEmployeeId] = useState<number | null>(null);
  const employeesQuery = useSelectableReleaseEmployeesQuery();
  const employees = employeesQuery.data ?? [];

  return (
    <section className="administrative-releases-page" aria-label={t('releases.admin.title')}>
      <PageHeader
        eyebrow={t('releases.admin.eyebrow')}
        title={t('releases.admin.title')}
        description={t('releases.employeeWeek.description')}
      />

      <InfoBanner variant="blue" icon="info-circle">
        {t('releases.employeeWeek.intro')}
      </InfoBanner>

      <div className="filter-bar">
        <label className="field-label" htmlFor="release-employee">
          {t('releases.employeeWeek.employee')}
        </label>
        <select
          id="release-employee"
          className="field-input"
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
      </div>

      {employeeId === null ? (
        <InfoBanner variant="amber" icon="user-question">
          {t('releases.employeeWeek.selectEmployeePrompt')}
        </InfoBanner>
      ) : (
        <EmployeeWeekRelease key={employeeId} employeeId={employeeId} />
      )}
    </section>
  );
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

      {occupancyQuery.isSuccess && daysWithReservations.length > 0 ? (
        <>
          <ReservationsPicker
            days={daysWithReservations}
            selected={selected}
            onToggle={toggle}
            language={i18n.language}
          />
          <ReleaseBatchForm
            selectedCount={selected.size}
            reason={reason}
            error={error}
            pending={batch.isPending}
            onReasonChange={setReason}
            onSubmit={handleRelease}
          />
        </>
      ) : null}

      {feedback ? (
        <InfoBanner
          variant={feedback.failed > 0 ? 'amber' : 'green'}
          icon={feedback.failed > 0 ? 'alert-triangle' : 'circle-check'}
        >
          {feedbackMessage(feedback, t)}
        </InfoBanner>
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
  onPrevious: () => void;
  onNext: () => void;
  onToday: () => void;
}

// Navegador de semana (anterior / siguiente / hoy) con el rango visible.
function WeekNavigator({ rangeLabel, onPrevious, onNext, onToday }: WeekNavigatorProps) {
  const { t } = useTranslation();
  return (
    <nav className="week-nav" aria-label={t('releases.admin.title')}>
      <div className="week-nav-controls">
        <Button
          variant="white"
          className="btn-icon-only"
          icon="chevron-left"
          aria-label={t('calendar.toolbar.previous')}
          onClick={onPrevious}
        />
        <Button
          variant="white"
          className="btn-icon-only"
          icon="chevron-right"
          aria-label={t('calendar.toolbar.next')}
          onClick={onNext}
        />
        <Button variant="white" onClick={onToday}>
          {t('calendar.toolbar.today')}
        </Button>
      </div>
      <div className="week-nav-range">
        <span className="week-range" aria-live="polite">
          {rangeLabel}
        </span>
      </div>
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
  const { t } = useTranslation();
  return (
    <div className="release-week-list">
      {days.map((day) => (
        <fieldset key={day.date} className="release-day-group">
          <legend className="release-day-legend">{longDate(day.date, language)}</legend>
          {day.reservations.map((item) => {
            const key = reservationKey(day.date, item);
            return (
              <label key={key} className="release-reservation-row">
                <input
                  type="checkbox"
                  checked={selected.has(key)}
                  onChange={() => onToggle(key)}
                />
                <span className="release-reservation-label">{resourceLabel(item, t)}</span>
                <StatusPill tone="released">
                  {t(`releases.byDate.origin.${item.origin}`)}
                </StatusPill>
              </label>
            );
          })}
        </fieldset>
      ))}
    </div>
  );
}

interface ReleaseBatchFormProps {
  selectedCount: number;
  reason: string;
  error: string | null;
  pending: boolean;
  onReasonChange: (value: string) => void;
  onSubmit: () => void;
}

// Motivo del lote + boton "Liberar" con el recuento de reservas seleccionadas.
function ReleaseBatchForm({
  selectedCount,
  reason,
  error,
  pending,
  onReasonChange,
  onSubmit,
}: ReleaseBatchFormProps) {
  const { t } = useTranslation();
  return (
    <form
      className="release-batch-form"
      onSubmit={(event) => {
        event.preventDefault();
        onSubmit();
      }}
      noValidate
    >
      <label className="field-label" htmlFor="release-batch-reason">
        {t('releases.employeeWeek.reason')}
      </label>
      <textarea
        id="release-batch-reason"
        className="field-input"
        value={reason}
        onChange={(event) => onReasonChange(event.target.value)}
      />
      <p className="hint">{t('releases.employeeWeek.reasonHint')}</p>

      {error ? (
        <p className="form-error" role="alert">
          {error}
        </p>
      ) : null}

      <div className="modal-footer-inline">
        <span className="release-selected-count" aria-live="polite">
          {t('releases.employeeWeek.selectedCount', { count: selectedCount })}
        </span>
        <Button variant="red" icon="arrow-back-up" submit disabled={pending}>
          {t('releases.employeeWeek.release')}
        </Button>
      </div>
    </form>
  );
}
