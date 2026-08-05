import { RESOURCE_ICON } from '../utils/resourceIcon';
import { useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import type { TFunction } from 'i18next';
import { Button } from '../components/Button';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { InfoBanner } from '../components/InfoBanner';
import { EmbeddablePageHeader } from '../components/EmbeddablePageHeader';
import { StatusPill, type StatusTone } from '../components/StatusPill';
import { TableEmpty, TableError, TableSkeleton } from '../components/TableStates';
import {
  useBatchRelease,
  useEmployeeRangeOccupancyQuery,
  useEmployeeWeekOccupancyQuery,
  type BatchReleaseItem,
  type BatchReleaseResult,
} from '../hooks/useReleaseSelection';
import {
  addDaysIso,
  longDate,
  mondayOfWeek,
  weekRangeLabel,
} from '../utils/calendar';
import { resourceLabel } from '../utils/resourceLabel';
import { todayIso } from '../utils/releases';
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
// El selector de empleado se IZA al control-row del hub (ReleaseHubPage): esta página
// recibe el `employeeId` ya seleccionado y solo muestra el vacío o la semana del empleado.
export function AdministrativeReleasesPage({
  embedded = false,
  employeeId = null,
}: {
  embedded?: boolean;
  employeeId?: number | null;
} = {}) {
  const { t } = useTranslation();

  return (
    <section className="administrative-releases-page" aria-label={t('releases.admin.title')}>
      <EmbeddablePageHeader
        embedded={embedded}
        eyebrow={t('releases.admin.eyebrow')}
        title={t('releases.admin.title')}
        description={t('releases.employeeWeek.description')}
      />

      <div className="release-body">
        {employeeId === null ? (
          <TableEmpty icon="user-question" message={t('releases.employeeWeek.selectEmployeePrompt')} />
        ) : (
          <EmployeeWeekRelease key={employeeId} employeeId={employeeId} />
        )}
      </div>
    </section>
  );
}

type ReleaseMode = 'week' | 'range';

// Selecciona todas las reservas liberables (de hoy en adelante) de la lista de días:
// usado como preselección por defecto del modo RANGO (opt-out: el admin ya declaró la
// intención al elegir el rango, deselecciona lo que no quiera liberar).
function allReleasableKeys(days: EmployeeWeekDay[]): Set<string> {
  const today = todayIso();
  const keys = new Set<string>();
  for (const day of days) {
    if (day.date < today) {
      continue;
    }
    for (const item of day.reservations) {
      keys.add(reservationKey(day.date, item));
    }
  }
  return keys;
}

// Flujo de liberacion de un empleado concreto en dos modos: SEMANA (navegador de
// semana, selección opt-in) o RANGO (rango de fechas para vacaciones/ausencias, con
// todas las reservas preseleccionadas). Comparte lista + barra de acción + modal de
// motivo + liberación en lote. Se remonta al cambiar de empleado (key=employeeId).
function EmployeeWeekRelease({ employeeId }: { employeeId: number }) {
  const { t, i18n } = useTranslation();
  const [mode, setMode] = useState<ReleaseMode>('week');
  const [weekStart, setWeekStart] = useState<string>(mondayOfWeek());
  const [from, setFrom] = useState<string>(() => todayIso());
  const [to, setTo] = useState<string>(() => addDaysIso(todayIso(), 6));
  const [selected, setSelected] = useState<Set<string>>(() => new Set());
  const [reason, setReason] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [feedback, setFeedback] = useState<BatchReleaseResult | null>(null);
  const [batchOpen, setBatchOpen] = useState(false);

  const weekQuery = useEmployeeWeekOccupancyQuery(employeeId, mode === 'week' ? weekStart : '');
  const rangeQuery = useEmployeeRangeOccupancyQuery(employeeId, from, to, mode === 'range');
  const query = mode === 'week' ? weekQuery : rangeQuery;
  const batch = useBatchRelease();

  const days = useMemo(() => query.data?.days ?? [], [query.data]);
  // Solo días de hoy en adelante con reservas: una fecha pasada no se puede liberar
  // (el backend responde 409), así que se oculta (misma regla que el `min` de hoy).
  const daysWithReservations = useMemo(() => {
    const today = todayIso();
    return days.filter((day) => day.reservations.length > 0 && day.date >= today);
  }, [days]);
  const reservations = useMemo(() => flattenReservations(days), [days]);

  // Al cambiar de modo, de semana o de rango, la selección y el feedback previos dejan
  // de ser válidos: se limpian (en RANGO el efecto siguiente repuebla con todo).
  useEffect(() => {
    setSelected(new Set());
    setFeedback(null);
    setError(null);
  }, [mode, weekStart, from, to]);

  // RANGO: preselecciona TODAS las reservas liberables cuando llega la ocupación del
  // rango (opt-out). Se reejecuta al cambiar el rango (nuevos datos), no al deseleccionar.
  useEffect(() => {
    if (mode === 'range' && rangeQuery.data) {
      setSelected(allReleasableKeys(rangeQuery.data.days));
    }
  }, [rangeQuery.data, mode]);

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
          setBatchOpen(false);
        },
      },
    );
  }

  const emptyMessage =
    mode === 'range' ? t('releases.range.empty') : t('releases.employeeWeek.empty');

  return (
    <>
      <div className="release-mode-row">
        <ReleaseModeToggle mode={mode} onChange={setMode} />

        {mode === 'week' ? (
          <WeekNavigator
            rangeLabel={rangeLabel}
            onPrevious={() => shiftWeek(-WEEK_LENGTH)}
            onNext={() => shiftWeek(WEEK_LENGTH)}
            onToday={() => setWeekStart(mondayOfWeek())}
          />
        ) : (
          <ReleaseRangePicker
            from={from}
            to={to}
            onChange={(nextFrom, nextTo) => {
              setFrom(nextFrom);
              setTo(nextTo);
            }}
          />
        )}
      </div>

      {query.isLoading ? (
        <TableSkeleton label={t('releases.employeeWeek.loading')} columns={1} />
      ) : null}

      {query.isError ? (
        <TableError
          message={t('releases.employeeWeek.loadError')}
          retryLabel={t('common.retry')}
          onRetry={() => void query.refetch()}
        />
      ) : null}

      {query.isSuccess && daysWithReservations.length === 0 ? (
        <TableEmpty icon="calendar-off" message={emptyMessage} />
      ) : null}

      <ReleaseFeedbackBanner feedback={feedback} />

      {query.isSuccess && daysWithReservations.length > 0 ? (
        <ReleaseSelectionPanel
          days={daysWithReservations}
          selected={selected}
          language={i18n.language}
          batchOpen={batchOpen}
          reason={reason}
          error={error}
          pending={batch.isPending}
          onToggle={toggle}
          onOpenBatch={() => {
            setError(null);
            setBatchOpen(true);
          }}
          onCloseBatch={() => setBatchOpen(false)}
          onReasonChange={setReason}
          onConfirm={handleRelease}
        />
      ) : null}
    </>
  );
}

// Banner de resultado de la liberación (nulo si aún no hay resultado): recuento de
// liberadas y, en ámbar, las que fallaron (best-effort, no transaccional).
function ReleaseFeedbackBanner({ feedback }: { feedback: BatchReleaseResult | null }) {
  const { t } = useTranslation();
  if (!feedback) {
    return null;
  }
  const failed = feedback.failed > 0;
  return (
    <div className="release-feedback">
      <InfoBanner variant={failed ? 'amber' : 'green'} icon={failed ? 'alert-triangle' : 'circle-check'}>
        {feedbackMessage(feedback, t)}
      </InfoBanner>
    </div>
  );
}

interface ReleaseSelectionPanelProps {
  days: EmployeeWeekDay[];
  selected: Set<string>;
  language: string;
  batchOpen: boolean;
  reason: string;
  error: string | null;
  pending: boolean;
  onToggle: (key: string) => void;
  onOpenBatch: () => void;
  onCloseBatch: () => void;
  onReasonChange: (value: string) => void;
  onConfirm: () => void;
}

// Panel de selección + acción: barra "N seleccionadas / Liberar", lista de reservas
// por día con checkbox y el modal de motivo. Compartido por los modos semana y rango.
function ReleaseSelectionPanel({
  days,
  selected,
  language,
  batchOpen,
  reason,
  error,
  pending,
  onToggle,
  onOpenBatch,
  onCloseBatch,
  onReasonChange,
  onConfirm,
}: ReleaseSelectionPanelProps) {
  const { t } = useTranslation();
  // Días distintos cubiertos por la selección (la clave de reserva empieza por la fecha):
  // el contador muestra "N reservas · M días" para dar contexto del alcance del lote.
  const dayCount = useMemo(() => {
    const dates = new Set<string>();
    for (const key of selected) {
      dates.add(key.split('__')[0]);
    }
    return dates.size;
  }, [selected]);
  return (
    <>
      {selected.size > 0 ? (
        <div className="release-batch-bar">
          <span aria-live="polite">
            {t('releases.employeeWeek.selectedCount', { count: selected.size })}
            <span className="release-batch-days">
              {' · '}
              {t('releases.employeeWeek.selectedDays', { count: dayCount })}
            </span>
          </span>
          <Button variant="red" icon="arrow-back-up" onClick={onOpenBatch}>
            {t('releases.employeeWeek.releaseSelected', { count: selected.size })}
          </Button>
        </div>
      ) : null}

      <ReservationsPicker days={days} selected={selected} onToggle={onToggle} language={language} />

      <ReleaseReasonDialog
        open={batchOpen}
        count={selected.size}
        reason={reason}
        error={error}
        pending={pending}
        onReasonChange={onReasonChange}
        onClose={onCloseBatch}
        onConfirm={onConfirm}
      />
    </>
  );
}

// Conmutador Semana ⇄ Rango del flujo "Por empleado" (2ª fila, subordinado a las
// pestañas del hub): SEMANA navega semana a semana; RANGO libera un tramo de fechas.
function ReleaseModeToggle({
  mode,
  onChange,
}: {
  mode: ReleaseMode;
  onChange: (mode: ReleaseMode) => void;
}) {
  const { t } = useTranslation();
  const modes: { id: ReleaseMode; label: string; icon: string }[] = [
    { id: 'week', label: t('releases.employeeWeek.modeWeek'), icon: 'calendar-week' },
    { id: 'range', label: t('releases.employeeWeek.modeRange'), icon: 'calendar-stats' },
  ];
  return (
    <div className="release-mode-toggle" role="group" aria-label={t('releases.employeeWeek.modeLabel')}>
      {modes.map((item) => (
        <button
          key={item.id}
          type="button"
          className={`release-mode-btn${mode === item.id ? ' is-active' : ''}`}
          aria-pressed={mode === item.id}
          onClick={() => onChange(item.id)}
        >
          <i className={`ti ti-${item.icon}`} aria-hidden="true" />
          {item.label}
        </button>
      ))}
    </div>
  );
}

// Días máximos del rango de liberación (coincide con el tope del backend, 62).
const RANGE_MAX_DAYS = 62;
const RANGE_PRESETS: { key: string; days: number }[] = [
  { key: 'week1', days: 7 },
  { key: 'week2', days: 14 },
  { key: 'month1', days: 30 },
];

// Selector de rango Desde/Hasta para "Por empleado · Rango": inicio fijado a hoy (no
// se libera en el pasado), tope de 2 meses y presets rápidos (1/2 semanas, 1 mes).
function ReleaseRangePicker({
  from,
  to,
  onChange,
}: {
  from: string;
  to: string;
  onChange: (from: string, to: string) => void;
}) {
  const { t } = useTranslation();
  const today = todayIso();
  const maxTo = addDaysIso(from, RANGE_MAX_DAYS - 1);

  // Normaliza el par para respetar hoy≤from≤to y el tope de días.
  function commit(nextFrom: string, nextTo: string): void {
    const safeFrom = nextFrom < today ? today : nextFrom;
    const cap = addDaysIso(safeFrom, RANGE_MAX_DAYS - 1);
    let safeTo = nextTo < safeFrom ? safeFrom : nextTo;
    if (safeTo > cap) {
      safeTo = cap;
    }
    onChange(safeFrom, safeTo);
  }

  return (
    <div className="release-range-bar">
      <div className="release-range-fields">
        <div className="release-range-field">
          <span className="release-range-caption">{t('releases.range.from')}</span>
          <label className="release-date-control">
            <i className="ti ti-calendar" aria-hidden="true" />
            <input
              type="date"
              className="release-date-control-input"
              aria-label={t('releases.range.from')}
              value={from}
              min={today}
              onChange={(event) => commit(event.target.value, to)}
              onClick={(event) => event.currentTarget.showPicker?.()}
            />
          </label>
        </div>
        <i className="ti ti-arrow-narrow-right release-range-sep" aria-hidden="true" />
        <div className="release-range-field">
          <span className="release-range-caption">{t('releases.range.to')}</span>
          <label className="release-date-control">
            <i className="ti ti-calendar" aria-hidden="true" />
            <input
              type="date"
              className="release-date-control-input"
              aria-label={t('releases.range.to')}
              value={to}
              min={from}
              max={maxTo}
              onChange={(event) => commit(from, event.target.value)}
              onClick={(event) => event.currentTarget.showPicker?.()}
            />
          </label>
        </div>
      </div>
      <div className="release-range-presets">
        {RANGE_PRESETS.map((preset) => (
          <button
            key={preset.key}
            type="button"
            className="release-range-preset"
            onClick={() => commit(today, addDaysIso(today, preset.days - 1))}
          >
            {t(`releases.range.preset.${preset.key}`)}
          </button>
        ))}
      </div>
    </div>
  );
}

interface ReleaseReasonDialogProps {
  open: boolean;
  count: number;
  reason: string;
  error: string | null;
  pending: boolean;
  onReasonChange: (value: string) => void;
  onClose: () => void;
  onConfirm: () => void;
}

// Modal de motivo del lote (compartido por semana y rango): motivo obligatorio (5–500)
// y confirmación con spinner mientras libera. El recuento viaja en título y botón.
function ReleaseReasonDialog({
  open,
  count,
  reason,
  error,
  pending,
  onReasonChange,
  onClose,
  onConfirm,
}: ReleaseReasonDialogProps) {
  const { t } = useTranslation();
  return (
    <ConfirmDialog
      open={open}
      onOpenChange={(next) => {
        if (!next) {
          onClose();
        }
      }}
      tone="green"
      icon="arrow-back-up"
      title={t('releases.employeeWeek.batchTitle', { count })}
      description={
        <div className="release-batch-form">
          <label className="release-field-label" htmlFor="release-batch-reason">
            {t('releases.employeeWeek.reason')}
          </label>
          <textarea
            id="release-batch-reason"
            className="field-input"
            rows={3}
            value={reason}
            onChange={(event) => onReasonChange(event.target.value)}
          />
          <p className="hint">{t('releases.employeeWeek.reasonHint')}</p>
          {error ? (
            <p className="form-error" role="alert">
              {error}
            </p>
          ) : null}
        </div>
      }
      confirmLabel={t('releases.employeeWeek.releaseSelected', { count })}
      busy={pending || reason.trim().length < REASON_MIN}
      loading={pending}
      onConfirm={onConfirm}
    />
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

// Navegador de semana (rango visible + grupo ‹ Hoy ›): "Hoy" entre las dos flechas,
// mismo patrón que Ocupación. Alineado a la derecha de la fila del modo.
function WeekNavigator({ rangeLabel, onPrevious, onNext, onToday }: WeekNavigatorProps) {
  const { t } = useTranslation();
  return (
    <nav className="release-week-nav" aria-label={t('releases.admin.title')}>
      <span className="release-week-range" aria-live="polite">
        {rangeLabel}
      </span>
      <div className="release-week-nav-controls">
        <button
          type="button"
          className="release-week-nav-btn"
          aria-label={t('calendar.toolbar.previous')}
          onClick={onPrevious}
        >
          <i className="ti ti-chevron-left" aria-hidden="true" />
        </button>
        <button type="button" className="release-week-today" onClick={onToday}>
          {t('calendar.toolbar.today')}
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

