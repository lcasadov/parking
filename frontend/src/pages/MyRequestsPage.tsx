import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from '../components/Button';
import { CancelRequestModal } from '../components/CancelRequestModal';
import { CreateRequestModal } from '../components/CreateRequestModal';
import { ExportMenu } from '../components/ExportMenu';
import { PageHeader } from '../components/PageHeader';
import { ResourceTypePill } from '../components/ResourceTypePill';
import { TableEmpty, TableError, TableSkeleton } from '../components/TableStates';
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
  const ready = !query.isLoading && !query.isError;
  const totalPages = query.data?.totalPages ?? 0;
  const isFirst = query.data?.first ?? true;
  const isLast = query.data?.last ?? true;

  function handleCreated(): void {
    // El toast de exito (aprobada al instante vs. pendiente) lo emite el propio
    // CreateRequestModal segun el estado con que nace la solicitud (tasks §6.4).
    setIsCreateOpen(false);
  }

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

  return (
    <section className="my-requests-page" aria-label={t('requests.mine.title')}>
      <PageHeader
        eyebrow={t('requests.mine.eyebrow')}
        title={t('requests.mine.title')}
        description={t('requests.mine.description')}
        actions={
          <>
            <ExportMenu path={EXPORT_PATHS.myRequests} fallbackBase="my-requests" />
            <Button variant="green" icon="plus" onClick={() => setIsCreateOpen(true)}>
              {t('requests.mine.new')}
            </Button>
          </>
        }
      />

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
          <TableEmpty
            icon="calendar-plus"
            message={t('requests.mine.empty')}
            action={
              <Button variant="green" icon="plus" onClick={() => setIsCreateOpen(true)}>
                {t('requests.mine.new')}
              </Button>
            }
          />
        ) : (
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
                {requests.map((request) => (
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
                ))}
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
