import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Input } from '../components/Input';
import { PageHeader } from '../components/PageHeader';
import { ResourceTypePill } from '../components/ResourceTypePill';
import { TableEmpty, TableError, TableSkeleton } from '../components/TableStates';
import { Toolbar } from '../components/Toolbar';
import { useAvailabilityQuery } from '../hooks/useCalendar';
import { isValidIsoDate } from '../utils/calendar';
import { todayIso } from '../utils/requests';
import type { ResourceType } from '../types/request';

// Orden fijo del conmutador plaza/puesto (S1192: sin literales repetidos).
const RESOURCE_TYPES: ResourceType[] = ['PARKING', 'DESK'];

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
    <section className="availability-page" aria-label={t('availability.title')}>
      <PageHeader
        eyebrow={t('availability.eyebrow')}
        title={t('availability.title')}
        description={t('availability.description')}
      />

      <Toolbar ariaLabel={t('availability.title')}>
        <Input
          id="availability-date"
          type="date"
          label={t('availability.dateLabel')}
          value={date}
          onChange={(event) => setDate(event.target.value)}
        />
        <div
          className="segmented"
          role="group"
          aria-label={t('availability.resourceTypeLabel')}
        >
          {RESOURCE_TYPES.map((type) => (
            <button
              key={type}
              type="button"
              className={resourceType === type ? 'active' : ''}
              aria-pressed={resourceType === type}
              onClick={() => setResourceType(type)}
            >
              {t(`availability.resourceType.${type}`)}
            </button>
          ))}
        </div>
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
