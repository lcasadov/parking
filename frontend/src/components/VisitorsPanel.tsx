import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from './Button';
import { Spinner } from './Spinner';
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
      <div className="toolbar">
        <input
          type="search"
          className="field-input"
          aria-label={t('visitors.searchLabel')}
          placeholder={t('visitors.searchPlaceholder')}
          value={q}
          onChange={(event) => handleSearch(event.target.value)}
        />
        <Button variant="green" icon="plus" onClick={openCreate}>
          {t('visitors.newVisitor')}
        </Button>
      </div>

      {query.isLoading ? <Spinner /> : null}

      {query.isError ? (
        <p className="form-error" role="alert">
          {t('visitors.loadError')}
        </p>
      ) : null}

      {!query.isLoading && !query.isError ? (
        <div className="table-scroll">
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
              {visitors.length === 0 ? (
                <tr>
                  <td colSpan={6} className="table-empty">
                    {t('visitors.empty')}
                  </td>
                </tr>
              ) : (
                visitors.map((visitor) => (
                  <tr key={visitor.id} className="table-row">
                    <td>{`${visitor.firstName} ${visitor.lastName}`}</td>
                    <td>{visitor.nationalId}</td>
                    <td>{visitor.licensePlate ?? none}</td>
                    <td>{visitor.company ?? none}</td>
                    <td>{visitor.usualReason ?? none}</td>
                    <td className="table-actions">
                      <Button
                        variant="white"
                        icon="eye"
                        onClick={() => setDetailId(visitor.id)}
                      >
                        {t('visitors.actions.view')}
                      </Button>
                      <Button
                        variant="white"
                        icon="pencil"
                        onClick={() => openEdit(visitor)}
                      >
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
                ))
              )}
            </tbody>
          </table>
        </div>
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
