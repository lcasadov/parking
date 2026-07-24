import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from '../components/Button';
import { EmbeddablePageHeader } from '../components/EmbeddablePageHeader';
import { StatusPill } from '../components/StatusPill';
import { TableEmpty, TableError, TableSkeleton } from '../components/TableStates';
import { useMyAdministrativeReleasesQuery } from '../hooks/useReleases';

const PAGE_SIZE = 20;

// Vista ADMIN/AGENCIA (solo lectura): historial paginado de las liberaciones
// administrativas creadas por el propio actor (GET /releases/administrative/mine).
// Orienta al actor sobre las liberaciones que ha ido aplicando; no ofrece acciones.
export function MyAdministrativeReleasesPage({ embedded = false }: { embedded?: boolean } = {}) {
  const { t } = useTranslation();
  const [page, setPage] = useState(0);

  const query = useMyAdministrativeReleasesQuery({ page, size: PAGE_SIZE });
  const releases = query.data?.content ?? [];
  const ready = !query.isLoading && !query.isError;
  const totalPages = query.data?.totalPages ?? 0;
  const isFirst = query.data?.first ?? true;
  const isLast = query.data?.last ?? true;

  return (
    <section
      className="administrative-releases-history-page"
      aria-label={t('releases.history.title')}
    >
      <EmbeddablePageHeader
        embedded={embedded}
        eyebrow={t('releases.history.eyebrow')}
        title={t('releases.history.title')}
        description={t('releases.history.description')}
      />

      {query.isLoading ? <TableSkeleton label={t('common.loading')} columns={4} /> : null}

      {query.isError ? (
        <TableError
          message={t('releases.history.loadError')}
          retryLabel={t('common.retry')}
          onRetry={() => void query.refetch()}
        />
      ) : null}

      {ready ? (
        releases.length === 0 ? (
          <TableEmpty icon="calendar-off" message={t('releases.history.empty')} />
        ) : (
          <div className="table-scroll table-cards-mobile">
            <table className="table">
              <thead>
                <tr className="table-header">
                  <th scope="col">{t('releases.history.columns.date')}</th>
                  <th scope="col">{t('releases.history.columns.type')}</th>
                  <th scope="col">{t('releases.history.columns.resource')}</th>
                  <th scope="col">{t('releases.history.columns.reason')}</th>
                </tr>
              </thead>
              <tbody>
                {releases.map((release) => (
                  <tr key={release.id} className="table-row">
                    <td data-label={t('releases.history.columns.date')}>{release.releaseDate}</td>
                    <td data-label={t('releases.history.columns.type')}>
                      <StatusPill tone="released">
                        {t(`releases.byDate.resourceType.${release.resourceType ?? 'PARKING'}`)}
                      </StatusPill>
                    </td>
                    <td data-label={t('releases.history.columns.resource')}>{`#${release.parkingSpaceId}`}</td>
                    <td data-label={t('releases.history.columns.reason')}>{release.reason ?? '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )
      ) : null}

      {totalPages > 1 ? (
        <nav className="pagination" aria-label={t('releases.history.title')}>
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
    </section>
  );
}
