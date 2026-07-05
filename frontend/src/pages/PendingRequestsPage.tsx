import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { ApproveRequestModal } from '../components/ApproveRequestModal';
import { Button } from '../components/Button';
import { ExportMenu } from '../components/ExportMenu';
import { RejectRequestModal } from '../components/RejectRequestModal';
import { ResourceTypePill } from '../components/ResourceTypePill';
import { Spinner } from '../components/Spinner';
import { EXPORT_PATHS } from '../api/exportApi';
import { useEmployeesQuery } from '../hooks/useEmployees';
import { usePendingRequestsQuery } from '../hooks/useRequests';
import type { Employee } from '../types/employee';

const PAGE_SIZE = 20;
const LOOKUP_SIZE = 100;

// Construye el mapa id -> nombre legible para los empleados.
function buildEmployeeNames(employees: Employee[]): Map<number, string> {
  const map = new Map<number, string>();
  for (const employee of employees) {
    map.set(employee.id, `${employee.firstName} ${employee.lastName}`);
  }
  return map;
}

// Vista ADMIN: bandeja de solicitudes pendientes en orden FIFO (createdAt ASC,
// devuelto por el servidor) + aprobar/rechazar (tasks §4.2-4.3).
export function PendingRequestsPage() {
  const { t } = useTranslation();
  const [page, setPage] = useState(0);
  const [approveId, setApproveId] = useState<number | null>(null);
  const [rejectId, setRejectId] = useState<number | null>(null);

  const query = usePendingRequestsQuery({ page, size: PAGE_SIZE });
  const employeesQuery = useEmployeesQuery({ page: 0, size: LOOKUP_SIZE });

  const employees = useMemo(() => employeesQuery.data?.content ?? [], [employeesQuery.data]);
  const employeeNames = useMemo(() => buildEmployeeNames(employees), [employees]);

  const requests = query.data?.content ?? [];
  const totalPages = query.data?.totalPages ?? 0;
  const isFirst = query.data?.first ?? true;
  const isLast = query.data?.last ?? true;

  const approveTarget = requests.find((request) => request.id === approveId) ?? null;

  function employeeName(id: number): string {
    return employeeNames.get(id) ?? `#${id}`;
  }

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

      {query.isLoading ? <Spinner /> : null}

      {query.isError ? (
        <p className="form-error" role="alert">
          {t('requests.inbox.loadError')}
        </p>
      ) : null}

      {!query.isLoading && !query.isError ? (
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
                    <td>{employeeName(request.employeeId)}</td>
                    <td>
                      <ResourceTypePill resourceType={request.resourceType} />
                    </td>
                    <td>{request.requestedDate}</td>
                    <td>{request.createdAt}</td>
                    <td className="table-actions">
                      <Button
                        variant="green"
                        icon="check"
                        onClick={() => setApproveId(request.id)}
                      >
                        {t('requests.inbox.approve')}
                      </Button>
                      <Button variant="red" icon="x" onClick={() => setRejectId(request.id)}>
                        {t('requests.inbox.reject')}
                      </Button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      ) : null}

      {totalPages > 1 ? (
        <nav className="pagination" aria-label={t('requests.inbox.title')}>
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

      {approveTarget !== null ? (
        <ApproveRequestModal
          request={approveTarget}
          onClose={() => setApproveId(null)}
          onApproved={() => setApproveId(null)}
        />
      ) : null}

      {rejectId !== null ? (
        <RejectRequestModal
          requestId={rejectId}
          onClose={() => setRejectId(null)}
          onRejected={() => setRejectId(null)}
        />
      ) : null}
    </section>
  );
}
