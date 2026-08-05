import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from '../components/Button';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { PageFrame } from '../components/PageFrame';
import { TableEmpty, TableError, TableSkeleton } from '../components/TableStates';
import { VehicleHistoryModal } from '../components/VehicleHistoryModal';
import { VehicleStatusModal } from '../components/VehicleStatusModal';
import type { VehicleStatusCounts } from '../api/employeeVehicleReviewApi';
import {
  useConfirmVehicleDeletion,
  useRestoreVehicle,
  useVehicleReviewQuery,
  useVehicleStatusCountsQuery,
} from '../hooks/useEmployeeVehicleReview';
import { useToast } from '../hooks/useToast';
import type { VehicleReviewRow } from '../types/employeeVehicleReview';
import type { VehicleStatus } from '../types/vehicle';

const PAGE_SIZE = 20;
const NONE = '—';

// Filtros: "Abiertos" reúne todo lo que sigue en juego (pendiente + en trámite + pendiente de
// borrado); el resto muestra solo su estado; "Todos" no filtra.
type Tab = 'ABIERTOS' | 'PENDING' | 'IN_PROGRESS' | 'APPROVED' | 'REJECTED' | 'ALL';
const TABS: Tab[] = ['ABIERTOS', 'PENDING', 'IN_PROGRESS', 'APPROVED', 'REJECTED', 'ALL'];

const TAB_STATUSES: Record<Tab, VehicleStatus[] | undefined> = {
  ABIERTOS: ['PENDING', 'IN_PROGRESS', 'PENDING_DELETION'],
  PENDING: ['PENDING'],
  IN_PROGRESS: ['IN_PROGRESS'],
  APPROVED: ['APPROVED'],
  REJECTED: ['REJECTED'],
  ALL: undefined,
};

const STATUS_CLASS: Record<VehicleStatus, string> = {
  PENDING: 'status-pending',
  IN_PROGRESS: 'status-in-progress',
  APPROVED: 'status-approved',
  REJECTED: 'status-rejected',
  PENDING_DELETION: 'status-rejected',
};

const ALL_STATUSES: VehicleStatus[] = [
  'PENDING',
  'IN_PROGRESS',
  'APPROVED',
  'REJECTED',
  'PENDING_DELETION',
];

const DOT: Record<Tab, string> = {
  ABIERTOS: 'var(--pend, #d97706)',
  PENDING: 'var(--pend, #d97706)',
  IN_PROGRESS: 'var(--info, #2563eb)',
  APPROVED: 'var(--accent, #16a34a)',
  REJECTED: 'var(--red, #dc2626)',
  ALL: 'var(--ink-faint, #9ca3af)',
};

function countFor(tab: Tab, counts: VehicleStatusCounts): number {
  const statuses = TAB_STATUSES[tab] ?? ALL_STATUSES;
  return statuses.reduce((sum, status) => sum + (counts[status] ?? 0), 0);
}

// Bandeja de validación de vehículos (ADMIN, change employee-vehicle-self-service, Fase 2):
// filtros por estado (con contadores) + búsqueda + tabla; cada fila se gestiona con "Cambiar
// estado" (modal selector) e "Histórico". Los pendientes de borrado tienen restaurar / confirmar.
export function VehicleReviewPage() {
  const { t } = useTranslation();
  const toast = useToast();
  const [tab, setTab] = useState<Tab>('ABIERTOS');
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [historyTarget, setHistoryTarget] = useState<VehicleReviewRow | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<VehicleReviewRow | null>(null);
  const [statusTarget, setStatusTarget] = useState<VehicleReviewRow | null>(null);

  const query = useVehicleReviewQuery({ statuses: TAB_STATUSES[tab], page, size: PAGE_SIZE });
  const countsQuery = useVehicleStatusCountsQuery();
  const restoreMutation = useRestoreVehicle();
  const confirmDeletion = useConfirmVehicleDeletion();

  const rows = query.data?.content ?? [];
  const totalPages = query.data?.totalPages ?? 0;
  const q = search.trim().toLowerCase();
  const visible =
    q === ''
      ? rows
      : rows.filter(
          (row) =>
            row.licensePlate.toLowerCase().includes(q) ||
            row.employee.fullName.toLowerCase().includes(q),
        );

  function changeTab(next: Tab): void {
    setTab(next);
    setPage(0);
  }

  function restore(id: number): void {
    restoreMutation
      .mutateAsync(id)
      .then(() => toast.success('vehicles.review.toasts.restored'))
      .catch(() => toast.error('vehicles.review.errors.generic'));
  }

  return (
    <PageFrame
      eyebrow={t('vehicles.review.eyebrow')}
      title={t('vehicles.review.title')}
      titleId="vehicle-review-title"
      bodyLabel={t('vehicles.review.title')}
      toolbar={
        <ReviewToolbar
          search={search}
          onSearch={setSearch}
          tab={tab}
          onTab={changeTab}
          counts={countsQuery.data ?? {}}
        />
      }
      footer={
        totalPages > 1 ? (
          <ReviewFooter
            page={page}
            totalPages={totalPages}
            isFirst={query.data?.first ?? true}
            isLast={query.data?.last ?? true}
            onPrev={() => setPage((p) => p - 1)}
            onNext={() => setPage((p) => p + 1)}
          />
        ) : undefined
      }
    >
      <ReviewBody
        isLoading={query.isLoading}
        isError={query.isError}
        onRetry={() => void query.refetch()}
        rows={visible}
        busy={restoreMutation.isPending}
        onRestore={restore}
        onConfirmDelete={setDeleteTarget}
        onChangeStatus={setStatusTarget}
        onHistory={setHistoryTarget}
      />

      {historyTarget ? (
        <VehicleHistoryModal
          vehicleId={historyTarget.vehicleId}
          plate={historyTarget.licensePlate}
          onClose={() => setHistoryTarget(null)}
        />
      ) : null}

      {statusTarget ? (
        <VehicleStatusModal
          vehicleId={statusTarget.vehicleId}
          plate={statusTarget.licensePlate}
          current={statusTarget.status}
          onClose={() => setStatusTarget(null)}
          onChanged={() => {
            setStatusTarget(null);
            toast.success('vehicles.review.toasts.statusChanged');
          }}
          onConflict={() => {
            setStatusTarget(null);
            toast.error('vehicles.review.errors.conflict');
            void query.refetch();
          }}
        />
      ) : null}

      <ConfirmDialog
        open={deleteTarget !== null}
        onOpenChange={(open) => {
          if (!open) setDeleteTarget(null);
        }}
        tone="red"
        icon="trash"
        title={t('vehicles.review.confirmDelete.title')}
        description={
          deleteTarget
            ? t('vehicles.review.confirmDelete.body', {
                plate: deleteTarget.licensePlate,
                name: deleteTarget.employee.fullName,
              })
            : ''
        }
        confirmLabel={t('vehicles.review.confirmDelete.confirm')}
        loading={confirmDeletion.isPending}
        onConfirm={() => {
          if (!deleteTarget) return;
          confirmDeletion
            .mutateAsync(deleteTarget.vehicleId)
            .then(() => {
              setDeleteTarget(null);
              toast.success('vehicles.review.toasts.deleted');
            })
            .catch(() => toast.error('vehicles.review.errors.generic'));
        }}
      />
    </PageFrame>
  );
}

interface ReviewBodyProps {
  isLoading: boolean;
  isError: boolean;
  onRetry: () => void;
  rows: VehicleReviewRow[];
  busy: boolean;
  onRestore: (vehicleId: number) => void;
  onConfirmDelete: (row: VehicleReviewRow) => void;
  onChangeStatus: (row: VehicleReviewRow) => void;
  onHistory: (row: VehicleReviewRow) => void;
}

function ReviewBody(props: ReviewBodyProps) {
  const { t } = useTranslation();
  const { isLoading, isError, onRetry, rows, busy } = props;
  if (isLoading) {
    return <TableSkeleton label={t('common.loading')} columns={5} />;
  }
  if (isError) {
    return (
      <TableError
        message={t('vehicles.review.loadError')}
        retryLabel={t('common.retry')}
        onRetry={onRetry}
      />
    );
  }
  if (rows.length === 0) {
    return <TableEmpty icon="car" message={t('vehicles.review.empty')} />;
  }
  return (
    <div className="table-scroll table-cards-mobile">
      <table className="table">
        <thead>
          <tr className="table-header">
            <th scope="col">{t('vehicles.review.columns.employee')}</th>
            <th scope="col">{t('vehicles.review.columns.vehicle')}</th>
            <th scope="col">{t('vehicles.review.columns.plate')}</th>
            <th scope="col">{t('vehicles.review.columns.status')}</th>
            <th scope="col">{t('vehicles.review.columns.actions')}</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <ReviewRow
              key={row.vehicleId}
              row={row}
              busy={busy}
              onRestore={() => props.onRestore(row.vehicleId)}
              onConfirmDelete={() => props.onConfirmDelete(row)}
              onChangeStatus={() => props.onChangeStatus(row)}
              onHistory={() => props.onHistory(row)}
            />
          ))}
        </tbody>
      </table>
    </div>
  );
}

function ReviewFooter({
  page,
  totalPages,
  isFirst,
  isLast,
  onPrev,
  onNext,
}: {
  page: number;
  totalPages: number;
  isFirst: boolean;
  isLast: boolean;
  onPrev: () => void;
  onNext: () => void;
}) {
  const { t } = useTranslation();
  return (
    <>
      <span className="pagination-info">
        {t('requests.pagination.pageInfo', { page: page + 1, total: totalPages })}
      </span>
      <div className="pf-footer-nav">
        <Button variant="white" disabled={isFirst} onClick={onPrev}>
          {t('requests.pagination.previous')}
        </Button>
        <Button variant="white" disabled={isLast} onClick={onNext}>
          {t('requests.pagination.next')}
        </Button>
      </div>
    </>
  );
}

function ReviewToolbar({
  search,
  onSearch,
  tab,
  onTab,
  counts,
}: {
  search: string;
  onSearch: (value: string) => void;
  tab: Tab;
  onTab: (tab: Tab) => void;
  counts: VehicleStatusCounts;
}) {
  const { t } = useTranslation();
  return (
    <>
      <div className="search-box">
        <i className="ti ti-search" aria-hidden="true" />
        <input
          type="text"
          aria-label={t('vehicles.review.search.label')}
          placeholder={t('vehicles.review.search.placeholder')}
          value={search}
          onChange={(event) => onSearch(event.target.value)}
        />
      </div>
      <div className="chip-filters" role="tablist" aria-label={t('vehicles.review.title')}>
        {TABS.map((id) => (
          <button
            key={id}
            type="button"
            role="tab"
            aria-selected={tab === id}
            className={`chip-filter${tab === id ? ' is-active' : ''}`}
            onClick={() => onTab(id)}
          >
            <span className="cf-dot" style={{ background: DOT[id] }} aria-hidden="true" />
            {t(`vehicles.review.tabs.${id}`)}
            <span className="cf-count">{countFor(id, counts)}</span>
          </button>
        ))}
      </div>
    </>
  );
}

interface ReviewRowProps {
  row: VehicleReviewRow;
  busy: boolean;
  onRestore: () => void;
  onConfirmDelete: () => void;
  onChangeStatus: () => void;
  onHistory: () => void;
}

function ReviewRow({ row, busy, onRestore, onConfirmDelete, onChangeStatus, onHistory }: ReviewRowProps) {
  const { t } = useTranslation();
  const meta = [row.brand, row.model, row.color].filter(Boolean).join(' · ') || NONE;
  return (
    <tr className="table-row">
      <td data-label={t('vehicles.review.columns.employee')}>
        <div className="employee-cell">
          <span className="employee-cell-name">{row.employee.fullName}</span>
          {row.employee.department ? (
            <span className="employee-cell-sub">{row.employee.department}</span>
          ) : null}
        </div>
      </td>
      <td data-label={t('vehicles.review.columns.vehicle')}>{meta}</td>
      <td className="mono" data-label={t('vehicles.review.columns.plate')}>{row.licensePlate}</td>
      <td data-label={t('vehicles.review.columns.status')}>
        <span className={`status-badge ${STATUS_CLASS[row.status]}`}>
          {t(`vehicles.status.${row.status}`)}
        </span>
      </td>
      <td className="table-actions" data-label={t('vehicles.review.columns.actions')}>
        {row.status === 'PENDING_DELETION' ? (
          <>
            <Button variant="white" icon="arrow-back-up" onClick={onRestore} disabled={busy}>
              {t('vehicles.review.actions.restore')}
            </Button>
            <Button variant="red" icon="trash" onClick={onConfirmDelete} disabled={busy}>
              {t('vehicles.review.actions.confirmDelete')}
            </Button>
          </>
        ) : (
          <Button variant="white" icon="repeat" onClick={onChangeStatus}>
            {t('vehicles.review.actions.changeStatus')}
          </Button>
        )}
        <Button variant="white" icon="history" onClick={onHistory}>
          {t('vehicles.review.actions.history')}
        </Button>
      </td>
    </tr>
  );
}
