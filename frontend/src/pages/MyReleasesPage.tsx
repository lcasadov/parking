import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from '../components/Button';
import { CancelReleaseModal } from '../components/CancelReleaseModal';
import { Spinner } from '../components/Spinner';
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
export function MyReleasesPage() {
  const { t } = useTranslation();
  const [page, setPage] = useState(0);
  const [cancelTarget, setCancelTarget] = useState<CancelTarget | null>(null);

  const query = useMyReleasesQuery({ page, size: PAGE_SIZE });
  const releases = query.data?.content ?? [];
  const totalPages = query.data?.totalPages ?? 0;
  const isFirst = query.data?.first ?? true;
  const isLast = query.data?.last ?? true;

  return (
    <section className="my-releases-page" aria-labelledby="my-releases-title">
      <header className="page-header">
        <h1 id="my-releases-title" className="section-title">
          {t('releases.mine.title')}
        </h1>
      </header>

      {query.isLoading ? <Spinner /> : null}

      {query.isError ? (
        <p className="form-error" role="alert">
          {t('releases.mine.loadError')}
        </p>
      ) : null}

      {!query.isLoading && !query.isError ? (
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
              {releases.length === 0 ? (
                <tr>
                  <td colSpan={4} className="table-empty">
                    {t('releases.mine.empty')}
                  </td>
                </tr>
              ) : (
                releases.map((release) => (
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
                ))
              )}
            </tbody>
          </table>
        </div>
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
