import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from './Button';
import { TableEmpty, TableError, TableSkeleton } from './TableStates';
import { ReservationWizard } from './wizard/ReservationWizard';
import { useVisitorsQuery } from '../hooks/useVisitors';
import type { Visitor } from '../types/visitor';

const PAGE_SIZE = 20;

interface VisitorsPanelProps {
  // Búsqueda controlada por el hub (VisitorsPage), donde vive junto al botón "Nuevo".
  q: string;
  onCreate: () => void;
  onEdit: (visitor: Visitor) => void;
}

// Panel ADMIN: fichas de visitante (tabla + reserva por fila). El buscador y el botón
// "Nuevo" viven en el control-row del hub; el alta/edición abre el modal en el hub.
export function VisitorsPanel({ q, onCreate, onEdit }: VisitorsPanelProps) {
  const { t } = useTranslation();
  const [page, setPage] = useState(0);
  const [reserveVisitor, setReserveVisitor] = useState<Visitor | null>(null);

  const query = useVisitorsQuery({ page, size: PAGE_SIZE, q });
  const visitors = query.data?.content ?? [];
  const ready = !query.isLoading && !query.isError;
  const totalPages = query.data?.totalPages ?? 0;
  const isFirst = query.data?.first ?? true;
  const isLast = query.data?.last ?? true;
  const none = t('visitors.detail.none');

  // La búsqueda la controla el hub; al cambiarla, volvemos a la primera página.
  useEffect(() => {
    setPage(0);
  }, [q]);

  return (
    <div className="visitors-panel">
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
              <Button variant="green" icon="plus" onClick={onCreate}>
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
                      <Button variant="white" icon="pencil" onClick={() => onEdit(visitor)}>
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

      {reserveVisitor ? (
        <ReservationWizard
          initialBeneficiaryType="VISITOR"
          initialVisitorId={reserveVisitor.id}
          onClose={() => setReserveVisitor(null)}
        />
      ) : null}
    </div>
  );
}
