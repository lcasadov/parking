import { Fragment, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from '../components/Button';
import { CancelRequestModal } from '../components/CancelRequestModal';
import { PageHeader } from '../components/PageHeader';
import { PendingConfirmationBanner } from '../components/PendingConfirmationBanner';
import { ResourceTypePill } from '../components/ResourceTypePill';
import { TableEmpty, TableError, TableSkeleton } from '../components/TableStates';
import { WaitlistBadge } from '../components/WaitlistBadge';
import { useMyRequestsQuery } from '../hooks/useRequests';
import { useToast } from '../hooks/useToast';
import { canCancelRequest, toIsoDate, todayIso } from '../utils/requests';
import type { Request, RequestStatus, ResourceType } from '../types/request';

// Estados filtrables + "todas" (sin filtro).
const STATUS_FILTERS: (RequestStatus | 'ALL')[] = [
  'ALL',
  'PENDING',
  'APPROVED',
  'REJECTED',
  'CANCELLED',
];

// Filtro por recurso (plaza/puesto) + "todos".
const RESOURCE_FILTERS: (ResourceType | 'ALL')[] = ['ALL', 'PARKING', 'DESK'];

// Cubre un mes entero (≤31 días × 2 recursos) para poder ordenar de mayor a menor
// en cliente sin partir el mes entre páginas.
const PAGE_SIZE = 62;
// Numero de columnas de la tabla (fecha/estado/recurso/plaza-puesto/acciones):
// usado como colSpan de la fila del PendingConfirmationBanner.
const TABLE_COLUMNS = 5;

interface CancelTarget {
  id: number;
  requestedDate: string;
  // Estado y recurso para decidir el modo del modal (cancelar vs liberar) y el
  // toast correcto (solicitud cancelada vs plaza/puesto liberado).
  status: Request['status'];
  resourceType: ResourceType;
}

// Clave i18n del toast al soltar una solicitud, según su estado: APPROVED se
// "libera" (mensaje por recurso); PENDING se "cancela".
function releasedToastKey(resourceType: ResourceType): string {
  return resourceType === 'PARKING'
    ? 'calendar.myWeek.released.PARKING'
    : 'calendar.myWeek.released.DESK';
}

// ¿La solicitud fue LIBERADA (no cancelada)? Al cancelar, backend deja status
// CANCELLED tanto si venía de PENDING (cancelación) como de APPROVED (liberación
// del recurso). Solo la APPROVED estaba resuelta (`resolvedAt`), así que ese es el
// discriminante para mostrar "Liberada" en vez de "Cancelada".
function wasReleased(request: Request): boolean {
  return request.status === 'CANCELLED' && Boolean(request.resolvedAt);
}

// Etiqueta i18n y clase del badge de estado, tratando la liberación como estado
// propio ("Liberada") aunque comparta el CANCELLED del backend.
function statusLabelKey(request: Request): string {
  return wasReleased(request) ? 'requests.status.RELEASED' : `requests.status.${request.status}`;
}

function statusBadgeClass(request: Request): string {
  return wasReleased(request) ? 'status-released' : `status-${request.status.toLowerCase()}`;
}

// Vista EMPLOYEE: lista paginada de las solicitudes propias. La acción por fila
// depende del estado: PENDING → "Cancelar solicitud"; APPROVED (futura) → "Liberar"
// (libera el recurso). Sin exportar ni "Nueva solicitud" (el alta vive en el CTA
// global "Nueva reserva").
export function MyRequestsPage() {
  const { t, i18n } = useTranslation();
  const toast = useToast();
  const [page, setPage] = useState(0);
  // Mes visible (0 = mes actual; navegable adelante/atrás sin tope). Filtra por
  // fecha de recurso (requestedDate) vía from/to.
  const [monthOffset, setMonthOffset] = useState(0);
  const [statusFilter, setStatusFilter] = useState<RequestStatus | 'ALL'>('ALL');
  // Filtro por recurso (plaza/puesto). Se aplica en cliente: la página ya carga el
  // mes completo (PAGE_SIZE), así que no requiere una llamada extra.
  const [resourceFilter, setResourceFilter] = useState<ResourceType | 'ALL'>('ALL');
  const [cancelTarget, setCancelTarget] = useState<CancelTarget | null>(null);

  const monthStart = useMemo(() => {
    const now = new Date();
    return new Date(now.getFullYear(), now.getMonth() + monthOffset, 1);
  }, [monthOffset]);
  const from = toIsoDate(new Date(monthStart.getFullYear(), monthStart.getMonth(), 1));
  const to = toIsoDate(new Date(monthStart.getFullYear(), monthStart.getMonth() + 1, 0));
  const monthLabel = new Intl.DateTimeFormat(i18n.language, {
    month: 'long',
    year: 'numeric',
  }).format(monthStart);

  function goToMonth(delta: number): void {
    setMonthOffset((offset) => offset + delta);
    setPage(0);
  }

  const query = useMyRequestsQuery({
    page,
    size: PAGE_SIZE,
    from,
    to,
    status: statusFilter === 'ALL' ? undefined : statusFilter,
  });
  // Orden de MAYOR a MENOR por fecha de recurso: lo próximo/futuro arriba. Filtra
  // por recurso (plaza/puesto) en cliente.
  const requests = useMemo(
    () =>
      [...(query.data?.content ?? [])]
        .filter(
          (request) =>
            resourceFilter === 'ALL' || (request.resourceType ?? 'PARKING') === resourceFilter,
        )
        .sort((a, b) => b.requestedDate.localeCompare(a.requestedDate)),
    [query.data, resourceFilter],
  );
  const now = todayIso();
  const ready = !query.isLoading && !query.isError;
  const totalPages = query.data?.totalPages ?? 0;
  const isFirst = query.data?.first ?? true;
  const isLast = query.data?.last ?? true;

  // Muestra el NUMERO real del recurso asignado ("Plaza 3005" / "Puesto 12"), nunca el
  // parkingSpaceId (id interno de BD). El backend solo resuelve resourceNumber para las
  // solicitudes APPROVED con recurso; en el resto (PENDING/REJECTED/CANCELLED) es null -> "—".
  function spaceLabel(request: Request): string {
    if (typeof request.resourceNumber !== 'number') {
      return '—';
    }
    if (request.resourceType === 'DESK') {
      return t('requests.mine.resourceLabel.desk', { number: request.resourceNumber });
    }
    return typeof request.floor === 'number'
      ? t('requests.mine.resourceLabel.parkingWithFloor', {
          number: request.resourceNumber,
          floor: request.floor,
        })
      : t('requests.mine.resourceLabel.parking', { number: request.resourceNumber });
  }

  // Cierre de la acción: emite el toast según fuera liberar (APPROVED) o cancelar
  // (PENDING). La invalidación de cache (requests + calendar) la hace el hook de la
  // mutación, así que la lista se refresca sola.
  function handleResolved(): void {
    if (cancelTarget) {
      const isRelease = cancelTarget.status === 'APPROVED';
      toast.success(isRelease ? releasedToastKey(cancelTarget.resourceType) : 'requests.cancel.done');
    }
    setCancelTarget(null);
  }

  return (
    <section className="my-requests-page" aria-label={t('requests.mine.title')}>
      <PageHeader
        eyebrow={t('requests.mine.eyebrow')}
        title={t('requests.mine.title')}
        description={t('requests.mine.description')}
      />

      <div className="mr-month-nav">
        <button
          type="button"
          className="mw-week-nav-btn"
          aria-label={t('requests.mine.prevMonth')}
          onClick={() => goToMonth(-1)}
        >
          <i className="ti ti-chevron-left" aria-hidden="true" />
        </button>
        <span className="mr-month-label">{monthLabel}</span>
        <button
          type="button"
          className="mw-week-nav-btn"
          aria-label={t('requests.mine.nextMonth')}
          onClick={() => goToMonth(1)}
        >
          <i className="ti ti-chevron-right" aria-hidden="true" />
        </button>
        <div className="mr-filters">
          <label className="mr-status-filter">
            <span className="sr-only">{t('requests.mine.filterStatus')}</span>
            <select
              className="field-input"
              value={statusFilter}
              onChange={(event) => {
                setStatusFilter(event.target.value as RequestStatus | 'ALL');
                setPage(0);
              }}
            >
              {STATUS_FILTERS.map((value) => (
                <option key={value} value={value}>
                  {value === 'ALL' ? t('requests.mine.filterAll') : t(`requests.status.${value}`)}
                </option>
              ))}
            </select>
          </label>
          <label className="mr-status-filter">
            <span className="sr-only">{t('requests.mine.filterResource')}</span>
            <select
              className="field-input"
              value={resourceFilter}
              onChange={(event) => setResourceFilter(event.target.value as ResourceType | 'ALL')}
            >
              {RESOURCE_FILTERS.map((value) => (
                <option key={value} value={value}>
                  {value === 'ALL' ? t('requests.mine.filterAllResources') : t(`requests.resourceType.${value}`)}
                </option>
              ))}
            </select>
          </label>
        </div>
      </div>

      {query.isLoading ? <TableSkeleton label={t('common.loading')} columns={5} /> : null}

      {query.isError ? (
        <TableError
          message={t('requests.mine.loadError')}
          retryLabel={t('common.retry')}
          onRetry={() => void query.refetch()}
        />
      ) : null}

      {ready ? (
        requests.length === 0 ? (
          <TableEmpty icon="calendar-plus" message={t('requests.mine.emptyMonth')} />
        ) : (
          <div className="table-scroll my-requests-table">
            <table className="table">
              <thead>
                <tr className="table-header">
                  <th scope="col">{t('requests.mine.columns.date')}</th>
                  <th scope="col">{t('requests.mine.columns.status')}</th>
                  <th scope="col">{t('requests.mine.columns.resource')}</th>
                  <th scope="col">{t('requests.mine.columns.space')}</th>
                  <th scope="col">{t('requests.mine.columns.actions')}</th>
                </tr>
              </thead>
              <tbody>
                {requests.map((request) => {
                  const isRelease = request.status === 'APPROVED';
                  // Pasadas (fecha anterior a hoy): atenuadas y sin acciones; las que
                  // importan son las de hoy/futuras.
                  const isPast = request.requestedDate < now;
                  return (
                    <Fragment key={request.id}>
                      <tr className={`table-row${isPast ? ' is-past' : ''}`}>
                        <td className="mr-cell-date" data-label={t('requests.mine.columns.date')}>
                          <span className="mr-date-main">{request.requestedDate}</span>
                          <span className="mr-date-sub">
                            {t('requests.mine.requestedOn', { date: request.createdAt.slice(0, 10) })}
                          </span>
                        </td>
                        <td className="mr-cell-status" data-label={t('requests.mine.columns.status')}>
                          <span className={`status-badge ${statusBadgeClass(request)}`}>
                            {t(statusLabelKey(request))}
                          </span>
                          {request.status === 'PENDING' && request.waitlisted ? (
                            <WaitlistBadge />
                          ) : null}
                        </td>
                        <td
                          className="mr-cell-resource"
                          data-label={t('requests.mine.columns.resource')}
                          data-space={request.status === 'APPROVED' ? `· ${spaceLabel(request)}` : ''}
                        >
                          <ResourceTypePill resourceType={request.resourceType} />
                        </td>
                        <td className="mr-cell-space" data-label={t('requests.mine.columns.space')}>
                          {spaceLabel(request)}
                        </td>
                        <td className="table-actions mr-actions" data-label={t('requests.mine.columns.actions')}>
                          {!isPast && canCancelRequest(request) ? (
                            <Button
                              variant={isRelease ? 'white' : 'red'}
                              icon={isRelease ? 'arrow-back-up' : 'x'}
                              onClick={() =>
                                setCancelTarget({
                                  id: request.id,
                                  requestedDate: request.requestedDate,
                                  status: request.status,
                                  resourceType: request.resourceType ?? 'PARKING',
                                })
                              }
                            >
                              {isRelease ? t('requests.mine.release') : t('requests.mine.cancelRequest')}
                            </Button>
                          ) : null}
                        </td>
                      </tr>
                      {request.status === 'PENDING' ? (
                        <tr className="table-row-banner">
                          <td colSpan={TABLE_COLUMNS}>
                            <PendingConfirmationBanner
                              requestId={request.id}
                              createdAt={request.createdAt}
                              lastRemindedAt={request.lastRemindedAt}
                            />
                          </td>
                        </tr>
                      ) : null}
                    </Fragment>
                  );
                })}
              </tbody>
            </table>
          </div>
        )
      ) : null}

      {totalPages > 1 ? (
        <nav className="pagination" aria-label={t('requests.mine.title')}>
          <Button variant="white" disabled={isFirst} onClick={() => setPage((p) => p - 1)}>
            {t('requests.pagination.previous')}
          </Button>
          <span className="pagination-info">
            {t('requests.pagination.pageInfo', { page: page + 1, total: totalPages })}
          </span>
          <Button variant="white" disabled={isLast} onClick={() => setPage((p) => p + 1)}>
            {t('requests.pagination.next')}
          </Button>
        </nav>
      ) : null}

      {cancelTarget ? (
        <CancelRequestModal
          requestId={cancelTarget.id}
          requestedDate={cancelTarget.requestedDate}
          mode={cancelTarget.status === 'PENDING' ? 'cancel' : 'release'}
          onClose={() => setCancelTarget(null)}
          onCancelled={handleResolved}
        />
      ) : null}
    </section>
  );
}
