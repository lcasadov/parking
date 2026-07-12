import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { ApproveRequestModal } from '../components/ApproveRequestModal';
import { Avatar } from '../components/Avatar';
import { Button } from '../components/Button';
import { ExportMenu } from '../components/ExportMenu';
import { InfoBanner } from '../components/InfoBanner';
import { RejectRequestModal } from '../components/RejectRequestModal';
import { ResourceTypePill } from '../components/ResourceTypePill';
import { Spinner } from '../components/Spinner';
import { Tabs, type TabItem } from '../components/Tabs';
import { EXPORT_PATHS } from '../api/exportApi';
import { useEmployeesQuery } from '../hooks/useEmployees';
import { usePendingRequestsQuery } from '../hooks/useRequests';
import { initialsOf } from '../utils/initials';
import type { Employee } from '../types/employee';
import type { Request } from '../types/request';

const PAGE_SIZE = 20;
const LOOKUP_SIZE = 100;

// Pestañas de la bandeja. Solo PENDING dispone de listado server-side (ADMIN);
// el resto quedan disponibles en la UI pero sin datos (ver informe / banner).
type RequestTab = 'pending' | 'approved' | 'rejected' | 'all';
const PENDING_TAB: RequestTab = 'pending';
const TAB_ORDER: RequestTab[] = ['pending', 'approved', 'rejected', 'all'];

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

// Celda de empleado del mockup 02: avatar-sm + nombre + departamento.
function EmployeeCell({ request, employee }: { request: Request; employee?: Employee }) {
  const { t } = useTranslation();
  const name = employee
    ? `${employee.firstName} ${employee.lastName}`
    : `#${request.employeeId}`;
  const initials = employee ? initialsOf(employee) : name.slice(0, 2).toUpperCase();
  return (
    <div className="employee-cell">
      <Avatar size="sm" initials={initials} label={name} seed={name} />
      <div>
        <div className="employee-cell-name">{name}</div>
        <div className="employee-cell-dept">
          {employee?.department ?? t('requests.approve.context.noDepartment')}
        </div>
      </div>
    </div>
  );
}

interface RequestsTableProps {
  requests: Request[];
  employeeMap: Map<number, Employee>;
  onApprove: (id: number) => void;
  onReject: (id: number) => void;
}

// Tabla de solicitudes pendientes (avatar + recurso + fechas + acciones).
function RequestsTable({ requests, employeeMap, onApprove, onReject }: RequestsTableProps) {
  const { t } = useTranslation();
  return (
    <div className="table-scroll">
      <table className="table">
        <thead>
          <tr className="table-header">
            <th scope="col">{t('requests.inbox.columns.employee')}</th>
            <th scope="col">{t('requests.inbox.columns.resource')}</th>
            <th scope="col">{t('requests.inbox.columns.date')}</th>
            <th scope="col">{t('requests.inbox.columns.created')}</th>
            <th scope="col">{t('requests.inbox.columns.actions')}</th>
          </tr>
        </thead>
        <tbody>
          {requests.length === 0 ? (
            <tr>
              <td colSpan={5} className="table-empty">
                {t('requests.inbox.empty')}
              </td>
            </tr>
          ) : (
            requests.map((request) => (
              <tr key={request.id} className="table-row">
                <td>
                  <EmployeeCell request={request} employee={employeeMap.get(request.employeeId)} />
                </td>
                <td>
                  <ResourceTypePill resourceType={request.resourceType} />
                </td>
                <td>{request.requestedDate}</td>
                <td>{request.createdAt}</td>
                <td className="table-actions">
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
}

// Cuerpo de la bandeja: spinner / error / banner (pestañas resueltas sin datos
// server-side) / tabla / paginación. Aísla las ramas de render de la página.
function InboxBody({
  isPendingTab,
  query,
  visibleRequests,
  employeeMap,
  page,
  onPageChange,
  onApprove,
  onReject,
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
  if (!isPendingTab) {
    return (
      <InfoBanner variant="blue" icon="info-circle">
        {t('requests.inbox.resolvedUnavailable')}
      </InfoBanner>
    );
  }

  const totalPages = query.data?.totalPages ?? 0;
  return (
    <>
      <RequestsTable
        requests={visibleRequests}
        employeeMap={employeeMap}
        onApprove={onApprove}
        onReject={onReject}
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
  employeeMap: Map<number, Employee>;
  onApproveClose: () => void;
  onRejectClose: () => void;
}

// Modales de aprobación/rechazo montados según la solicitud seleccionada.
function InboxModals({
  approveTarget,
  rejectTarget,
  employeeMap,
  onApproveClose,
  onRejectClose,
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
  const [approveId, setApproveId] = useState<number | null>(null);
  const [rejectId, setRejectId] = useState<number | null>(null);

  const query = usePendingRequestsQuery({ page, size: PAGE_SIZE });
  const employeesQuery = useEmployeesQuery({ page: 0, size: LOOKUP_SIZE });

  const employees = useMemo(() => employeesQuery.data?.content ?? [], [employeesQuery.data]);
  const employeeMap = useMemo(() => buildEmployeeMap(employees), [employees]);
  const requests = useMemo(() => query.data?.content ?? [], [query.data]);

  const pendingCount = query.data?.totalElements ?? 0;
  const isPendingTab = activeTab === PENDING_TAB;

  const visibleRequests = useMemo(
    () =>
      requests.filter((request) =>
        matchesSearch(request, employeeMap.get(request.employeeId), search),
      ),
    [requests, employeeMap, search],
  );

  const approveTarget = requests.find((request) => request.id === approveId) ?? null;
  const rejectTarget = requests.find((request) => request.id === rejectId) ?? null;

  const tabs: TabItem[] = TAB_ORDER.map((id) => ({
    id,
    label: t(`requests.inbox.tabs.${id}`),
    ...(id === PENDING_TAB ? { count: pendingCount } : {}),
  }));

  return (
    <section className="pending-requests-page" aria-labelledby="pending-requests-title">
      <header className="page-header">
        <h1 id="pending-requests-title" className="section-title">
          {t('requests.inbox.title')}
        </h1>
        <div className="page-actions">
          <ExportMenu path={EXPORT_PATHS.requests} fallbackBase="requests" requiredRole="ADMIN" />
        </div>
      </header>

      <div className="toolbar">
        <Tabs
          tabs={tabs}
          active={activeTab}
          onChange={(id) => setActiveTab(id as RequestTab)}
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
      />

      <InboxModals
        approveTarget={approveTarget}
        rejectTarget={rejectTarget}
        employeeMap={employeeMap}
        onApproveClose={() => {
          setApproveId(null);
        }}
        onRejectClose={() => {
          setRejectId(null);
        }}
      />
    </section>
  );
}
