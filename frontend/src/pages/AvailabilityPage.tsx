import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Input } from '../components/Input';
import { PageHeader } from '../components/PageHeader';
import { ResourceModeSwitch } from '../components/ResourceModeSwitch';
import { ResourceTypePill } from '../components/ResourceTypePill';
import { TableEmpty, TableError, TableSkeleton } from '../components/TableStates';
import { Toolbar } from '../components/Toolbar';
import { useAvailabilityQuery } from '../hooks/useCalendar';
import { isValidIsoDate } from '../utils/calendar';
import { todayIso } from '../utils/requests';
import type { ResourceType } from '../types/request';

// Vista ADMIN: elige una fecha y un tipo de recurso, y lista los recursos
// disponibles esa fecha (consume GET /availability?date&resourceType). Cada fila
// muestra la etiqueta de negocio del recurso, nunca el id interno (availability-
// calendar spec, "Conmutador de tipo de recurso en la disponibilidad").
export function AvailabilityPage() {
  const { t } = useTranslation();
  const [date, setDate] = useState<string>(todayIso());
  const [resourceType, setResourceType] = useState<ResourceType>('PARKING');

  const isDateValid = isValidIsoDate(date);
  const query = useAvailabilityQuery(date, resourceType);
  const resources = query.data?.availableResources ?? [];

  return (
    <section className="availability-page occ-availability" aria-label={t('availability.title')}>
      {/* Cabecera coherente con la vista Semanal: eyebrow "Ocupación" + titulo
          mode-explicito (Plazas de parking / Puestos de oficina). */}
      <PageHeader
        eyebrow={t('occupancy.title')}
        title={t(`occupancy.weekly.modeTitle.${resourceType}`)}
        description={t('availability.description')}
      />

      {/* Mismo selector GRANDE plaza/puesto que la vista Semanal. */}
      <ResourceModeSwitch
        value={resourceType}
        onChange={setResourceType}
        ariaLabel={t('occupancy.weekly.modeSwitchLabel')}
      />

      <Toolbar ariaLabel={t('availability.title')}>
        <Input
          id="availability-date"
          type="date"
          label={t('availability.dateLabel')}
          value={date}
          onChange={(event) => setDate(event.target.value)}
        />
      </Toolbar>

      {!isDateValid ? (
        <p className="form-hint" role="status">
          {date.length === 0 ? t('availability.hint') : t('availability.invalidDate')}
        </p>
      ) : null}

      {isDateValid && query.isLoading ? (
        <TableSkeleton label={t('common.loading')} columns={2} />
      ) : null}

      {isDateValid && query.isError ? (
        <TableError
          message={t('availability.loadError')}
          retryLabel={t('common.retry')}
          onRetry={() => void query.refetch()}
        />
      ) : null}

      {isDateValid && !query.isLoading && !query.isError ? (
        <>
          <p className="availability-count" aria-live="polite">
            {t(`availability.count.${resourceType}`, { count: resources.length })}
          </p>
          {resources.length === 0 ? (
            <TableEmpty icon="parking-off" message={t(`availability.empty.${resourceType}`)} />
          ) : (
            <div className="table-scroll">
              <table className="table">
                <thead>
                  <tr className="table-header">
                    <th scope="col">{t('availability.columns.space')}</th>
                    <th scope="col">{t('availability.columns.type')}</th>
                  </tr>
                </thead>
                <tbody>
                  {resources.map((resource) => (
                    <tr key={resource.parkingSpaceId} className="table-row">
                      <td>{resource.label}</td>
                      <td>
                        <ResourceTypePill resourceType={resourceType} />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </>
      ) : null}
    </section>
  );
}
