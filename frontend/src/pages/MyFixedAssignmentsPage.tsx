import { useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { DayBadges } from '../components/DayBadges';
import { Spinner } from '../components/Spinner';
import { useAuth } from '../auth/useAuth';
import { useEmployeeFixedAssignmentsQuery } from '../hooks/useFixedAssignments';
import { groupFixedAssignments } from '../utils/fixedAssignments';

// Vista EMPLOYEE de solo lectura: "mis asignaciones fijas" del usuario
// autenticado (getEmployeeFixedAssignments con su propio id). Sin controles de
// edicion (tasks §4.4).
export function MyFixedAssignmentsPage() {
  const { t } = useTranslation();
  const { user } = useAuth();
  const employeeId = user?.employeeId ?? null;

  const query = useEmployeeFixedAssignmentsQuery(employeeId);
  const groups = useMemo(() => groupFixedAssignments(query.data ?? []), [query.data]);

  return (
    <section className="my-fixed-assignments-page" aria-labelledby="my-fixed-assignments-title">
      <header className="page-header">
        <h1 id="my-fixed-assignments-title" className="section-title">
          {t('fixedAssignments.mine.title')}
        </h1>
      </header>

      {query.isLoading ? <Spinner /> : null}

      {query.isError ? (
        <p className="form-error" role="alert">
          {t('fixedAssignments.mine.loadError')}
        </p>
      ) : null}

      {!query.isLoading && !query.isError ? (
        <div className="table-scroll">
          <table className="table">
            <thead>
              <tr className="table-header">
                <th scope="col">{t('fixedAssignments.mine.columns.space')}</th>
                <th scope="col">{t('fixedAssignments.mine.columns.days')}</th>
              </tr>
            </thead>
            <tbody>
              {groups.length === 0 ? (
                <tr>
                  <td colSpan={2} className="table-empty">
                    {t('fixedAssignments.mine.empty')}
                  </td>
                </tr>
              ) : (
                groups.map((group) => (
                  <tr key={group.key} className="table-row">
                    <td>{`#${group.parkingSpaceId}`}</td>
                    <td>
                      <DayBadges days={group.days} />
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      ) : null}
    </section>
  );
}
