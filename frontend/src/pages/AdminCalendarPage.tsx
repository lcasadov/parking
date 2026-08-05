import { useMemo, useState } from 'react';
import type { TFunction } from 'i18next';
import { useTranslation } from 'react-i18next';
import { Button } from '../components/Button';
import { CalendarCellView } from '../components/CalendarCellView';
import { KpiStat } from '../components/KpiStat';
import { Spinner } from '../components/Spinner';
import { ViewInPlanTrigger } from '../components/ViewInPlanTrigger';
import {
  AdministrativeReleaseModal,
  type AdministrativeReleasePrefill,
} from '../components/AdministrativeReleaseModal';
import {
  AdminCancelRequestModal,
  type AdminCancelRequestPrefill,
} from '../components/AdminCancelRequestModal';
import { OccupancyAssignModal } from '../components/OccupancyAssignModal';
import {
  RequestManageModal,
  type RequestManagePrefill,
  type ReassignTarget,
  type SwapTarget,
} from '../components/RequestManageModal';
import { PageFrame } from '../components/PageFrame';
import { ResourceSelector } from '../components/ResourceSelector';
import { emitApiErrorToast } from '../api/events';
import { useAdminCalendarQuery } from '../hooks/useCalendar';
import { useWeekendReservableQuery } from '../hooks/useSettings';
import { useFloorPlanQuery } from '../hooks/useFloorPlan';
import { useToast } from '../hooks/useToast';
import { summarizeDay, type DaySnapshot, buildAdminCalendarCsv } from '../utils/adminCalendar';
import { triggerBlobDownload } from '../utils/download';
import type { CalendarCell, CalendarCellState, CalendarRow } from '../types/calendar';
import type { ResourceType } from '../types/request';
import type { FloorPlanDesk } from '../types/floorPlan';
import {
  addDaysIso,
  calendarStateKey,
  dayMonth,
  mondayOfWeek,
  weekdayIndex,
  weekRangeLabel,
} from '../utils/calendar';
import { isTodayOrFuture, todayIso } from '../utils/requests';

const WEEK_LENGTH = 7;

// Filtros rápidos SIEMPRE visibles sobre la rejilla (sustituyen al antiguo botón
// "Filtrar" + panel plegable). Segmento de selección única que agrupa los estados
// del contrato en categorías legibles. "free" replica lo que hacía la vista
// Disponibilidad (recursos con hueco). Orden de lectura (S1192: sin literales sueltos).
type QuickFilter = 'all' | 'free' | 'occupied' | 'released' | 'requests';

const QUICK_FILTERS: QuickFilter[] = ['all', 'free', 'occupied', 'released', 'requests'];

// Estados de CalendarCellState que cubre cada filtro rápido. `null` = todos (sin
// filtrar). "occupied" agrupa fija (ASSIGNED) y solicitud aprobada (ambas ocupan
// el día); "requests" agrupa las derivadas de una solicitud (pendiente/aprobada).
const QUICK_FILTER_STATES: Record<QuickFilter, CalendarCellState[] | null> = {
  all: null,
  free: ['FREE'],
  occupied: ['ASSIGNED', 'REQUEST_APPROVED', 'VISITOR_RESERVATION'],
  released: ['RELEASED'],
  requests: ['REQUEST_PENDING', 'REQUEST_APPROVED'],
};

// Punto de color por filtro (mapa filtro->color del design system, §4).
const QUICK_FILTER_DOT: Record<QuickFilter, string> = {
  all: 'var(--ink-faint)',
  free: 'var(--state-free-bg)',
  occupied: 'var(--state-occupied-bg)',
  released: 'var(--state-released-bg)',
  requests: 'var(--state-pending-bg)',
};

// ¿Encaja una celda (por su estado) en el filtro rápido activo? "all" siempre.
function cellMatchesQuickFilter(state: CalendarCellState, filter: QuickFilter): boolean {
  const states = QUICK_FILTER_STATES[filter];
  return states === null || states.includes(state);
}

// ¿Tiene la fila (recurso) al menos una celda que encaje en el filtro? Determina si
// el recurso sigue visible: "Solo libres" oculta los recursos sin ningún hueco.
function rowMatchesQuickFilter(row: CalendarRow, filter: QuickFilter): boolean {
  return filter === 'all' || row.cells.some((cell) => cellMatchesQuickFilter(cell.state, filter));
}

// Filtra las filas por el filtro rápido (filtrado REAL de recursos, no cosmético).
function filterCalendarRows(rows: CalendarRow[], filter: QuickFilter): CalendarRow[] {
  return filter === 'all' ? rows : rows.filter((row) => rowMatchesQuickFilter(row, filter));
}

// Nº de recursos que encaja en cada filtro (badge del chip: refuerza que el filtro
// es efectivo). Extraído a módulo para no cargar la complejidad del componente (S3776).
function countRowsByFilter(rows: CalendarRow[]): Record<QuickFilter, number> {
  const counts: Record<QuickFilter, number> = {
    all: rows.length,
    free: 0,
    occupied: 0,
    released: 0,
    requests: 0,
  };
  for (const row of rows) {
    for (const filter of QUICK_FILTERS) {
      if (filter !== 'all' && rowMatchesQuickFilter(row, filter)) {
        counts[filter] += 1;
      }
    }
  }
  return counts;
}

// Número de puesto (1-65) a partir de la etiqueta de la fila DESK ("D-05" -> 5),
// para resolver el puesto en el plano ("Ver en plano"). Null si no hay dígitos.
function deskNumberFromLabel(label: string): number | null {
  const match = /\d+/.exec(label);
  return match ? Number(match[0]) : null;
}

// Sábado/domingo. Se ocultan del calendario semanal cuando el admin no admite
// reservas en fin de semana.
function isWeekendIso(date: string): boolean {
  const day = weekdayIndex(date);
  return day === 0 || day === 6;
}

// Días visibles: todos, o solo laborables si no se admite reservar en finde.
function visibleDays(days: string[], weekendReservable: boolean): string[] {
  return weekendReservable ? days : days.filter((d) => !isWeekendIso(d));
}

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

// Candidatos de reasignación (recursos libres) e intercambio (otras reservas
// APPROVED) de una fecha, derivados de la propia rejilla para no pedir datos extra.
interface ManageTargets {
  reassign: ReassignTarget[];
  swap: SwapTarget[];
}

function cellForDate(row: CalendarRow, date: string): CalendarCell | undefined {
  return row.cells.find((cell) => cell.date === date);
}

function deriveManageTargets(
  rows: CalendarRow[],
  date: string,
  excludeRequestId: number,
): ManageTargets {
  const reassign: ReassignTarget[] = [];
  const swap: SwapTarget[] = [];
  for (const row of rows) {
    const cell = cellForDate(row, date);
    if (!cell) {
      continue;
    }
    if (cell.state === 'FREE') {
      reassign.push({ resourceId: row.parkingSpaceId, label: row.label });
    } else if (
      cell.state === 'REQUEST_APPROVED' &&
      typeof cell.requestId === 'number' &&
      cell.requestId !== excludeRequestId
    ) {
      swap.push({
        requestId: cell.requestId,
        resourceLabel: row.label,
        employeeName: cell.employeeName ?? '',
      });
    }
  }
  return { reassign, swap };
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
  const [quickFilter, setQuickFilter] = useState<QuickFilter>('all');
  const [assignTarget, setAssignTarget] = useState<AssignTarget | null>(null);
  const [releasePrefill, setReleasePrefill] = useState<AdministrativeReleasePrefill | null>(null);
  const [cancelPrefill, setCancelPrefill] = useState<AdminCancelRequestPrefill | null>(null);
  const [managePrefill, setManagePrefill] = useState<RequestManagePrefill | null>(null);

  const query = useAdminCalendarQuery(weekStart, resourceType);
  const weekendReservable = useWeekendReservableQuery().data ?? false;
  const days = useMemo(
    () => visibleDays(query.data?.days ?? [], weekendReservable),
    [query.data, weekendReservable],
  );
  const daySet = useMemo(() => new Set(days), [days]);
  const rows = useMemo(() => query.data?.rows ?? [], [query.data]);
  const today = todayIso();

  // Plano de puestos para HOY (solo en modo DESK): alimenta el tooltip mini-plano y
  // el modal de "Ver en plano". React Query deduplica la petición aunque haya 65
  // filas. El mapa nº de puesto -> puesto resuelve cada fila por su etiqueta.
  const floorPlanQuery = useFloorPlanQuery(today, resourceType === 'DESK');
  const desksByNumber = useMemo(() => {
    const map = new Map<number, FloorPlanDesk>();
    for (const desk of floorPlanQuery.data?.desks ?? []) {
      map.set(desk.deskNumber, desk);
    }
    return map;
  }, [floorPlanQuery.data]);

  // KPIs del modo activo, referidos a HOY (todo derivado de las filas ya cargadas,
  // sin fetch). La seleccion de columna y los ratios viven en helpers de modulo
  // para mantener baja la complejidad del componente (Sonar S3776).
  const kpi = useMemo(() => deriveKpiView(rows, days, today), [rows, days, today]);
  const dayTag = formatKpiDay(kpi, t);

  // Filas visibles según el filtro rápido y contadores por filtro (derivados de las
  // filas ya cargadas, sin fetch). El filtrado es real: oculta recursos y atenúa
  // celdas que no cumplen (más abajo).
  const filteredRows = useMemo(() => filterCalendarRows(rows, quickFilter), [rows, quickFilter]);
  const manageTargets = useMemo<ManageTargets | null>(
    () =>
      managePrefill
        ? deriveManageTargets(rows, managePrefill.date, managePrefill.requestId)
        : null,
    [rows, managePrefill],
  );
  const filterCounts = useMemo(() => countRowsByFilter(rows), [rows]);

  const rangeLabel =
    days.length > 0 ? weekRangeLabel(days[0], days[days.length - 1], i18n.language) : '';

  function goPrevious(): void {
    setWeekStart((current) => addDaysIso(current, -WEEK_LENGTH));
  }

  function goNext(): void {
    setWeekStart((current) => addDaysIso(current, WEEK_LENGTH));
  }

  function goToday(): void {
    setWeekStart(mondayOfWeek());
  }

  // Resuelve el puesto del plano para una fila DESK a partir de su etiqueta ("D-05"
  // -> puesto nº 5). Null mientras el plano carga o si la etiqueta no trae número.
  function deskForRow(label: string): FloorPlanDesk | null {
    const deskNumber = deskNumberFromLabel(label);
    return deskNumber !== null ? (desksByNumber.get(deskNumber) ?? null) : null;
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
      // Celda de reserva APPROVED → modal de gestión (reasignar / intercambiar /
      // liberar), en lugar de saltar directo a la cancelación.
      setManagePrefill({
        requestId: cell.requestId,
        employeeName: cell.employeeName ?? '',
        resourceLabel: row.label,
        date: cell.date,
        resourceType,
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

  // Éxito de reasignación/intercambio desde el modal de gestión.
  function handleManaged(messageKey: string): void {
    setManagePrefill(null);
    refresh();
    toast.success(messageKey);
  }

  // "Liberar" desde el modal de gestión: cierra gestión y abre el modal de
  // cancelación con motivo (reutiliza el flujo existente) sobre la misma reserva.
  function handleManageCancel(): void {
    if (!managePrefill) {
      return;
    }
    setCancelPrefill({
      requestId: managePrefill.requestId,
      employeeName: managePrefill.employeeName,
      resourceLabel: managePrefill.resourceLabel,
      releaseDate: managePrefill.date,
    });
    setManagePrefill(null);
  }

  const showGrid = !query.isLoading && !query.isError;

  return (
    <PageFrame
      eyebrow={t('occupancy.title')}
      title={t(`occupancy.weekly.modeTitle.${resourceType}`)}
      titleId="admin-calendar-title"
      titleAriaLive="polite"
      bodyLabel={t('calendar.admin.title')}
      actions={
        <Button variant="green" icon="download" disabled={rows.length === 0} onClick={handleExport}>
          {t('calendar.actions.export')}
        </Button>
      }
      resourceSelector={
        <ResourceSelector
          size="lg"
          value={resourceType}
          onChange={(value) => setResourceType(value as ResourceType)}
          labels={{
            parking: t('occupancy.weekly.resourceType.PARKING'),
            desk: t('occupancy.weekly.resourceType.DESK'),
          }}
          ariaLabel={t('occupancy.weekly.modeSwitchLabel')}
        />
      }
      toolbar={
        <div className="occ-kpi-strip" role="group" aria-label={t('occupancy.title')}>
          <KpiStat
            dot="var(--ink-faint)"
            value={kpi.snapshot.total}
            label={t(`occupancy.weekly.kpi.totalLabel.${resourceType}`)}
            sub={t('occupancy.weekly.kpi.inInventory')}
          />
          <KpiStat
            dot="var(--accent)"
            value={kpi.snapshot.occupied}
            label={t('occupancy.weekly.kpi.occupied', { day: dayTag })}
            sub={t('occupancy.weekly.kpi.ofOccupancy', { pct: kpi.occupancyPct })}
          />
          <KpiStat
            dot="var(--info)"
            value={kpi.snapshot.free}
            label={t('occupancy.weekly.kpi.free', { day: dayTag })}
            sub={t('occupancy.weekly.kpi.availableNow')}
          />
          <KpiStat
            dot="var(--rel)"
            value={kpi.snapshot.released}
            label={t('occupancy.weekly.kpi.released', { day: dayTag })}
            sub={t('occupancy.weekly.kpi.releasedSub')}
          />
        </div>
      }
      subbar={
        <nav className="week-nav" aria-label={t('calendar.admin.title')}>
          <div className="week-nav-controls">
            <Button
              variant="white"
              className="btn-icon-only"
              icon="chevron-left"
              aria-label={t('calendar.toolbar.previous')}
              onClick={goPrevious}
            />
            <Button variant="white" onClick={goToday}>
              {t('calendar.toolbar.today')}
            </Button>
            <Button
              variant="white"
              className="btn-icon-only"
              icon="chevron-right"
              aria-label={t('calendar.toolbar.next')}
              onClick={goNext}
            />
          </div>
          <div className="week-nav-range">
            <span className="week-range" aria-live="polite">
              {rangeLabel || t('calendar.toolbar.weekOf', { date: weekStart })}
            </span>
          </div>
          {/* Filtros rápidos donde antes iba la leyenda (ahorra una fila): segmento de
              selección única que filtra la rejilla al instante (oculta recursos + atenúa
              celdas que no cumplen). "Solo libres" replica la antigua vista Disponibilidad. */}
          <div
            className="chip-filters occ-quick-filters"
            role="group"
            aria-label={t('occupancy.weekly.filterLabel')}
          >
            {QUICK_FILTERS.map((filter) => {
              const isActive = quickFilter === filter;
              return (
                <button
                  key={filter}
                  type="button"
                  className={`chip-filter${isActive ? ' is-active' : ''}`}
                  aria-pressed={isActive}
                  onClick={() => setQuickFilter(filter)}
                >
                  <span
                    className="cf-dot"
                    style={{ background: QUICK_FILTER_DOT[filter] }}
                    aria-hidden="true"
                  />
                  {t(`occupancy.weekly.filters.${filter}`)}
                  <span className="cf-count">{filterCounts[filter]}</span>
                </button>
              );
            })}
          </div>
        </nav>
      }
    >
      {query.isLoading ? (
        <div className="pf-state">
          <Spinner />
        </div>
      ) : null}

      {query.isError ? (
        <div className="pf-state">
          <p className="form-error" role="alert">
            {t('calendar.loadError')}
          </p>
        </div>
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
              ) : filteredRows.length === 0 ? (
                <tr>
                  <td colSpan={days.length + 1} className="table-empty">
                    {t('occupancy.weekly.noMatches')}
                  </td>
                </tr>
              ) : (
                filteredRows.map((row) => (
                  <tr key={row.parkingSpaceId} className="table-row">
                    <th scope="row" className="calendar-space-cell">
                      <span className="calendar-space-label">{row.label}</span>
                      {resourceType === 'DESK' ? (
                        <ViewInPlanTrigger
                          desk={deskForRow(row.label)}
                          deskLabel={row.label}
                          date={today}
                        />
                      ) : null}
                    </th>
                    {row.cells
                      .filter((cell) => daySet.has(cell.date))
                      .map((cell) => {
                      const actionable = isCellActionable(cell);
                      return (
                        <CalendarCellView
                          key={cell.date}
                          cell={cell}
                          isToday={cell.date === today}
                          dimmed={
                            quickFilter !== 'all' && !cellMatchesQuickFilter(cell.state, quickFilter)
                          }
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

      {managePrefill && manageTargets ? (
        <RequestManageModal
          prefill={managePrefill}
          reassignTargets={manageTargets.reassign}
          swapTargets={manageTargets.swap}
          onDone={handleManaged}
          onRequestCancel={handleManageCancel}
          onClose={() => setManagePrefill(null)}
        />
      ) : null}

      {cancelPrefill ? (
        <AdminCancelRequestModal
          prefill={cancelPrefill}
          onClose={() => setCancelPrefill(null)}
          onCancelled={handleCancelled}
        />
      ) : null}
    </PageFrame>
  );
}

