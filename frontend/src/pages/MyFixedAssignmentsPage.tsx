import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from '../components/Button';
import { DayBadges } from '../components/DayBadges';
import { PageHeader } from '../components/PageHeader';
import { ReleaseResourceModal } from '../components/ReleaseResourceModal';
import { TableEmpty, TableError, TableSkeleton } from '../components/TableStates';
import { emitApiErrorToast } from '../api/events';
import { useAuth } from '../auth/useAuth';
import { useDesksByIdsQuery } from '../hooks/useDesks';
import { useEmployeeFixedAssignmentsQuery } from '../hooks/useFixedAssignments';
import { groupFixedAssignments, type FixedAssignmentGroup } from '../utils/fixedAssignments';
import type { ResourceType } from '../types/request';

interface ReleaseTarget {
  parkingSpaceId: number;
  spaceLabel: string;
  resourceType: ResourceType;
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
  const ready = !query.isLoading && !query.isError;

  // Numero real del puesto (GET /desks/{id} es accesible a EMPLOYEE, a diferencia
  // del catalogo de plazas que es solo ADMIN — ver nota en fixedAssignments.mine
  // .genericParkingLabel). Solo se resuelven los puestos presentes en la lista.
  const deskIds = useMemo(
    () => groups.filter((group) => group.resourceType === 'DESK').map((group) => group.parkingSpaceId),
    [groups],
  );
  const desksById = useDesksByIdsQuery(deskIds);

  // Etiqueta de negocio del recurso (nunca el id interno, spec employee-portal
  // "Liberación de recurso fijo con tipo y etiqueta correctos"): el puesto muestra
  // su numero real; la plaza usa una etiqueta generica al no haber endpoint
  // EMPLOYEE-safe que resuelva su numero (catalogo de plazas es solo ADMIN).
  function spaceLabel(group: FixedAssignmentGroup): string {
    if (group.resourceType === 'DESK') {
      const desk = desksById[group.parkingSpaceId];
      return desk
        ? t('requests.mine.resourceLabel.desk', { number: desk.number })
        : t('common.loading');
    }
    return t('fixedAssignments.mine.genericParkingLabel');
  }

  function handleReleased(): void {
    setReleaseTarget(null);
    emitApiErrorToast('releases.release.created');
  }

  return (
    <section className="my-fixed-assignments-page" aria-label={t('fixedAssignments.mine.title')}>
      <PageHeader
        eyebrow={t('fixedAssignments.mine.eyebrow')}
        title={t('fixedAssignments.mine.title')}
        description={t('fixedAssignments.mine.description')}
      />

      {query.isLoading ? <TableSkeleton label={t('common.loading')} columns={3} /> : null}

      {query.isError ? (
        <TableError
          message={t('fixedAssignments.mine.loadError')}
          retryLabel={t('common.retry')}
          onRetry={() => void query.refetch()}
        />
      ) : null}

      {ready ? (
        groups.length === 0 ? (
          <TableEmpty icon="calendar-star" message={t('fixedAssignments.mine.empty')} />
        ) : (
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
                {groups.map((group) => (
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
                            resourceType: group.resourceType,
                          })
                        }
                      >
                        {t('releases.release.action')}
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )
      ) : null}

      {releaseTarget ? (
        <ReleaseResourceModal
          parkingSpaceId={releaseTarget.parkingSpaceId}
          spaceLabel={releaseTarget.spaceLabel}
          resourceType={releaseTarget.resourceType}
          onClose={() => setReleaseTarget(null)}
          onReleased={handleReleased}
        />
      ) : null}
    </section>
  );
}
