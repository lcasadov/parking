import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Input } from '../components/Input';
import { PageHeader } from '../components/PageHeader';
import { TableEmpty, TableError, TableSkeleton } from '../components/TableStates';
import { Toolbar } from '../components/Toolbar';
import { useAvailabilityQuery } from '../hooks/useCalendar';
import { isValidIsoDate } from '../utils/calendar';
import { todayIso } from '../utils/requests';

// Vista ADMIN: elige una fecha y lista las plazas disponibles esa fecha
// (consume GET /availability). tasks §4.1.
export function AvailabilityPage() {
  const { t } = useTranslation();
  const [date, setDate] = useState<string>(todayIso());

  const isDateValid = isValidIsoDate(date);
  const query = useAvailabilityQuery(date);
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
            {t('availability.count', { count: resources.length })}
          </p>
          {resources.length === 0 ? (
            <TableEmpty icon="parking-off" message={t('availability.empty')} />
          ) : (
            <div className="table-scroll">
              <table className="table">
                <thead>
                  <tr className="table-header">
                    <th scope="col">{t('availability.columns.space')}</th>
                    <th scope="col">{t('availability.columns.id')}</th>
                  </tr>
                </thead>
                <tbody>
                  {resources.map((resource) => (
                    <tr key={resource.parkingSpaceId} className="table-row">
                      <td>{resource.label}</td>
                      <td>{resource.parkingSpaceId}</td>
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
