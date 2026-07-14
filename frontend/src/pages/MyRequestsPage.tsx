import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from '../components/Button';
import { CancelRequestModal } from '../components/CancelRequestModal';
import { CreateRequestModal } from '../components/CreateRequestModal';
import { ExportMenu } from '../components/ExportMenu';
import { ResourceTypePill } from '../components/ResourceTypePill';
import { Spinner } from '../components/Spinner';
import { EXPORT_PATHS } from '../api/exportApi';
import { useMyRequestsQuery } from '../hooks/useRequests';
import { canCancelRequest } from '../utils/requests';
import type { Request } from '../types/request';

const PAGE_SIZE = 20;

interface CancelTarget {
  id: number;
  requestedDate: string;
}

// Vista EMPLOYEE: lista paginada de las solicitudes propias + alta de solicitud
// + cancelacion cuando estan en PENDING o APPROVED con fecha futura (>= hoy),
// esta ultima libera el recurso (change cancel-approved-request §5).
export function MyRequestsPage() {
  const { t } = useTranslation();
  const [page, setPage] = useState(0);
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [cancelTarget, setCancelTarget] = useState<CancelTarget | null>(null);

  const query = useMyRequestsQuery({ page, size: PAGE_SIZE });
  const requests = query.data?.content ?? [];
  const totalPages = query.data?.totalPages ?? 0;
  const isFirst = query.data?.first ?? true;
  const isLast = query.data?.last ?? true;

  function handleCreated(): void {
    // El toast de exito (aprobada al instante vs. pendiente) lo emite el propio
    // CreateRequestModal segun el estado con que nace la solicitud (tasks §6.4).
    setIsCreateOpen(false);
  }

  function spaceLabel(request: Request): string {
    return request.parkingSpaceId ? `#${request.parkingSpaceId}` : '—';
  }

  return (
    <section className="my-requests-page" aria-labelledby="my-requests-title">
      <header className="page-header">
        <h1 id="my-requests-title" className="section-title">
          {t('requests.mine.title')}
        </h1>
        <div className="page-actions">
          <ExportMenu path={EXPORT_PATHS.myRequests} fallbackBase="my-requests" />
          <Button variant="green" icon="plus" onClick={() => setIsCreateOpen(true)}>
            {t('requests.mine.new')}
          </Button>
        </div>
      </header>

      {query.isLoading ? <Spinner /> : null}

      {query.isError ? (
        <p className="form-error" role="alert">
          {t('requests.mine.loadError')}
        </p>
      ) : null}

      {!query.isLoading && !query.isError ? (
        <div className="table-scroll">
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
              {requests.length === 0 ? (
                <tr>
                  <td colSpan={5} className="table-empty">
                    {t('requests.mine.empty')}
                  </td>
                </tr>
              ) : (
                requests.map((request) => (
                  <tr key={request.id} className="table-row">
                    <td>{request.requestedDate}</td>
                    <td>
                      <span className={`status-badge status-${request.status.toLowerCase()}`}>
                        {t(`requests.status.${request.status}`)}
                      </span>
                    </td>
                    <td>
                      <ResourceTypePill resourceType={request.resourceType} />
                    </td>
                    <td>{spaceLabel(request)}</td>
                    <td className="table-actions">
                      {canCancelRequest(request) ? (
                        <Button
                          variant="red"
                          icon="x"
                          onClick={() =>
                            setCancelTarget({
                              id: request.id,
                              requestedDate: request.requestedDate,
                            })
                          }
                        >
                          {t('requests.mine.cancel')}
                        </Button>
                      ) : null}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
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

      {isCreateOpen ? (
        <CreateRequestModal onClose={() => setIsCreateOpen(false)} onCreated={handleCreated} />
      ) : null}

      {cancelTarget ? (
        <CancelRequestModal
          requestId={cancelTarget.id}
          requestedDate={cancelTarget.requestedDate}
          onClose={() => setCancelTarget(null)}
          onCancelled={() => setCancelTarget(null)}
        />
      ) : null}
    </section>
  );
}
