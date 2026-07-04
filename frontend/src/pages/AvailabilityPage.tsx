import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Input } from '../components/Input';
import { Spinner } from '../components/Spinner';
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
    <section className="availability-page" aria-labelledby="availability-title">
      <header className="page-header">
        <h1 id="availability-title" className="section-title">
          {t('availability.title')}
        </h1>
      </header>

      <div className="availability-controls">
        <Input
          id="availability-date"
          type="date"
          label={t('availability.dateLabel')}
          value={date}
          onChange={(event) => setDate(event.target.value)}
        />
      </div>

      {!isDateValid ? (
        <p className="form-hint" role="status">
          {date.length === 0 ? t('availability.hint') : t('availability.invalidDate')}
        </p>
      ) : null}

      {isDateValid && query.isLoading ? <Spinner /> : null}

      {isDateValid && query.isError ? (
        <p className="form-error" role="alert">
          {t('availability.loadError')}
        </p>
      ) : null}

      {isDateValid && !query.isLoading && !query.isError ? (
        <>
          <p className="availability-count" aria-live="polite">
            {t('availability.count', { count: resources.length })}
          </p>
          <div className="table-scroll">
            <table className="table">
              <thead>
                <tr className="table-header">
                  <th scope="col">{t('availability.columns.space')}</th>
                  <th scope="col">{t('availability.columns.id')}</th>
                </tr>
              </thead>
              <tbody>
                {resources.length === 0 ? (
                  <tr>
                    <td colSpan={2} className="table-empty">
                      {t('availability.empty')}
                    </td>
                  </tr>
                ) : (
                  resources.map((resource) => (
                    <tr key={resource.parkingSpaceId} className="table-row">
                      <td>{resource.label}</td>
                      <td>{resource.parkingSpaceId}</td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </>
      ) : null}
    </section>
  );
}
