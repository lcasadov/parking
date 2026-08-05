import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { ApproveRequestModal } from '../components/ApproveRequestModal';
import { Avatar } from '../components/Avatar';
import { Button } from '../components/Button';
import { ExportMenu } from '../components/ExportMenu';
import { PageFrame } from '../components/PageFrame';
import { RejectRequestModal } from '../components/RejectRequestModal';
import { ResourceSelector } from '../components/ResourceSelector';
import { ResourceTypePill } from '../components/ResourceTypePill';
import { SortableTh } from '../components/SortableTh';
import { Spinner } from '../components/Spinner';
import { EXPORT_PATHS } from '../api/exportApi';
import { useEmployeesQuery } from '../hooks/useEmployees';
import { usePendingRequestsQuery, useRequestsByStatusQuery } from '../hooks/useRequests';
import { useTableSort, type SortState } from '../hooks/useTableSort';
import { weekdayIndex } from '../utils/calendar';
import { initialsOf } from '../utils/initials';
import type { Employee } from '../types/employee';
import type { Request, RequestStatus, ResourceType } from '../types/request';

const PAGE_SIZE = 20;
const LOOKUP_SIZE = 100;

// Pestañas de la bandeja. La de pendientes usa el listado FIFO (ADMIN); el resto
// (aprobadas / rechazadas / todas) usa el listado admin por estado (GET /requests).
type RequestTab = 'pending' | 'approved' | 'rejected' | 'all';
const PENDING_TAB: RequestTab = 'pending';
const TAB_ORDER: RequestTab[] = ['pending', 'approved', 'rejected', 'all'];

// Punto de color por estado (mismo lenguaje visual que los filtros rápidos de Ocupación).
const STATUS_DOT: Record<RequestTab, string> = {
  pending: 'var(--pend)',
  approved: 'var(--accent)',
  rejected: 'var(--red)',
  all: 'var(--ink-faint)',
};

// Estado por el que filtra cada pestaña en el listado por estado. 'all' no filtra
// (status undefined = todas); 'pending' no usa este listado (tiene su cola FIFO).
const STATUS_BY_TAB: Record<RequestTab, RequestStatus | undefined> = {
  pending: undefined,
  approved: 'APPROVED',
  rejected: 'REJECTED',
  all: undefined,
};

// Construye el mapa id -> empleado para resolver nombre y departamento.
function buildEmployeeMap(employees: Employee[]): Map<number, Employee> {
  const map = new Map<number, Employee>();
  for (const employee of employees) {
    map.set(employee.id, employee);
  }
  return map;
}

// Coincidencia de búsqueda (nombre completo o departamento, case-insensitive).
function matchesSearch(request: Request, employee: Employee | undefined, term: string): boolean {
  if (term === '') {
    return true;
  }
  const haystack = employee
    ? `${employee.firstName} ${employee.lastName} ${employee.department ?? ''}`
    : `#${request.employeeId}`;
  return haystack.toLowerCase().includes(term.toLowerCase());
}

// Celda de empleado (contrato §Solicitudes): avatar de color + nombre (elipsis) +
// sub-linea con el tipo de recurso (pill) y el departamento (elipsis). El tipo de
// recurso se integra aqui porque la tabla ya no tiene columna "Recurso" propia.
function EmployeeCell({ request, employee }: { request: Request; employee?: Employee }) {
  const { t } = useTranslation();
  const name = employee
    ? `${employee.firstName} ${employee.lastName}`
    : `#${request.employeeId}`;
  const initials = employee ? initialsOf(employee) : name.slice(0, 2).toUpperCase();
  return (
    <div className="employee-cell">
      <Avatar size="sm" initials={initials} label={name} seed={name} />
      <div className="employee-cell-text">
        <div className="employee-cell-name">{name}</div>
        <div className="employee-cell-sub">
          <ResourceTypePill resourceType={request.resourceType} />
          <span className="employee-cell-dept">
            {employee?.department ?? t('requests.approve.context.noDepartment')}
          </span>
        </div>
      </div>
    </div>
  );
}

interface RowActionsProps {
  request: Request;
  onApprove: (id: number) => void;
  onReject: (id: number) => void;
  onCancel: (id: number) => void;
}

// Acciones por fila según el estado: una PENDING se aprueba/rechaza; una APPROVED se
// cancela (libera el recurso vía reject en el backend); los estados terminales no ofrecen
// acciones (— para mantener la celda alineada).
function RowActions({ request, onApprove, onReject, onCancel }: RowActionsProps) {
  const { t } = useTranslation();
  if (request.status === 'PENDING') {
    return (
      <>
        <Button variant="green" icon="check" onClick={() => onApprove(request.id)}>
          {t('requests.inbox.approve')}
        </Button>
        <button
          type="button"
          className="btn-danger-outline"
          onClick={() => onReject(request.id)}
        >
          <i className="ti ti-x" aria-hidden="true" /> {t('requests.inbox.reject')}
        </button>
      </>
    );
  }
  if (request.status === 'APPROVED') {
    return (
      <button
        type="button"
        className="btn-danger-outline"
        onClick={() => onCancel(request.id)}
      >
        <i className="ti ti-ban" aria-hidden="true" /> {t('requests.inbox.cancel')}
      </button>
    );
  }
  return <span className="table-actions-empty">—</span>;
}

interface RequestsTableProps {
  requests: Request[];
  employeeMap: Map<number, Employee>;
  emptyLabel: string;
  sort: SortState | null;
  onToggleSort: (field: string) => void;
  onApprove: (id: number) => void;
  onReject: (id: number) => void;
  onCancel: (id: number) => void;
}

// Tabla de solicitudes (avatar + recurso + fechas + estado + acciones). Sirve a todas las
// pestañas: las acciones por fila dependen del estado (ver RowActions). Las columnas de
// fecha (solicitada y creada) son ordenables en servidor (SortableTh).
function RequestsTable({
  requests,
  employeeMap,
  emptyLabel,
  sort,
  onToggleSort,
  onApprove,
  onReject,
  onCancel,
}: RequestsTableProps) {
  const { t } = useTranslation();
  return (
    <div className="table-scroll table-cards-mobile">
      <table className="table">
        <thead>
          <tr className="table-header">
            <th scope="col">{t('requests.inbox.columns.employee')}</th>
            <SortableTh
              field="requestedDate"
              label={t('requests.inbox.columns.date')}
              sort={sort}
              onToggle={onToggleSort}
            />
            <th scope="col">{t('requests.inbox.columns.day')}</th>
            <SortableTh
              field="createdAt"
              label={t('requests.inbox.columns.created')}
              sort={sort}
              onToggle={onToggleSort}
            />
            <th scope="col">{t('requests.inbox.columns.status')}</th>
            <th scope="col">{t('requests.inbox.columns.actions')}</th>
          </tr>
        </thead>
        <tbody>
          {requests.length === 0 ? (
            <tr>
              <td colSpan={6} className="table-empty">
                {emptyLabel}
              </td>
            </tr>
          ) : (
            requests.map((request) => (
              <tr key={request.id} className="table-row">
                <td data-label={t('requests.inbox.columns.employee')}>
                  <EmployeeCell request={request} employee={employeeMap.get(request.employeeId)} />
                </td>
                <td data-label={t('requests.inbox.columns.date')}>
                  <span className="request-date">{request.requestedDate}</span>
                </td>
                <td className="request-weekday" data-label={t('requests.inbox.columns.day')}>
                  {t(`calendar.weekdaysShort.${weekdayIndex(request.requestedDate)}`)}
                </td>
                <td data-label={t('requests.inbox.columns.created')}>{request.createdAt}</td>
                <td data-label={t('requests.inbox.columns.status')}>
                  <span className={`status-badge status-${request.status.toLowerCase()}`}>
                    {t(`requests.status.${request.status}`)}
                  </span>
                </td>
                <td className="table-actions" data-label={t('requests.inbox.columns.actions')}>
                  <RowActions
                    request={request}
                    onApprove={onApprove}
                    onReject={onReject}
                    onCancel={onCancel}
                  />
                </td>
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );
}

interface InboxBodyProps {
  isPendingTab: boolean;
  query: ReturnType<typeof usePendingRequestsQuery>;
  visibleRequests: Request[];
  employeeMap: Map<number, Employee>;
  sort: SortState | null;
  onToggleSort: (field: string) => void;
  onApprove: (id: number) => void;
  onReject: (id: number) => void;
  onCancel: (id: number) => void;
}

// Cuerpo de la bandeja: spinner / error / tabla (estados centrados en el body del marco).
// La paginación vive ahora en el footer fijo del PageFrame. Sirve a todas las pestañas
// (pendientes FIFO y aprobadas/rechazadas/todas por estado). Aísla las ramas de render.
function InboxBody({
  isPendingTab,
  query,
  visibleRequests,
  employeeMap,
  sort,
  onToggleSort,
  onApprove,
  onReject,
  onCancel,
}: InboxBodyProps) {
  const { t } = useTranslation();

  if (query.isLoading) {
    return (
      <div className="pf-state">
        <Spinner />
      </div>
    );
  }
  if (query.isError) {
    return (
      <div className="pf-state">
        <p className="form-error" role="alert">
          {t('requests.inbox.loadError')}
        </p>
      </div>
    );
  }

  const emptyLabel = isPendingTab
    ? t('requests.inbox.empty')
    : t('requests.inbox.emptyByStatus');
  return (
    <RequestsTable
      requests={visibleRequests}
      employeeMap={employeeMap}
      emptyLabel={emptyLabel}
      sort={sort}
      onToggleSort={onToggleSort}
      onApprove={onApprove}
      onReject={onReject}
      onCancel={onCancel}
    />
  );
}

interface InboxModalsProps {
  approveTarget: Request | null;
  rejectTarget: Request | null;
  cancelTarget: Request | null;
  employeeMap: Map<number, Employee>;
  onApproveClose: () => void;
  onRejectClose: () => void;
  onCancelClose: () => void;
}

// Modales de aprobación / rechazo / cancelación montados según la solicitud seleccionada.
// La cancelación de una APPROVED reutiliza RejectRequestModal en modo 'cancel' (mismo
// endpoint reject en backend, que libera el recurso; copia adaptada).
function InboxModals({
  approveTarget,
  rejectTarget,
  cancelTarget,
  employeeMap,
  onApproveClose,
  onRejectClose,
  onCancelClose,
}: InboxModalsProps) {
  return (
    <>
      {approveTarget !== null ? (
        <ApproveRequestModal
          request={approveTarget}
          employee={employeeMap.get(approveTarget.employeeId)}
          onClose={onApproveClose}
          onApproved={onApproveClose}
        />
      ) : null}
      {rejectTarget !== null ? (
        <RejectRequestModal
          request={rejectTarget}
          employee={employeeMap.get(rejectTarget.employeeId)}
          onClose={onRejectClose}
          onRejected={onRejectClose}
        />
      ) : null}
      {cancelTarget !== null ? (
        <RejectRequestModal
          request={cancelTarget}
          employee={employeeMap.get(cancelTarget.employeeId)}
          mode="cancel"
          onClose={onCancelClose}
          onRejected={onCancelClose}
        />
      ) : null}
    </>
  );
}

// Vista ADMIN: bandeja de solicitudes pendientes en orden FIFO (createdAt ASC,
// devuelto por el servidor) + aprobar/rechazar (tasks §4.2-4.3). Pestañas por
// estado + búsqueda por empleado (mockup 02).
export function PendingRequestsPage() {
  const { t } = useTranslation();
  const [page, setPage] = useState(0);
  const [activeTab, setActiveTab] = useState<RequestTab>(PENDING_TAB);
  const [search, setSearch] = useState('');
  const [resourceFilter, setResourceFilter] = useState<ResourceType | 'ALL'>('ALL');
  const [approveId, setApproveId] = useState<number | null>(null);
  const [rejectId, setRejectId] = useState<number | null>(null);
  const [cancelId, setCancelId] = useState<number | null>(null);

  const isPendingTab = activeTab === PENDING_TAB;
  // Orden de las columnas (fecha solicitada / creada), en SERVIDOR. Sin orden → el backend
  // aplica su orden por defecto (pendientes FIFO por createdAt; resto por createdAt desc).
  const { sort, sortParam, toggle } = useTableSort();

  // Pendientes: cola FIFO (y fuente del contador del badge). Resto de pestañas: listado
  // por estado, sólo activo cuando la pestaña no es la de pendientes.
  const pendingQuery = usePendingRequestsQuery({ page, size: PAGE_SIZE, sort: sortParam });
  const byStatusQuery = useRequestsByStatusQuery(
    { page, size: PAGE_SIZE, status: STATUS_BY_TAB[activeTab], sort: sortParam },
    !isPendingTab,
  );
  const query = isPendingTab ? pendingQuery : byStatusQuery;
  const employeesQuery = useEmployeesQuery({ page: 0, size: LOOKUP_SIZE });

  const employees = useMemo(() => employeesQuery.data?.content ?? [], [employeesQuery.data]);
  const employeeMap = useMemo(() => buildEmployeeMap(employees), [employees]);
  const requests = useMemo(() => query.data?.content ?? [], [query.data]);

  const pendingCount = pendingQuery.data?.totalElements ?? 0;

  // Al cambiar de pestaña, vuelve a la primera página (los listados no comparten paginación).
  function handleTabChange(tab: RequestTab): void {
    setActiveTab(tab);
    setPage(0);
  }

  // Al reordenar, vuelve a la primera página (el orden afecta a todo el conjunto).
  function handleToggleSort(field: string): void {
    toggle(field);
    setPage(0);
  }

  const visibleRequests = useMemo(
    () =>
      requests.filter(
        (request) =>
          matchesSearch(request, employeeMap.get(request.employeeId), search) &&
          (resourceFilter === 'ALL' || (request.resourceType ?? 'PARKING') === resourceFilter),
      ),
    [requests, employeeMap, search, resourceFilter],
  );

  const approveTarget = requests.find((request) => request.id === approveId) ?? null;
  const rejectTarget = requests.find((request) => request.id === rejectId) ?? null;
  const cancelTarget = requests.find((request) => request.id === cancelId) ?? null;

  const totalPages = query.data?.totalPages ?? 0;

  return (
    <PageFrame
      eyebrow={t('requests.inbox.eyebrow')}
      title={t('requests.inbox.title')}
      titleId="pending-requests-title"
      bodyLabel={t('requests.inbox.title')}
      actions={
        <ExportMenu
          path={EXPORT_PATHS.requests}
          fallbackBase="requests"
          requiredRole="ADMIN"
          variant="green"
          formats={['csv']}
        />
      }
      toolbar={
        <>
          {/* Izquierda: tipo de recurso. Centro: buscador. Derecha: filtros de estado. */}
          <ResourceSelector
            size="lg"
            withAll
            value={resourceFilter}
            onChange={(value) => setResourceFilter(value as ResourceType | 'ALL')}
            labels={{
              all: t('occupancy.weekly.filters.all'),
              parking: t('occupancy.weekly.resourceType.PARKING'),
              desk: t('occupancy.weekly.resourceType.DESK'),
            }}
            ariaLabel={t('requests.mine.filterResource')}
          />
          <div className="search-box">
            <i className="ti ti-search" aria-hidden="true" />
            <input
              type="text"
              aria-label={t('requests.inbox.search.label')}
              placeholder={t('requests.inbox.search.placeholder')}
              value={search}
              onChange={(event) => setSearch(event.target.value)}
            />
          </div>
          <div
            className="chip-filters"
            role="tablist"
            aria-label={t('requests.inbox.tabs.ariaLabel')}
          >
            {TAB_ORDER.map((id) => {
              const isActive = activeTab === id;
              return (
                <button
                  key={id}
                  type="button"
                  role="tab"
                  aria-selected={isActive}
                  className={`chip-filter${isActive ? ' is-active' : ''}`}
                  onClick={() => handleTabChange(id)}
                >
                  <span
                    className="cf-dot"
                    style={{ background: STATUS_DOT[id] }}
                    aria-hidden="true"
                  />
                  {t(`requests.inbox.tabs.${id}`)}
                  {id === PENDING_TAB ? <span className="cf-count">{pendingCount}</span> : null}
                </button>
              );
            })}
          </div>
        </>
      }
      footer={
        totalPages > 1 ? (
          <>
            <span className="pagination-info">
              {t('requests.pagination.pageInfo', { page: page + 1, total: totalPages })}
            </span>
            <div className="pf-footer-nav">
              <Button
                variant="white"
                disabled={query.data?.first ?? true}
                onClick={() => setPage((previous) => previous - 1)}
              >
                {t('requests.pagination.previous')}
              </Button>
              <Button
                variant="white"
                disabled={query.data?.last ?? true}
                onClick={() => setPage((previous) => previous + 1)}
              >
                {t('requests.pagination.next')}
              </Button>
            </div>
          </>
        ) : undefined
      }
    >
      <InboxBody
        isPendingTab={isPendingTab}
        query={query}
        visibleRequests={visibleRequests}
        employeeMap={employeeMap}
        sort={sort}
        onToggleSort={handleToggleSort}
        onApprove={setApproveId}
        onReject={setRejectId}
        onCancel={setCancelId}
      />

      <InboxModals
        approveTarget={approveTarget}
        rejectTarget={rejectTarget}
        cancelTarget={cancelTarget}
        employeeMap={employeeMap}
        onApproveClose={() => {
          setApproveId(null);
        }}
        onRejectClose={() => {
          setRejectId(null);
        }}
        onCancelClose={() => {
          setCancelId(null);
        }}
      />
    </PageFrame>
  );
}
