import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useQueryClient } from '@tanstack/react-query';
import { Button } from '../components/Button';
import { InfoBanner } from '../components/InfoBanner';
import { PageHeader } from '../components/PageHeader';
import { StatusPill } from '../components/StatusPill';
import { TableEmpty, TableError, TableSkeleton } from '../components/TableStates';
import {
  AdministrativeReleaseModal,
  type AdministrativeReleasePrefill,
} from '../components/AdministrativeReleaseModal';
import { emitApiErrorToast } from '../api/events';
import { useOccupancyQuery } from '../hooks/useOccupancy';
import { todayIso } from '../utils/releases';
import type { OccupancyItem } from '../types/occupancy';

const OCCUPANCY_KEY = 'occupancy';

// Vista ADMIN: "Liberar por fecha". Elige una fecha, lista los recursos OCUPADOS
// (plazas y puestos) con su titular y origen, y libera uno a uno abriendo el modal
// administrativo pre-rellenado (tasks §Liberar por fecha).
export function ReleaseByDatePage() {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const [date, setDate] = useState(todayIso());
  const [prefill, setPrefill] = useState<AdministrativeReleasePrefill | null>(null);

  const query = useOccupancyQuery(date);
  const occupied = query.data?.occupiedResources ?? [];

  // Etiqueta humana del recurso: "Plaza 3005 · Planta 3" para plazas, "Puesto 12"
  // para puestos (sin planta).
  function resourceLabel(item: OccupancyItem): string {
    if (item.resourceType === 'DESK') {
      return t('releases.byDate.resourceDesk', { number: item.resourceNumber });
    }
    return t('releases.byDate.resourcePark', {
      number: item.resourceNumber,
      floor: item.floor ?? '',
    });
  }

  function openRelease(item: OccupancyItem): void {
    setPrefill({
      employeeId: item.employeeId,
      employeeName: item.employeeName,
      parkingSpaceId: item.resourceId,
      resourceLabel: resourceLabel(item),
      releaseDate: date,
      resourceType: item.resourceType,
    });
  }

  function handleCreated(): void {
    setPrefill(null);
    void queryClient.invalidateQueries({ queryKey: [OCCUPANCY_KEY] });
    emitApiErrorToast('releases.admin.created');
  }

  return (
    <section className="release-by-date-page" aria-label={t('releases.byDate.title')}>
      <PageHeader
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
          className="field-input"
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
        <div className="table-scroll">
          <table className="table">
            <thead>
              <tr className="table-header">
                <th scope="col">{t('releases.byDate.columns.resource')}</th>
                <th scope="col">{t('releases.byDate.columns.type')}</th>
                <th scope="col">{t('releases.byDate.columns.employee')}</th>
                <th scope="col">{t('releases.byDate.columns.origin')}</th>
                <th scope="col">{t('releases.byDate.columns.actions')}</th>
              </tr>
            </thead>
            <tbody>
              {occupied.map((item) => (
                <tr key={`${item.resourceType}-${item.resourceId}`} className="table-row">
                  <td>{resourceLabel(item)}</td>
                  <td>{t(`releases.byDate.resourceType.${item.resourceType}`)}</td>
                  <td>{item.employeeName}</td>
                  <td>
                    <StatusPill tone="released">
                      {t(`releases.byDate.origin.${item.origin}`)}
                    </StatusPill>
                  </td>
                  <td className="table-actions">
                    <Button variant="red" icon="arrow-back-up" onClick={() => openRelease(item)}>
                      {t('releases.byDate.release')}
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : null}

      {prefill ? (
        <AdministrativeReleaseModal
          prefill={prefill}
          onClose={() => setPrefill(null)}
          onCreated={handleCreated}
        />
      ) : null}
    </section>
  );
}
