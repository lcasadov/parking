import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useQueryClient } from '@tanstack/react-query';
import { Button } from '../components/Button';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { InfoBanner } from '../components/InfoBanner';
import { EmbeddablePageHeader } from '../components/EmbeddablePageHeader';
import { StatusPill } from '../components/StatusPill';
import { TableEmpty, TableError, TableSkeleton } from '../components/TableStates';
import { useBatchRelease, type BatchReleaseItem } from '../hooks/useReleaseSelection';
import {
  AdministrativeReleaseModal,
  type AdministrativeReleasePrefill,
} from '../components/AdministrativeReleaseModal';
import {
  AdminCancelRequestModal,
  type AdminCancelRequestPrefill,
} from '../components/AdminCancelRequestModal';
import { emitApiErrorToast } from '../api/events';
import { useOccupancyQuery } from '../hooks/useOccupancy';
import { todayIso } from '../utils/releases';
import { resourceLabel } from '../utils/resourceLabel';
import type { OccupancyItem } from '../types/occupancy';

const OCCUPANCY_KEY = 'occupancy';

// Vista ADMIN: "Liberar por fecha". Elige una fecha, lista los recursos OCUPADOS
// (plazas y puestos) con su titular y origen, y libera uno a uno abriendo el modal
// administrativo pre-rellenado (tasks §Liberar por fecha).
export function ReleaseByDatePage({ embedded = false }: { embedded?: boolean } = {}) {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const [date, setDate] = useState(todayIso());
  const [prefill, setPrefill] = useState<AdministrativeReleasePrefill | null>(null);
  const [cancelPrefill, setCancelPrefill] = useState<AdminCancelRequestPrefill | null>(null);
  // Selección múltiple para liberar en lote (checkboxes por fila).
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [batchOpen, setBatchOpen] = useState(false);
  const [batchReason, setBatchReason] = useState('');
  const batchRelease = useBatchRelease();

  const query = useOccupancyQuery(date);
  const occupied = useMemo(() => query.data?.occupiedResources ?? [], [query.data]);

  const rowKey = (item: OccupancyItem): string => `${item.resourceType}-${item.resourceId}`;
  const allSelected = occupied.length > 0 && occupied.every((item) => selected.has(rowKey(item)));

  function toggleRow(key: string): void {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(key)) {
        next.delete(key);
      } else {
        next.add(key);
      }
      return next;
    });
  }

  function toggleAll(): void {
    setSelected(allSelected ? new Set() : new Set(occupied.map(rowKey)));
  }

  const selectedItems = useMemo<BatchReleaseItem[]>(
    () =>
      occupied
        .filter((item) => selected.has(rowKey(item)))
        .map((item) => ({
          employeeId: item.employeeId,
          resourceType: item.resourceType,
          resourceId: item.resourceId,
          releaseDate: date,
          origin: item.origin,
          requestId: item.requestId,
        })),
    [occupied, selected, date],
  );

  function confirmBatch(): void {
    batchRelease.mutate(
      { items: selectedItems, reason: batchReason.trim() },
      {
        onSuccess: () => {
          setBatchOpen(false);
          setBatchReason('');
          setSelected(new Set());
          refreshOccupancy();
          emitApiErrorToast('releases.admin.created');
        },
        onError: () => emitApiErrorToast('releases.byDate.batchError'),
      },
    );
  }

  // El mecanismo de liberacion depende del origen del recurso: si lo ocupa una
  // solicitud (trae `requestId`), se libera cancelando la solicitud (admin-cancel);
  // si es una asignacion fija, se usa la liberacion administrativa (Release).
  function openRelease(item: OccupancyItem): void {
    if (typeof item.requestId === 'number') {
      setCancelPrefill({
        requestId: item.requestId,
        employeeName: item.employeeName,
        resourceLabel: resourceLabel(item, t),
        releaseDate: date,
      });
      return;
    }
    setPrefill({
      employeeId: item.employeeId,
      employeeName: item.employeeName,
      parkingSpaceId: item.resourceId,
      resourceLabel: resourceLabel(item, t),
      releaseDate: date,
      resourceType: item.resourceType,
    });
  }

  function refreshOccupancy(): void {
    void queryClient.invalidateQueries({ queryKey: [OCCUPANCY_KEY] });
  }

  function handleCreated(): void {
    setPrefill(null);
    refreshOccupancy();
    emitApiErrorToast('releases.admin.created');
  }

  function handleCancelled(): void {
    setCancelPrefill(null);
    refreshOccupancy();
    emitApiErrorToast('requests.adminCancel.cancelled');
  }

  return (
    <section className="release-by-date-page" aria-label={t('releases.byDate.title')}>
      <EmbeddablePageHeader
        embedded={embedded}
        eyebrow={t('releases.byDate.eyebrow')}
        title={t('releases.byDate.title')}
        description={t('releases.byDate.description')}
      />

      <InfoBanner variant="blue" icon="info-circle">
        {t('releases.byDate.intro')}
      </InfoBanner>

      <div className="filter-bar">
        <label className="field-label" htmlFor="release-by-date-date">
          {t('releases.byDate.dateLabel')}
        </label>
        <input
          id="release-by-date-date"
          type="date"
          className="field-input release-date-input"
          value={date}
          min={todayIso()}
          onChange={(event) => setDate(event.target.value)}
        />
      </div>

      {query.isLoading ? <TableSkeleton label={t('common.loading')} columns={5} /> : null}

      {query.isError ? (
        <TableError
          message={t('releases.byDate.loadError')}
          retryLabel={t('common.retry')}
          onRetry={() => void query.refetch()}
        />
      ) : null}

      {!query.isLoading && !query.isError && occupied.length === 0 ? (
        <TableEmpty icon="calendar-check" message={t('releases.byDate.empty')} />
      ) : null}

      {!query.isLoading && !query.isError && occupied.length > 0 ? (
        <>
        {selected.size > 0 ? (
          <div className="release-batch-bar">
            <span>{t('releases.byDate.selectedCount', { count: selected.size })}</span>
            <Button variant="red" icon="arrow-back-up" onClick={() => setBatchOpen(true)}>
              {t('releases.byDate.releaseSelected', { count: selected.size })}
            </Button>
          </div>
        ) : null}
        <div className="table-scroll table-cards-mobile">
          <table className="table">
            <thead>
              <tr className="table-header">
                <th scope="col" className="col-check">
                  <input
                    type="checkbox"
                    aria-label={t('releases.byDate.selectAll')}
                    checked={allSelected}
                    onChange={toggleAll}
                  />
                </th>
                <th scope="col">{t('releases.byDate.columns.resource')}</th>
                <th scope="col">{t('releases.byDate.columns.type')}</th>
                <th scope="col">{t('releases.byDate.columns.employee')}</th>
                <th scope="col">{t('releases.byDate.columns.origin')}</th>
                <th scope="col">{t('releases.byDate.columns.actions')}</th>
              </tr>
            </thead>
            <tbody>
              {occupied.map((item) => (
                <tr key={rowKey(item)} className="table-row">
                  <td className="col-check" data-label={t('releases.byDate.selectAll')}>
                    <input
                      type="checkbox"
                      aria-label={resourceLabel(item, t)}
                      checked={selected.has(rowKey(item))}
                      onChange={() => toggleRow(rowKey(item))}
                    />
                  </td>
                  <td data-label={t('releases.byDate.columns.resource')}>{resourceLabel(item, t)}</td>
                  <td data-label={t('releases.byDate.columns.type')}>
                    {t(`releases.byDate.resourceType.${item.resourceType}`)}
                  </td>
                  <td data-label={t('releases.byDate.columns.employee')}>{item.employeeName}</td>
                  <td data-label={t('releases.byDate.columns.origin')}>
                    <StatusPill tone="released">
                      {t(`releases.byDate.origin.${item.origin}`)}
                    </StatusPill>
                  </td>
                  <td className="table-actions" data-label={t('releases.byDate.columns.actions')}>
                    <Button variant="red" icon="arrow-back-up" onClick={() => openRelease(item)}>
                      {t('releases.byDate.release')}
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        </>
      ) : null}

      <ConfirmDialog
        open={batchOpen}
        onOpenChange={(open) => {
          if (!open) setBatchOpen(false);
        }}
        tone="green"
        icon="arrow-back-up"
        title={t('releases.byDate.batchTitle', { count: selected.size })}
        description={
          <div className="release-batch-form">
            <p>{t('releases.byDate.batchBody', { count: selected.size })}</p>
            <label className="field-label" htmlFor="batch-reason">
              {t('releases.byDate.batchReasonLabel')}
            </label>
            <textarea
              id="batch-reason"
              className="field-input"
              rows={3}
              value={batchReason}
              onChange={(event) => setBatchReason(event.target.value)}
            />
            <p className="hint">{t('releases.byDate.batchReasonHint')}</p>
          </div>
        }
        confirmLabel={t('releases.byDate.releaseSelected', { count: selected.size })}
        busy={batchRelease.isPending || batchReason.trim().length < 5}
        onConfirm={confirmBatch}
      />

      {prefill ? (
        <AdministrativeReleaseModal
          prefill={prefill}
          onClose={() => setPrefill(null)}
          onCreated={handleCreated}
        />
      ) : null}

      {cancelPrefill ? (
        <AdminCancelRequestModal
          prefill={cancelPrefill}
          onClose={() => setCancelPrefill(null)}
          onCancelled={handleCancelled}
        />
      ) : null}
    </section>
  );
}
