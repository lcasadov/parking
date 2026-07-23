import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from './Button';
import { SearchBox } from './SearchBox';
import { TableEmpty, TableError, TableSkeleton } from './TableStates';
import { Toolbar } from './Toolbar';
import { VisitorDetailModal } from './VisitorDetailModal';
import { VisitorFormModal } from './VisitorFormModal';
import { VisitorReservationModal } from './VisitorReservationModal';
import { useVisitorsQuery } from '../hooks/useVisitors';
import type { Visitor } from '../types/visitor';

const PAGE_SIZE = 20;

// Panel ADMIN: fichas de visitante con buscador (DNI/nombre/matricula), alta,
// edicion, detalle y lanzamiento de reserva por fila (tasks §4.1-4.4).
export function VisitorsPanel() {
  const { t } = useTranslation();
  const [q, setQ] = useState('');
  const [page, setPage] = useState(0);
  const [formVisitor, setFormVisitor] = useState<Visitor | null>(null);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [detailId, setDetailId] = useState<number | null>(null);
  const [reserveVisitor, setReserveVisitor] = useState<Visitor | null>(null);

  const query = useVisitorsQuery({ page, size: PAGE_SIZE, q });
  const visitors = query.data?.content ?? [];
  const ready = !query.isLoading && !query.isError;
  const totalPages = query.data?.totalPages ?? 0;
  const isFirst = query.data?.first ?? true;
  const isLast = query.data?.last ?? true;
  const none = t('visitors.detail.none');

  function handleSearch(value: string): void {
    setQ(value);
    setPage(0);
  }

  function openCreate(): void {
    setFormVisitor(null);
    setIsFormOpen(true);
  }

  function openEdit(visitor: Visitor): void {
    setFormVisitor(visitor);
    setIsFormOpen(true);
  }

  function closeForm(): void {
    setIsFormOpen(false);
    setFormVisitor(null);
  }

  return (
    <div className="visitors-panel">
      <Toolbar ariaLabel={t('visitors.searchLabel')}>
        <SearchBox
          label={t('visitors.searchLabel')}
          placeholder={t('visitors.searchPlaceholder')}
          value={q}
          onValueChange={handleSearch}
        />
        <Button variant="green" icon="plus" onClick={openCreate}>
          {t('visitors.newVisitor')}
        </Button>
      </Toolbar>

      {query.isLoading ? <TableSkeleton label={t('common.loading')} columns={6} /> : null}

      {query.isError ? (
        <TableError
          message={t('visitors.loadError')}
          retryLabel={t('common.retry')}
          onRetry={() => void query.refetch()}
        />
      ) : null}

      {ready ? (
        visitors.length === 0 ? (
          <TableEmpty
            icon="user-question"
            message={t('visitors.empty')}
            action={
              <Button variant="green" icon="plus" onClick={openCreate}>
                {t('visitors.newVisitor')}
              </Button>
            }
          />
        ) : (
          <div className="table-scroll table-cards-mobile">
            <table className="table">
              <thead>
                <tr className="table-header">
                  <th scope="col">{t('visitors.columns.name')}</th>
                  <th scope="col">{t('visitors.columns.nationalId')}</th>
                  <th scope="col">{t('visitors.columns.licensePlate')}</th>
                  <th scope="col">{t('visitors.columns.company')}</th>
                  <th scope="col">{t('visitors.columns.usualReason')}</th>
                  <th scope="col">{t('visitors.columns.actions')}</th>
                </tr>
              </thead>
              <tbody>
                {visitors.map((visitor) => (
                  <tr key={visitor.id} className="table-row">
                    <td data-label={t('visitors.columns.name')}>
                      {`${visitor.firstName} ${visitor.lastName}`}
                    </td>
                    <td data-label={t('visitors.columns.nationalId')}>{visitor.nationalId}</td>
                    <td data-label={t('visitors.columns.licensePlate')}>
                      {visitor.licensePlate ?? none}
                    </td>
                    <td data-label={t('visitors.columns.company')}>{visitor.company ?? none}</td>
                    <td data-label={t('visitors.columns.usualReason')}>
                      {visitor.usualReason ?? none}
                    </td>
                    <td className="table-actions" data-label={t('visitors.columns.actions')}>
                      <Button variant="white" icon="eye" onClick={() => setDetailId(visitor.id)}>
                        {t('visitors.actions.view')}
                      </Button>
                      <Button variant="white" icon="pencil" onClick={() => openEdit(visitor)}>
                        {t('visitors.actions.edit')}
                      </Button>
                      <Button
                        variant="blue"
                        icon="calendar-plus"
                        onClick={() => setReserveVisitor(visitor)}
                      >
                        {t('visitors.actions.reserve')}
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
        <nav className="pagination" aria-label={t('visitors.title')}>
          <Button variant="white" disabled={isFirst} onClick={() => setPage((p) => p - 1)}>
            {t('visitors.pagination.previous')}
          </Button>
          <span className="pagination-info">
            {t('visitors.pagination.pageInfo', { page: page + 1, total: totalPages })}
          </span>
          <Button variant="white" disabled={isLast} onClick={() => setPage((p) => p + 1)}>
            {t('visitors.pagination.next')}
          </Button>
        </nav>
      ) : null}

      {isFormOpen ? (
        <VisitorFormModal visitor={formVisitor} onClose={closeForm} onSaved={closeForm} />
      ) : null}

      {detailId !== null ? (
        <VisitorDetailModal visitorId={detailId} onClose={() => setDetailId(null)} />
      ) : null}

      {reserveVisitor ? (
        <VisitorReservationModal
          visitor={reserveVisitor}
          onClose={() => setReserveVisitor(null)}
          onCreated={() => setReserveVisitor(null)}
        />
      ) : null}
    </div>
  );
}
