import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from './Button';
import { TableEmpty, TableError, TableSkeleton } from './TableStates';
import { CancelVisitorReservationModal } from './CancelVisitorReservationModal';
import { useVisitorReservationsQuery } from '../hooks/useVisitorReservations';
import { canCancelReservation } from '../utils/visitors';

const PAGE_SIZE = 20;

interface CancelTarget {
  id: number;
  reservationDate: string;
}

interface VisitorReservationsPanelProps {
  // El alta ("Nueva reserva") vive en el control-row del hub; el asistente lo abre allí.
  onCreate: () => void;
}

// Panel ADMIN: reservas de visita con anulacion de las futuras
// (DELETE /visitor-reservations/{id}); las pasadas no son anulables y su boton
// se muestra deshabilitado (tasks §4.5). El botón "Nueva reserva" vive en el hub.
export function VisitorReservationsPanel({ onCreate }: VisitorReservationsPanelProps) {
  const { t } = useTranslation();
  const [page, setPage] = useState(0);
  const [cancelTarget, setCancelTarget] = useState<CancelTarget | null>(null);

  const query = useVisitorReservationsQuery({ page, size: PAGE_SIZE });
  const reservations = query.data?.content ?? [];
  const ready = !query.isLoading && !query.isError;
  const totalPages = query.data?.totalPages ?? 0;
  const isFirst = query.data?.first ?? true;
  const isLast = query.data?.last ?? true;

  return (
    <div className="visitor-reservations-panel">
      {query.isLoading ? <TableSkeleton label={t('common.loading')} columns={5} /> : null}

      {query.isError ? (
        <TableError
          message={t('visitors.reservations.loadError')}
          retryLabel={t('common.retry')}
          onRetry={() => void query.refetch()}
        />
      ) : null}

      {ready && reservations.length === 0 ? (
        <TableEmpty
          icon="calendar-off"
          message={t('visitors.reservations.empty')}
          action={
            <Button variant="green" icon="calendar-plus" onClick={onCreate}>
              {t('visitors.newReservation')}
            </Button>
          }
        />
      ) : null}

      {ready && reservations.length > 0 ? (
        <div className="table-scroll table-cards-mobile">
          <table className="table">
            <thead>
              <tr className="table-header">
                <th scope="col">{t('visitors.reservations.columns.date')}</th>
                <th scope="col">{t('visitors.reservations.columns.visitor')}</th>
                <th scope="col">{t('visitors.reservations.columns.space')}</th>
                <th scope="col">{t('visitors.reservations.columns.notes')}</th>
                <th scope="col">{t('visitors.reservations.columns.actions')}</th>
              </tr>
            </thead>
            <tbody>
              {reservations.map((reservation) => (
                <tr key={reservation.id} className="table-row">
                  <td data-label={t('visitors.reservations.columns.date')}>
                    {reservation.reservationDate}
                  </td>
                  <td data-label={t('visitors.reservations.columns.visitor')}>
                    {`#${reservation.visitorId}`}
                  </td>
                  <td data-label={t('visitors.reservations.columns.space')}>
                    {`#${reservation.resourceId}`}
                  </td>
                  <td data-label={t('visitors.reservations.columns.notes')}>
                    {reservation.notes ?? t('visitors.detail.none')}
                  </td>
                  <td className="table-actions" data-label={t('visitors.reservations.columns.actions')}>
                    <Button
                      variant="red"
                      icon="x"
                      disabled={!canCancelReservation(reservation.reservationDate)}
                      onClick={() =>
                        setCancelTarget({
                          id: reservation.id,
                          reservationDate: reservation.reservationDate,
                        })
                      }
                    >
                      {t('visitors.reservations.cancel')}
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : null}

      {totalPages > 1 ? (
        <nav className="pagination" aria-label={t('visitors.tabs.reservations')}>
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

      {cancelTarget ? (
        <CancelVisitorReservationModal
          reservationId={cancelTarget.id}
          reservationDate={cancelTarget.reservationDate}
          onClose={() => setCancelTarget(null)}
          onCancelled={() => setCancelTarget(null)}
        />
      ) : null}
    </div>
  );
}
