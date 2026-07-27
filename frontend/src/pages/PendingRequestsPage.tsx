import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { ApproveRequestModal } from '../components/ApproveRequestModal';
import { Avatar } from '../components/Avatar';
import { Button } from '../components/Button';
import { ExportMenu } from '../components/ExportMenu';
import { RejectRequestModal } from '../components/RejectRequestModal';
import { ResourceTypePill } from '../components/ResourceTypePill';
import { Spinner } from '../components/Spinner';
import { Tabs, type TabItem } from '../components/Tabs';
import { EXPORT_PATHS } from '../api/exportApi';
import { useEmployeesQuery } from '../hooks/useEmployees';
import { usePendingRequestsQuery, useRequestsByStatusQuery } from '../hooks/useRequests';
import { weekdayIndex } from '../utils/calendar';
import { initialsOf } from '../utils/initials';
import type { Employee } from '../types/employee';
import type { Request, RequestStatus, ResourceType } from '../types/request';

// Filtro por recurso (plaza/puesto) + "todos".
const RESOURCE_FILTERS: (ResourceType | 'ALL')[] = ['ALL', 'PARKING', 'DESK'];

const PAGE_SIZE = 20;
const LOOKUP_SIZE = 100;

// Pestañas de la bandeja. La de pendientes usa el listado FIFO (ADMIN); el resto
// (aprobadas / rechazadas / todas) usa el listado admin por estado (GET /requests).
type RequestTab = 'pending' | 'approved' | 'rejected' | 'all';
const PENDING_TAB: RequestTab = 'pending';
const TAB_ORDER: RequestTab[] = ['pending', 'approved', 'rejected', 'all'];

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
  onApprove: (id: number) => void;
  onReject: (id: number) => void;
  onCancel: (id: number) => void;
}

// Tabla de solicitudes (avatar + recurso + fechas + estado + acciones). Sirve a todas las
// pestañas: las acciones por fila dependen del estado (ver RowActions).
function RequestsTable({
  requests,
  employeeMap,
  emptyLabel,
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
            <th scope="col">{t('requests.inbox.columns.date')}</th>
            <th scope="col">{t('requests.inbox.columns.day')}</th>
            <th scope="col">{t('requests.inbox.columns.created')}</th>
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

interface PaginationProps {
  page: number;
  totalPages: number;
  isFirst: boolean;
  isLast: boolean;
  onChange: (updater: (previous: number) => number) => void;
}

// Controles de paginación de la bandeja (solo cuando hay más de una página).
function InboxPagination({ page, totalPages, isFirst, isLast, onChange }: PaginationProps) {
  const { t } = useTranslation();
  return (
    <nav className="pagination" aria-label={t('requests.inbox.title')}>
      <Button variant="white" disabled={isFirst} onClick={() => onChange((p) => p - 1)}>
        {t('requests.pagination.previous')}
      </Button>
      <span className="pagination-info">
        {t('requests.pagination.pageInfo', { page: page + 1, total: totalPages })}
      </span>
      <Button variant="white" disabled={isLast} onClick={() => onChange((p) => p + 1)}>
        {t('requests.pagination.next')}
      </Button>
    </nav>
  );
}

interface InboxBodyProps {
  isPendingTab: boolean;
  query: ReturnType<typeof usePendingRequestsQuery>;
  visibleRequests: Request[];
  employeeMap: Map<number, Employee>;
  page: number;
  onPageChange: (updater: (previous: number) => number) => void;
  onApprove: (id: number) => void;
  onReject: (id: number) => void;
  onCancel: (id: number) => void;
}

// Cuerpo de la bandeja: spinner / error / tabla / paginación. Sirve a todas las pestañas
// (pendientes FIFO y aprobadas/rechazadas/todas por estado). Aísla las ramas de render.
function InboxBody({
  isPendingTab,
  query,
  visibleRequests,
  employeeMap,
  page,
  onPageChange,
  onApprove,
  onReject,
  onCancel,
}: InboxBodyProps) {
  const { t } = useTranslation();

  if (query.isLoading) {
    return <Spinner />;
  }
  if (query.isError) {
    return (
      <p className="form-error" role="alert">
        {t('requests.inbox.loadError')}
      </p>
    );
  }

  const totalPages = query.data?.totalPages ?? 0;
  const emptyLabel = isPendingTab
    ? t('requests.inbox.empty')
    : t('requests.inbox.emptyByStatus');
  return (
    <>
      <RequestsTable
        requests={visibleRequests}
        employeeMap={employeeMap}
        emptyLabel={emptyLabel}
        onApprove={onApprove}
        onReject={onReject}
        onCancel={onCancel}
      />
      {totalPages > 1 ? (
        <InboxPagination
          page={page}
          totalPages={totalPages}
          isFirst={query.data?.first ?? true}
          isLast={query.data?.last ?? true}
          onChange={onPageChange}
        />
      ) : null}
    </>
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

  // Pendientes: cola FIFO (y fuente del contador del badge). Resto de pestañas: listado
  // por estado, sólo activo cuando la pestaña no es la de pendientes.
  const pendingQuery = usePendingRequestsQuery({ page, size: PAGE_SIZE });
  const byStatusQuery = useRequestsByStatusQuery(
    { page, size: PAGE_SIZE, status: STATUS_BY_TAB[activeTab] },
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

  const tabs: TabItem[] = TAB_ORDER.map((id) => ({
    id,
    label: t(`requests.inbox.tabs.${id}`),
    ...(id === PENDING_TAB ? { count: pendingCount } : {}),
  }));

  return (
    <section className="pending-requests-page" aria-labelledby="pending-requests-title">
      <header className="page-header">
        <div className="page-heading">
          <span className="page-eyebrow">{t('requests.inbox.eyebrow')}</span>
          <h1 id="pending-requests-title" className="section-title">
            {t('requests.inbox.title')}
          </h1>
          <p className="page-description">{t('requests.inbox.description')}</p>
        </div>
        <div className="page-actions">
          <ExportMenu path={EXPORT_PATHS.requests} fallbackBase="requests" requiredRole="ADMIN" />
        </div>
      </header>

      <div className="toolbar">
        <Tabs
          tabs={tabs}
          active={activeTab}
          onChange={(id) => handleTabChange(id as RequestTab)}
          ariaLabel={t('requests.inbox.tabs.ariaLabel')}
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
        <label className="mr-status-filter">
          <span className="sr-only">{t('requests.mine.filterResource')}</span>
          <select
            className="field-input"
            value={resourceFilter}
            onChange={(event) => setResourceFilter(event.target.value as ResourceType | 'ALL')}
          >
            {RESOURCE_FILTERS.map((value) => (
              <option key={value} value={value}>
                {value === 'ALL'
                  ? t('requests.mine.filterAllResources')
                  : t(`requests.resourceType.${value}`)}
              </option>
            ))}
          </select>
        </label>
      </div>

      <InboxBody
        isPendingTab={isPendingTab}
        query={query}
        visibleRequests={visibleRequests}
        employeeMap={employeeMap}
        page={page}
        onPageChange={setPage}
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
    </section>
  );
}
