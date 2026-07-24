import { useMemo, useState } from 'react';
import type { TFunction } from 'i18next';
import { useTranslation } from 'react-i18next';
import { Button } from '../components/Button';
import { CalendarCellView } from '../components/CalendarCellView';
import { Legend } from '../components/Legend';
import { Spinner } from '../components/Spinner';
import {
  AdministrativeReleaseModal,
  type AdministrativeReleasePrefill,
} from '../components/AdministrativeReleaseModal';
import {
  AdminCancelRequestModal,
  type AdminCancelRequestPrefill,
} from '../components/AdminCancelRequestModal';
import { OccupancyAssignModal } from '../components/OccupancyAssignModal';
import { ResourceModeSwitch } from '../components/ResourceModeSwitch';
import { emitApiErrorToast } from '../api/events';
import { useAdminCalendarQuery } from '../hooks/useCalendar';
import { useToast } from '../hooks/useToast';
import { adminCalendarLegend } from '../utils/calendarLegend';
import { summarizeDay, type DaySnapshot, buildAdminCalendarCsv } from '../utils/adminCalendar';
import { triggerBlobDownload } from '../utils/download';
import type { CalendarCell, CalendarCellState, CalendarRow } from '../types/calendar';
import type { ResourceType } from '../types/request';
import {
  addDaysIso,
  calendarStateKey,
  dayMonth,
  isoWeekNumber,
  mondayOfWeek,
  weekdayIndex,
  weekRangeLabel,
} from '../utils/calendar';
import { isTodayOrFuture, todayIso } from '../utils/requests';

const WEEK_LENGTH = 7;

// Estados del calendario en orden de lectura (S1192: sin literales repetidos).
const ALL_STATES: CalendarCellState[] = [
  'ASSIGNED',
  'RELEASED',
  'REQUEST_PENDING',
  'REQUEST_APPROVED',
  'FREE',
];

// Punto de color por estado para los chips del filtro (mapa estado->color, §4).
const STATE_DOT: Record<CalendarCellState, string> = {
  ASSIGNED: 'var(--state-occupied-bg)',
  RELEASED: 'var(--state-released-bg)',
  REQUEST_PENDING: 'var(--state-pending-bg)',
  REQUEST_APPROVED: 'var(--state-request-bg)',
  FREE: 'var(--state-free-bg)',
};

// Accion inline resoluble desde una celda (weekly-assignment spec, celdas
// accionables): asignar (celda FREE) o liberar (ASSIGNED = fija, REQUEST_APPROVED
// = solicitud). El resto de estados no admiten accion en contexto.
type CellActionKind = 'ASSIGN' | 'RELEASE_FIXED' | 'CANCEL_REQUEST';

function cellActionKind(cell: CalendarCell): CellActionKind | null {
  if (cell.state === 'FREE') {
    return 'ASSIGN';
  }
  if (cell.state === 'ASSIGNED') {
    return 'RELEASE_FIXED';
  }
  if (cell.state === 'REQUEST_APPROVED') {
    return 'CANCEL_REQUEST';
  }
  return null;
}

// Estado de las tres acciones inline (asignar / liberar fija / cancelar solicitud).
interface AssignTarget {
  resourceId: number;
  resourceLabel: string;
  date: string;
}

// Vista de KPIs del modo activo (referidos a hoy). La columna es HOY si la semana
// mostrada la contiene; si el admin navego a otra semana, se cae al primer dia
// visible para que los numeros sigan siendo reales (la etiqueta pasa de "hoy" a la
// fecha). Extraido a modulo para no cargar la complejidad del componente (S3776).
interface KpiView {
  snapshot: DaySnapshot;
  occupancyPct: number;
  isToday: boolean;
  date: string;
}

function deriveKpiView(rows: CalendarRow[], days: string[], today: string): KpiView {
  const date = days.includes(today) ? today : (days[0] ?? today);
  const snapshot = summarizeDay(rows, date);
  const occupancyPct =
    snapshot.total > 0 ? Math.round((snapshot.occupied / snapshot.total) * 100) : 0;
  return { snapshot, occupancyPct, isToday: date === today, date };
}

// Etiqueta del dia de los KPIs: "hoy" cuando la columna es hoy; si no, "Lun 11 may".
function formatKpiDay(kpi: KpiView, t: TFunction): string {
  if (kpi.isToday) {
    return t('occupancy.weekly.kpi.today');
  }
  return `${t(`calendar.weekdaysShort.${weekdayIndex(kpi.date)}`)} ${dayMonth(kpi.date)}`;
}

// Rejilla de calendario semanal ADMIN (consume GET /calendar/admin). Presentacion
// ALEATICA + celdas ACCIONABLES (weekly-assignment spec, restructure-admin-workflows):
// una celda libre inicia la asignacion (fija o puntual) en contexto; una celda
// ocupada inicia la liberacion (administrativa o admin-cancel segun el origen). El
// conmutador plaza/puesto pasa `resourceType` a GET /calendar/admin (design §D4).
export function AdminCalendarPage() {
  const { t, i18n } = useTranslation();
  const toast = useToast();
  const [weekStart, setWeekStart] = useState<string>(mondayOfWeek());
  const [resourceType, setResourceType] = useState<ResourceType>('PARKING');
  const [isFilterOpen, setIsFilterOpen] = useState(false);
  const [activeStates, setActiveStates] = useState<Set<CalendarCellState>>(
    () => new Set(ALL_STATES),
  );
  const [assignTarget, setAssignTarget] = useState<AssignTarget | null>(null);
  const [releasePrefill, setReleasePrefill] = useState<AdministrativeReleasePrefill | null>(null);
  const [cancelPrefill, setCancelPrefill] = useState<AdminCancelRequestPrefill | null>(null);

  const query = useAdminCalendarQuery(weekStart, resourceType);
  const days = useMemo(() => query.data?.days ?? [], [query.data]);
  const rows = useMemo(() => query.data?.rows ?? [], [query.data]);
  const today = todayIso();

  // KPIs del modo activo, referidos a HOY (todo derivado de las filas ya cargadas,
  // sin fetch). La seleccion de columna y los ratios viven en helpers de modulo
  // para mantener baja la complejidad del componente (Sonar S3776).
  const kpi = useMemo(() => deriveKpiView(rows, days, today), [rows, days, today]);
  const dayTag = formatKpiDay(kpi, t);

  const rangeLabel =
    days.length > 0 ? weekRangeLabel(days[0], days[days.length - 1], i18n.language) : '';
  const weekNumber = isoWeekNumber(weekStart);

  function goPrevious(): void {
    setWeekStart((current) => addDaysIso(current, -WEEK_LENGTH));
  }

  function goNext(): void {
    setWeekStart((current) => addDaysIso(current, WEEK_LENGTH));
  }

  function goToday(): void {
    setWeekStart(mondayOfWeek());
  }

  function toggleState(state: CalendarCellState): void {
    setActiveStates((current) => {
      const next = new Set(current);
      if (next.has(state)) {
        next.delete(state);
      } else {
        next.add(state);
      }
      return next;
    });
  }

  function handleExport(): void {
    if (!query.data) {
      return;
    }
    const csv = buildAdminCalendarCsv(
      query.data,
      t(`occupancy.weekly.resourceColumn.${resourceType}`),
      (iso) => `${t(`calendar.weekdaysShort.${weekdayIndex(iso)}`)} ${dayMonth(iso)}`,
      (state) => t(calendarStateKey(state)),
    );
    // BOM UTF-8 para que Excel detecte la codificacion en el CSV.
    const blob = new Blob([`\uFEFF${csv}`], { type: 'text/csv;charset=utf-8' });
    triggerBlobDownload(blob, `${t('calendar.export.filename')}-${weekStart}.csv`);
  }

  // Abre el modal correspondiente a la accion de la celda (asignar / liberar).
  function openCellAction(row: CalendarRow, cell: CalendarCell): void {
    const kind = cellActionKind(cell);
    if (kind === 'ASSIGN') {
      setAssignTarget({
        resourceId: row.parkingSpaceId,
        resourceLabel: row.label,
        date: cell.date,
      });
    } else if (kind === 'RELEASE_FIXED') {
      setReleasePrefill({
        employeeId: cell.employeeId ?? 0,
        employeeName: cell.employeeName ?? '',
        parkingSpaceId: row.parkingSpaceId,
        resourceLabel: row.label,
        releaseDate: cell.date,
        resourceType,
      });
    } else if (kind === 'CANCEL_REQUEST' && typeof cell.requestId === 'number') {
      setCancelPrefill({
        requestId: cell.requestId,
        employeeName: cell.employeeName ?? '',
        resourceLabel: row.label,
        releaseDate: cell.date,
      });
    }
  }

  // Una celda es accionable si su estado admite accion inline y la fecha es hoy o
  // futura (no se asignan ni liberan fechas pasadas; el backend las rechaza).
  function isCellActionable(cell: CalendarCell): boolean {
    return cellActionKind(cell) !== null && isTodayOrFuture(cell.date);
  }

  function refresh(): void {
    void query.refetch();
  }

  function handleAssigned(): void {
    setAssignTarget(null);
    refresh();
    toast.success('occupancy.assign.done');
  }

  function handleReleased(): void {
    setReleasePrefill(null);
    refresh();
    emitApiErrorToast('releases.admin.created');
  }

  function handleCancelled(): void {
    setCancelPrefill(null);
    refresh();
    emitApiErrorToast('requests.adminCancel.cancelled');
  }

  const showGrid = !query.isLoading && !query.isError;

  return (
    <section className="admin-calendar-page occ-weekly" aria-labelledby="admin-calendar-title">
      {/* Hero MODO-EXPLÍCITO: el titulo grande dice qué se está viendo (Plazas de
          parking / Puestos de oficina) y cambia con el conmutador. */}
      <header className="occ-hero">
        <div className="occ-hero-text">
          <span className="page-eyebrow">{t('occupancy.title')}</span>
          <h1 id="admin-calendar-title" className="occ-hero-title" aria-live="polite">
            {t(`occupancy.weekly.modeTitle.${resourceType}`)}
          </h1>
          <p className="occ-hero-sub">{t('occupancy.weekly.actionableHint')}</p>
        </div>
        <div className="occ-hero-actions">
          <Button
            variant="white"
            icon="filter"
            aria-pressed={isFilterOpen}
            onClick={() => setIsFilterOpen((open) => !open)}
          >
            {t('calendar.actions.filter')}
          </Button>
          <Button
            variant="green"
            icon="download"
            disabled={rows.length === 0}
            onClick={handleExport}
          >
            {t('calendar.actions.export')}
          </Button>
        </div>
      </header>

      {/* Selector GRANDE plaza/puesto: imposible de ignorar. Al cambiarlo cambian
          titulo, KPIs y rejilla. */}
      <ResourceModeSwitch
        value={resourceType}
        onChange={setResourceType}
        ariaLabel={t('occupancy.weekly.modeSwitchLabel')}
      />

      {/* KPIs del modo activo, referidos a HOY (datos reales de la rejilla). */}
      <div className="occ-kpis">
        <KpiCard
          dot="var(--ink-faint)"
          label={t(`occupancy.weekly.kpi.totalLabel.${resourceType}`)}
          value={kpi.snapshot.total}
          sub={t('occupancy.weekly.kpi.inInventory')}
        />
        <KpiCard
          dot="var(--accent)"
          label={t('occupancy.weekly.kpi.occupied', { day: dayTag })}
          value={kpi.snapshot.occupied}
          sub={t('occupancy.weekly.kpi.ofOccupancy', { pct: kpi.occupancyPct })}
        />
        <KpiCard
          dot="var(--info)"
          label={t('occupancy.weekly.kpi.free', { day: dayTag })}
          value={kpi.snapshot.free}
          sub={t('occupancy.weekly.kpi.availableNow')}
        />
        <KpiCard
          dot="var(--rel)"
          label={t('occupancy.weekly.kpi.released', { day: dayTag })}
          value={kpi.snapshot.released}
          sub={t('occupancy.weekly.kpi.releasedSub')}
        />
      </div>

      <nav className="week-nav" aria-label={t('calendar.admin.title')}>
        <div className="week-nav-controls">
          <Button
            variant="white"
            className="btn-icon-only"
            icon="chevron-left"
            aria-label={t('calendar.toolbar.previous')}
            onClick={goPrevious}
          />
          <Button
            variant="white"
            className="btn-icon-only"
            icon="chevron-right"
            aria-label={t('calendar.toolbar.next')}
            onClick={goNext}
          />
          <Button variant="white" onClick={goToday}>
            {t('calendar.toolbar.today')}
          </Button>
        </div>
        <div className="week-nav-range">
          <span className="week-range" aria-live="polite">
            {rangeLabel || t('calendar.toolbar.weekOf', { date: weekStart })}
          </span>
          <span className="week-number">{t('calendar.weekNav.week', { number: weekNumber })}</span>
        </div>
        <Legend items={adminCalendarLegend(t)} />
      </nav>

      {isFilterOpen ? (
        <div
          className="chip-filters calendar-state-filter"
          role="group"
          aria-label={t('calendar.actions.filter')}
        >
          {ALL_STATES.map((state) => {
            const isActive = activeStates.has(state);
            return (
              <button
                key={state}
                type="button"
                className={`chip-filter${isActive ? ' is-active' : ''}`}
                aria-pressed={isActive}
                onClick={() => toggleState(state)}
              >
                <span
                  className="cf-dot"
                  style={{ background: STATE_DOT[state] }}
                  aria-hidden="true"
                />
                {t(calendarStateKey(state))}
              </button>
            );
          })}
        </div>
      ) : null}

      {query.isLoading ? <Spinner /> : null}

      {query.isError ? (
        <p className="form-error" role="alert">
          {t('calendar.loadError')}
        </p>
      ) : null}

      {showGrid ? (
        <div className="table-scroll calendar-grid-scroll">
          <table className="table calendar-grid">
            <caption className="sr-only">{t('calendar.admin.title')}</caption>
            <thead>
              <tr className="table-header">
                <th scope="col" className="calendar-resource-head">
                  {t(`occupancy.weekly.resourceColumn.${resourceType}`)}
                </th>
                {days.map((date) => (
                  <th key={date} scope="col" className={date === today ? 'is-today' : undefined}>
                    <span className="day-head-abbr">
                      {t(`calendar.weekdaysShort.${weekdayIndex(date)}`)}
                    </span>
                    <span className="day-head-date">{dayMonth(date)}</span>
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {rows.length === 0 ? (
                <tr>
                  <td colSpan={days.length + 1} className="table-empty">
                    {t('calendar.empty')}
                  </td>
                </tr>
              ) : (
                rows.map((row) => (
                  <tr key={row.parkingSpaceId} className="table-row">
                    <th scope="row" className="calendar-space-cell">
                      <span className="calendar-space-label">{row.label}</span>
                      <span className="calendar-space-zone" aria-hidden="true">
                        #{row.parkingSpaceId}
                      </span>
                    </th>
                    {row.cells.map((cell) => {
                      const actionable = isCellActionable(cell);
                      return (
                        <CalendarCellView
                          key={cell.date}
                          cell={cell}
                          isToday={cell.date === today}
                          dimmed={!activeStates.has(cell.state)}
                          onActivate={actionable ? () => openCellAction(row, cell) : undefined}
                          actionLabel={
                            actionable
                              ? t(`occupancy.weekly.cellAction.${cellActionKind(cell)}`, {
                                  resource: row.label,
                                })
                              : undefined
                          }
                        />
                      );
                    })}
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      ) : null}

      {assignTarget ? (
        <OccupancyAssignModal
          resourceId={assignTarget.resourceId}
          resourceLabel={assignTarget.resourceLabel}
          resourceType={resourceType}
          date={assignTarget.date}
          onClose={() => setAssignTarget(null)}
          onAssigned={handleAssigned}
        />
      ) : null}

      {releasePrefill ? (
        <AdministrativeReleaseModal
          prefill={releasePrefill}
          onClose={() => setReleasePrefill(null)}
          onCreated={handleReleased}
        />
      ) : null}

      {cancelPrefill ? (
        <AdminCancelRequestModal
          prefill={cancelPrefill}
          onClose={() => setCancelPrefill(null)}
          onCancelled={handleCancelled}
        />
      ) : null}
    </section>
  );
}

// Tarjeta KPI (mismo lenguaje que el Dashboard: punto de color + etiqueta +
// numeral Geist Mono tabular + sublinea). El color solo tiñe el punto; el
// numeral queda en tinta para legibilidad en claro y oscuro.
function KpiCard({
  dot,
  label,
  value,
  sub,
}: {
  dot: string;
  label: string;
  value: number;
  sub: string;
}) {
  return (
    <div className="card occ-kpi">
      <div className="occ-kpi-lbl">
        <span className="occ-kpi-dot" style={{ background: dot }} />
        {label}
      </div>
      <div className="occ-kpi-val mono">{value}</div>
      <div className="occ-kpi-sub">{sub}</div>
    </div>
  );
}
