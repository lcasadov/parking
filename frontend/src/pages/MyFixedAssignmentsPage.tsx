import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from '../components/Button';
import { DayBadges } from '../components/DayBadges';
import { ReleaseResourceModal } from '../components/ReleaseResourceModal';
import { Spinner } from '../components/Spinner';
import { emitApiErrorToast } from '../api/events';
import { useAuth } from '../auth/useAuth';
import { useEmployeeFixedAssignmentsQuery } from '../hooks/useFixedAssignments';
import { groupFixedAssignments, type FixedAssignmentGroup } from '../utils/fixedAssignments';

interface ReleaseTarget {
  parkingSpaceId: number;
  spaceLabel: string;
}

// Vista EMPLOYEE: "mis asignaciones fijas" del usuario autenticado
// (getEmployeeFixedAssignments con su propio id). Cada recurso fijo ofrece la
// accion "Liberar" para una fecha presente o futura (tasks §4.1).
export function MyFixedAssignmentsPage() {
  const { t } = useTranslation();
  const { user } = useAuth();
  const employeeId = user?.employeeId ?? null;
  const [releaseTarget, setReleaseTarget] = useState<ReleaseTarget | null>(null);

  const query = useEmployeeFixedAssignmentsQuery(employeeId);
  const groups = useMemo(() => groupFixedAssignments(query.data ?? []), [query.data]);

  function spaceLabel(group: FixedAssignmentGroup): string {
    return `#${group.parkingSpaceId}`;
  }

  function handleReleased(): void {
    setReleaseTarget(null);
    emitApiErrorToast('releases.release.created');
  }

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
                <th scope="col">{t('releases.mine.columns.actions')}</th>
              </tr>
            </thead>
            <tbody>
              {groups.length === 0 ? (
                <tr>
                  <td colSpan={3} className="table-empty">
                    {t('fixedAssignments.mine.empty')}
                  </td>
                </tr>
              ) : (
                groups.map((group) => (
                  <tr key={group.key} className="table-row">
                    <td>{spaceLabel(group)}</td>
                    <td>
                      <DayBadges days={group.days} />
                    </td>
                    <td className="table-actions">
                      <Button
                        variant="blue"
                        icon="calendar-off"
                        onClick={() =>
                          setReleaseTarget({
                            parkingSpaceId: group.parkingSpaceId,
                            spaceLabel: spaceLabel(group),
                          })
                        }
                      >
                        {t('releases.release.action')}
                      </Button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      ) : null}

      {releaseTarget ? (
        <ReleaseResourceModal
          parkingSpaceId={releaseTarget.parkingSpaceId}
          spaceLabel={releaseTarget.spaceLabel}
          onClose={() => setReleaseTarget(null)}
          onReleased={handleReleased}
        />
      ) : null}
    </section>
  );
}
