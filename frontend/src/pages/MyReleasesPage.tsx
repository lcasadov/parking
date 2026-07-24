import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from '../components/Button';
import { CancelReleaseModal } from '../components/CancelReleaseModal';
import { EmbeddablePageHeader } from '../components/EmbeddablePageHeader';
import { TableEmpty, TableError, TableSkeleton } from '../components/TableStates';
import { useMyReleasesQuery } from '../hooks/useReleases';
import { canCancelRelease } from '../utils/releases';

const PAGE_SIZE = 20;

interface CancelTarget {
  id: number;
  releaseDate: string;
}

// Vista EMPLOYEE: lista paginada de las liberaciones propias (GET /releases/mine)
// con anulacion de las futuras (DELETE /releases/{id}); las pasadas no son
// anulables y su boton se muestra deshabilitado (tasks §4.2).
export function MyReleasesPage({ embedded = false }: { embedded?: boolean } = {}) {
  const { t } = useTranslation();
  const [page, setPage] = useState(0);
  const [cancelTarget, setCancelTarget] = useState<CancelTarget | null>(null);

  const query = useMyReleasesQuery({ page, size: PAGE_SIZE });
  const releases = query.data?.content ?? [];
  const ready = !query.isLoading && !query.isError;
  const totalPages = query.data?.totalPages ?? 0;
  const isFirst = query.data?.first ?? true;
  const isLast = query.data?.last ?? true;

  return (
    <section className="my-releases-page" aria-label={t('releases.mine.title')}>
      <EmbeddablePageHeader
        embedded={embedded}
        eyebrow={t('releases.mine.eyebrow')}
        title={t('releases.mine.title')}
        description={t('releases.mine.description')}
      />

      {query.isLoading ? <TableSkeleton label={t('common.loading')} columns={4} /> : null}

      {query.isError ? (
        <TableError
          message={t('releases.mine.loadError')}
          retryLabel={t('common.retry')}
          onRetry={() => void query.refetch()}
        />
      ) : null}

      {ready ? (
        releases.length === 0 ? (
          <TableEmpty icon="calendar-off" message={t('releases.mine.empty')} />
        ) : (
          <div className="table-scroll">
            <table className="table">
              <thead>
                <tr className="table-header">
                  <th scope="col">{t('releases.mine.columns.date')}</th>
                  <th scope="col">{t('releases.mine.columns.type')}</th>
                  <th scope="col">{t('releases.mine.columns.space')}</th>
                  <th scope="col">{t('releases.mine.columns.actions')}</th>
                </tr>
              </thead>
              <tbody>
                {releases.map((release) => (
                  <tr key={release.id} className="table-row">
                    <td>{release.releaseDate}</td>
                    <td>
                      <span
                        className={`pill ${
                          release.type === 'VOLUNTARY' ? 'pill-pink' : 'pill-blue'
                        }`}
                      >
                        {t(`releases.type.${release.type}`)}
                      </span>
                    </td>
                    <td>{`#${release.parkingSpaceId}`}</td>
                    <td className="table-actions">
                      <Button
                        variant="red"
                        icon="x"
                        disabled={!canCancelRelease(release.releaseDate)}
                        onClick={() =>
                          setCancelTarget({ id: release.id, releaseDate: release.releaseDate })
                        }
                      >
                        {t('releases.mine.cancel')}
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )
      ) : null}

      {totalPages > 1 ? (
        <nav className="pagination" aria-label={t('releases.mine.title')}>
          <Button variant="white" disabled={isFirst} onClick={() => setPage((p) => p - 1)}>
            {t('releases.pagination.previous')}
          </Button>
          <span className="pagination-info">
            {t('releases.pagination.pageInfo', { page: page + 1, total: totalPages })}
          </span>
          <Button variant="white" disabled={isLast} onClick={() => setPage((p) => p + 1)}>
            {t('releases.pagination.next')}
          </Button>
        </nav>
      ) : null}

      {cancelTarget ? (
        <CancelReleaseModal
          releaseId={cancelTarget.id}
          releaseDate={cancelTarget.releaseDate}
          onClose={() => setCancelTarget(null)}
          onCancelled={() => setCancelTarget(null)}
        />
      ) : null}
    </section>
  );
}
